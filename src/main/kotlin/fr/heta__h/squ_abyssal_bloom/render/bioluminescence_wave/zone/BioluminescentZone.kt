package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationSnapshot
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationStage
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerator
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction.BioluminescentMovementWaveField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.PI
import kotlin.math.sin

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
    val requestedByCommand: Boolean
) : AutoCloseable {
    companion object {
        const val PULSE_PERIOD_TICKS = 320.0
        const val MIN_PULSE_INTENSITY = 0.30
        const val MAX_PULSE_INTENSITY = 0.70
        const val APPEARANCE_END = 0.20
        const val DISAPPEARANCE_START = 0.80
        const val MAX_RENDER_INTENSITY = 1.15f
        const val BRIGHTNESS_SEED_SALT = 0x6A09E667F3BCC909L
    }

    private var generator: BioluminescentZoneGenerator? = BioluminescentZoneGenerator(
        anchor,
        initialCell,
        preset,
        zoneSeed,
        palette
    )
    private val brightnessScale = brightnessScaleFromSeed()
    private val movementWaves = BioluminescentMovementWaveField()

    var spatialData: BioluminescentZoneGenerationResult? = null
        private set

    var activatedAt: Long? = null
        private set

    var generationStage = BioluminescentZoneGenerationStage.COLLECT_WATER_DOMAIN
        private set

    var failureReason: String? = null
        private set

    var generationCpuNanos: Long = 0L
        private set

    val tiles: List<BioluminescentZoneTile>
        get() = spatialData?.tiles ?: emptyList()

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

    val movingEntityCount: Int
        get() = movementWaves.movingSourceCount

    fun advanceGeneration(
        level: ClientLevel,
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maxTileCount: Int,
        gameTime: Long
    ) {
        val activeGenerator = generator ?: return
        activeGenerator.advance(level, textureManager, identifierFactory, maxTileCount)
        generationStage = activeGenerator.stage
        generationCpuNanos = activeGenerator.cpuNanos
        if (activeGenerator.stage == BioluminescentZoneGenerationStage.READY) {
            spatialData = checkNotNull(activeGenerator.takeCompletedResult())
            activatedAt = gameTime
            activeGenerator.close()
            generator = null
        } else if (activeGenerator.stage == BioluminescentZoneGenerationStage.FAILED) {
            failureReason = activeGenerator.failureReason ?: "echec de generation inconnu"
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

    fun lifecycleIntensityAt(renderGameTime: Double): Float {
        val start = activatedAt ?: return 0.0f
        val elapsed = (renderGameTime - start).coerceAtLeast(0.0)
        return lifecycleIntensity(elapsed).toFloat().coerceIn(0.0f, 1.0f)
    }

    fun localPulseIntensityAt(renderGameTime: Double, spatialPhase: Double): Float {
        val start = activatedAt ?: return 0.0f
        val elapsed = (renderGameTime - start).coerceAtLeast(0.0)
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

    fun movementWaveIntensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Float =
        movementWaves.intensityAt(worldX, worldZ, renderGameTime)

    fun movementWavesAffect(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Boolean = movementWaves.affects(minX, minZ, maxX, maxZ, renderGameTime)

    fun movementWaveVisibilityStrength(): Double = movementWaves.visibilityStrength

    fun isCompleteAt(gameTime: Long): Boolean {
        val start = activatedAt ?: return false
        return gameTime - start >= lifetime
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
        movementWaves.clear()
    }

    private fun lifecycleIntensity(elapsedTicks: Double): Double {
        if (elapsedTicks >= lifetime) return 0.0
        val progress = elapsedTicks / lifetime
        return when {
            progress < APPEARANCE_END -> ModUtilities.smooth(0.0, APPEARANCE_END, progress)
            progress < DISAPPEARANCE_START -> 1.0
            else -> 1.0 - ModUtilities.smooth(DISAPPEARANCE_START, 1.0, progress)
        }
    }

    private fun brightnessScaleFromSeed(): Double {
        val mixed = RandomSupport.mixStafford13(zoneSeed xor BRIGHTNESS_SEED_SALT)
        return 0.97 + ((mixed ushr 40) and 0xFFFFL).toDouble() / 65535.0 * 0.06
    }

}
