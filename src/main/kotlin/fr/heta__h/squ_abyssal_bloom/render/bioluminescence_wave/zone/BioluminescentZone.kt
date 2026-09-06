package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloomPulse
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloomPulseField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationSnapshot
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentZoneGenerationStage
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerator
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction.BioluminescentAmbientParticleEmitter
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction.BioluminescentBloomParticleEmitter
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction.BioluminescentMovementWave
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction.BioluminescentMovementWaveField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction.BioluminescentShimmeringSoundEmitter
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.PI
import kotlin.math.sin
import java.util.UUID

class BioluminescentZone(
    val zoneSeed: Long,
    val anchor: BlockPos,
    val palette: BioluminescentPalette,
    val preset: BioluminescentZonePreset,
    val activity: BioluminescentZoneActivity,
    initialCell: BioluminescentWaterCell,
    val createdAt: Long,
    val lifetime: Long,
    val colorPhase: Double,
    val requestedByCommand: Boolean,
    val eventId: UUID = UUID.randomUUID(),
    val mode: BioluminescenceWaveMode = BioluminescenceWaveMode.NORMAL,
    val endGameTime: Long = createdAt + lifetime,
    var serverGameTimeOffset: Long = 0L
) : AutoCloseable {
    companion object {
        const val PULSE_PERIOD_TICKS = 320.0
        const val MIN_PULSE_INTENSITY = 0.30
        const val MAX_PULSE_INTENSITY = 0.70
        const val MAX_RENDER_INTENSITY = 1.15f
        const val BRIGHTNESS_SEED_SALT = 0x6A09E667F3BCC909L
        const val BLOOM_PULSE_VOLUME = 2.0f
        const val SOUND_PITCH_SPREAD = 0.2f
    }

    val blooms: MutableMap<UUID, BioluminescentBloom> = linkedMapOf()

    private var revalidationTileIndex = 0
    private var revalidationQuadIndex = 0

    private var generator: BioluminescentZoneGenerator? = BioluminescentZoneGenerator(
        anchor,
        initialCell,
        preset,
        zoneSeed,
        palette
    )
    private val brightnessScale = brightnessScaleFromSeed()
    private val movementWaves = BioluminescentMovementWaveField()
    private val ambientParticles = BioluminescentAmbientParticleEmitter()
    private val shimmeringSounds = BioluminescentShimmeringSoundEmitter()
    private val bloomPulses = BioluminescentBloomPulseField()
    private val bloomParticles = BioluminescentBloomParticleEmitter()

    var spatialData: BioluminescentZoneGenerationResult? = null
        private set

    var activatedAt: Long? = createdAt
        private set

    private var endingAtServerGameTime: Long? = null

    private var readyAtGameTime: Long? = null

    var generationStage = BioluminescentZoneGenerationStage.COLLECT_WATER_DOMAIN
        private set

    var failureReason: String? = null
        private set

    var generationCpuNanos: Long = 0L
        private set

    val tiles: List<BioluminescentZoneTile>
        get() = spatialData?.tiles ?: emptyList()

    val hasRenderableTiles: Boolean
        get() = tiles.isNotEmpty()

    val reservedTileCount: Int
        get() = generator?.reservedTileCount ?: spatialData?.tiles?.size ?: 0

    val isReady: Boolean
        get() = generationStage == BioluminescentZoneGenerationStage.READY

    val isFailed: Boolean
        get() = generationStage == BioluminescentZoneGenerationStage.FAILED

    val targetMacroCoverage: Double
        get() = generator?.targetMacroCoverage ?: spatialData?.macroField?.targetCoverage ?: 0.0

    val targetVisibleCoverage: Double
        get() = generator?.targetVisibleCoverage ?: spatialData?.emissionField?.targetVisibleCoverage ?: 0.0

    val activeMovementWaveCount: Int
        get() = movementWaves.activeWaveCount

    val activeBloomPulseCount: Int
        get() = bloomPulses.activePulseCount

    val movingEntityCount: Int
        get() = movementWaves.movingSourceCount

    fun advanceGeneration(
        level: ClientLevel,
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maxTileCount: Int,
        gameTime: Long,
        priorityPosition: BlockPos,
        sliceNanos: Long
    ) {
        val activeGenerator = generator ?: return
        activeGenerator.advance(
            level,
            textureManager,
            identifierFactory,
            maxTileCount,
            gameTime,
            sliceNanos,
            BioluminescentZoneGenerator.workStepsForSlice(sliceNanos),
            BioluminescentZoneGenerator.uploadStepsForSlice(sliceNanos),
            priorityPosition.x + 0.5,
            priorityPosition.z + 0.5
        )
        generationStage = activeGenerator.stage
        generationCpuNanos = activeGenerator.cpuNanos
        if (activeGenerator.stage == BioluminescentZoneGenerationStage.READY) {
            val completed = checkNotNull(activeGenerator.takeCompletedResult())
            for (tile in completed.tiles) tile.beginFadeIn(gameTime)
            readyAtGameTime = gameTime
            spatialData = completed
            activeGenerator.close()
            generator = null
        } else if (activeGenerator.stage == BioluminescentZoneGenerationStage.FAILED) {
            failureReason = activeGenerator.failureReason ?: "unknown generation failure"
            activeGenerator.close()
            generator = null
        }
    }

    fun generationSnapshot(): BioluminescentZoneGenerationSnapshot {
        generator?.let { return it.snapshot() }
        val data = spatialData
        return BioluminescentZoneGenerationSnapshot(
            generationStage,
            data?.domain?.size ?: 0,
            data?.domain?.geodesicRadius ?: 0,
            data?.topology?.cores?.size ?: 0,
            data?.reactionDiffusion?.iterations ?: 0,
            preset.reactionDiffusionIterations,
            data?.tiles?.size ?: 0,
            data?.tiles?.count(BioluminescentZoneTile::uploaded) ?: 0,
            generationCpuNanos,
            failureReason
        )
    }

    fun temporalIntensityAt(renderGameTime: Double): Float {
        val lifecycle = lifecycleIntensityAt(renderGameTime)
        if (lifecycle <= 0.0f) return 0.0f
        return (lifecycle * brightnessScale)
            .toFloat().coerceIn(0.0f, MAX_RENDER_INTENSITY)
    }

    private val configuredFadeTicks: Double
        get() = ModConfig.bioluminescenceTileFadeInTicks.coerceAtLeast(0).toDouble()

    fun loadingIntensityAt(renderGameTime: Double): Float {
        val readyAt = readyAtGameTime ?: return 0.0f
        val fade = configuredFadeTicks
        if (fade <= 0.0) return 1.0f
        return ((renderGameTime - readyAt) / fade).toFloat().coerceIn(0.0f, 1.0f)
    }

    fun lifecycleIntensityAt(renderGameTime: Double): Float {
        val start = activatedAt ?: return 0.0f
        val serverTime = renderGameTime + serverGameTimeOffset
        val elapsed = (serverTime - start).coerceAtLeast(0.0)
        var intensity = lifecycleIntensity(elapsed)
        val endingAt = endingAtServerGameTime
        if (endingAt != null) {
            val fadeOutElapsed = (serverTime - endingAt).coerceAtLeast(0.0)
            intensity *= 1.0 - ModUtilities.smooth(0.0, configuredFadeTicks, fadeOutElapsed)
        }
        return intensity.toFloat().coerceIn(0.0f, 1.0f)
    }

    fun beginEnding(serverGameTime: Long) {
        if (endingAtServerGameTime == null) endingAtServerGameTime = serverGameTime
    }

    fun endingFadeComplete(renderGameTime: Double): Boolean {
        val endingAt = endingAtServerGameTime ?: return false
        return renderGameTime + serverGameTimeOffset - endingAt >= configuredFadeTicks
    }

    fun localPulseIntensityAt(renderGameTime: Double, spatialPhase: Double): Float {
        val start = activatedAt ?: return 0.0f
        val elapsed = (renderGameTime + serverGameTimeOffset - start).coerceAtLeast(0.0)
        val phase = colorPhase + spatialPhase + elapsed * PI * 2.0 / PULSE_PERIOD_TICKS
        val wave = 0.5 + 0.5 * sin(phase)
        val easedWave = ModUtilities.smooth(0.0, 1.0, wave)
        return (MIN_PULSE_INTENSITY +
            (MAX_PULSE_INTENSITY - MIN_PULSE_INTENSITY) * easedWave).toFloat()
    }

    fun updateMovementWaves(level: ClientLevel, gameTime: Long) {
        val data = spatialData ?: return
        movementWaves.tick(level, data, gameTime)
    }

    fun spawnAmbientParticles(level: ClientLevel, gameTime: Long) {
        val data = spatialData ?: return
        ambientParticles.tick(level, data, temporalIntensityAt(gameTime.toDouble()))
    }

    fun tickAmbientSounds(level: ClientLevel, gameTime: Long) {
        val data = spatialData ?: return
        if (lifecycleIntensityAt(gameTime.toDouble()) <= 0.0f) return
        if (activity != BioluminescentZoneActivity.ACTIVE && !movementWaves.playerDisturbedRecently(gameTime)) return
        shimmeringSounds.tick(level, data, gameTime)
    }

    fun tickBlooms(level: ClientLevel, gameTime: Long) {
        if (blooms.isEmpty()) return
        val renderGameTime = gameTime.toDouble()
        val waveLifecycle = lifecycleIntensityAt(renderGameTime)
        val data = spatialData

        val terminal = mutableListOf<UUID>()
        for (bloom in blooms.values) {
            if (data != null) bloom.ensureGeometry(data)
            if (bloom.terminalFadeComplete(renderGameTime)) {
                terminal.add(bloom.id)
                continue
            }
            if (bloom.lifecycle != PlanktonBloomLifecycle.ACTIVE) continue
            bloomParticles.tickAmbient(level, bloom, palette, waveLifecycle * bloom.terminalFadeAt(renderGameTime))
        }
        terminal.forEach(blooms::remove)

        if (data == null) return
        for (bloom in bloomPulses.tick(blooms.values, data.domain.bounds, gameTime)) {
            bloomParticles.emitPulseBurst(level, bloom, palette, waveLifecycle)
            bloom.geometry?.let { geometry ->
                ModUtilities.playPositionedSound(
                    level,
                    ModSounds.BLOOM_PULSE.get(),
                    geometry.centerX,
                    geometry.surfaceY,
                    geometry.centerZ,
                    BLOOM_PULSE_VOLUME,
                    SOUND_PITCH_SPREAD
                )
            }
        }
        bloomPulses.forEachFront(renderGameTime) { originX, originZ, frontRadius, strength ->
            bloomParticles.emitPulseFront(
                level, data, palette, originX, originZ, frontRadius, (strength * waveLifecycle).toFloat()
            )
        }
    }

    fun movementWaveIntensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Float =
        movementWaves.intensityAt(worldX, worldZ, renderGameTime)

    fun movementWaveIntensityAt(
        worldX: Double,
        worldZ: Double,
        renderGameTime: Double,
        candidates: Iterable<BioluminescentMovementWave>
    ): Float = movementWaves.intensityAt(worldX, worldZ, renderGameTime, candidates)

    fun collectMovementWavesAffecting(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double,
        destination: MutableList<BioluminescentMovementWave>
    ) = movementWaves.collectAffecting(minX, minZ, maxX, maxZ, renderGameTime, destination)

    fun movementWavesAffect(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Boolean = movementWaves.affects(minX, minZ, maxX, maxZ, renderGameTime)

    fun movementWaveVisibilityStrength(): Double = movementWaves.visibilityStrength

    fun bloomPulseIntensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Float =
        bloomPulses.intensityAt(worldX, worldZ, renderGameTime)

    fun bloomPulseIntensityAt(
        worldX: Double,
        worldZ: Double,
        renderGameTime: Double,
        candidates: Iterable<BioluminescentBloomPulse>
    ): Float = bloomPulses.intensityAt(worldX, worldZ, renderGameTime, candidates)

    fun collectBloomPulsesAffecting(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double,
        destination: MutableList<BioluminescentBloomPulse>
    ) = bloomPulses.collectAffecting(minX, minZ, maxX, maxZ, renderGameTime, destination)

    fun bloomPulseMaxIntensityIn(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Float = bloomPulses.maxIntensityIn(minX, minZ, maxX, maxZ, renderGameTime)

    fun bloomPulseVisibilityStrength(): Double = bloomPulses.visibilityStrength

    fun revalidateWaterStep(level: ClientLevel, budget: Int) {
        val currentTiles = tiles
        if (currentTiles.isEmpty()) return
        var remaining = budget
        while (remaining > 0) {
            if (revalidationTileIndex >= currentTiles.size) {
                revalidationTileIndex = 0
                revalidationQuadIndex = 0
            }
            val tile = currentTiles[revalidationTileIndex]
            val quads = tile.renderQuads
            if (quads.isEmpty() || revalidationQuadIndex >= quads.size) {
                revalidationTileIndex++
                revalidationQuadIndex = 0
                continue
            }
            val quad = quads[revalidationQuadIndex]
            revalidationQuadIndex++
            remaining--
            if (quad.invalidated) continue
            val checkX = tile.originX + (quad.minLocalX + quad.maxLocalX) / 2
            val checkZ = tile.originZ + (quad.minLocalZ + quad.maxLocalZ) / 2
            if (!ModUtilities.hasLoadedChunk(level, checkX shr 4, checkZ shr 4)) continue
            val waterBlockY = kotlin.math.floor(quad.surfaceY).toInt()
            if (!ModUtilities.isRenderableWaterSurface(level, BlockPos(checkX, waterBlockY, checkZ))) {
                quad.invalidated = true
            }
        }
    }

    fun isCompleteAt(gameTime: Long): Boolean {
        return mode == BioluminescenceWaveMode.NORMAL && gameTime + serverGameTimeOffset >= endGameTime
    }

    fun horizontalDistanceSqr(worldX: Double, worldZ: Double): Double {
        return ModUtilities.horizontalDistanceSqr(
            anchor.x + 0.5,
            anchor.z + 0.5,
            worldX,
            worldZ
        )
    }

    override fun close() {
        generator?.close()
        generator = null
        spatialData?.tiles?.forEach(BioluminescentZoneTile::close)
        spatialData = null
        activatedAt = null
        endingAtServerGameTime = null
        movementWaves.clear()
        shimmeringSounds.clear()
        bloomPulses.clear()
        blooms.clear()
    }

    private fun lifecycleIntensity(elapsedTicks: Double): Double {
        if (mode == BioluminescenceWaveMode.TOTAL_NIGHT) return 1.0
        val remainingTicks = lifetime - elapsedTicks
        if (remainingTicks <= 0.0) return 0.0
        val fade = configuredFadeTicks
        if (fade <= 0.0) return 1.0
        return ModUtilities.smooth(0.0, fade, remainingTicks)
    }

    private fun brightnessScaleFromSeed(): Double {
        val mixed = RandomSupport.mixStafford13(zoneSeed xor BRIGHTNESS_SEED_SALT)
        return 0.97 + ((mixed ushr 40) and 0xFFFFL).toDouble() / 65535.0 * 0.06
    }

}
