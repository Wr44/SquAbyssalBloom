package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water.BioluminescentWaterParticleOptions
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class BioluminescentBloomParticleEmitter {
    companion object {
        private const val AMBIENT_COUNT_PER_TICK = 5
        private const val PULSE_BURST_COUNT = 20
        private const val MIN_ALPHA = 0.5f
        private const val ALPHA_SPREAD = 0.4f
        private const val FRONT_MIN_RADIUS = 1.0
        private const val FRONT_BASE_ATTEMPTS = 6.0
        private const val FRONT_ATTEMPTS_PER_BLOCK = 2.0
        private const val FRONT_MAX_ATTEMPTS = 48
        private const val FRONT_THICKNESS = 1.8
        private const val FRONT_MAX_DISTANCE = 32.0
        private const val FRONT_MIN_ALPHA = 0.75f
        private const val FRONT_ALPHA_SPREAD = 0.25f
        private const val FRONT_COLOR_BLEND = 0.45
    }

    fun tickAmbient(
        level: ClientLevel,
        bloom: BioluminescentBloom,
        palette: BioluminescentPalette,
        intensity: Float
    ) {
        emit(level, bloom, palette, AMBIENT_COUNT_PER_TICK, intensity)
    }

    fun emitPulseBurst(
        level: ClientLevel,
        bloom: BioluminescentBloom,
        palette: BioluminescentPalette,
        intensity: Float
    ) {
        emit(level, bloom, palette, PULSE_BURST_COUNT, intensity)
    }

    fun emitPulseFront(
        level: ClientLevel,
        data: BioluminescentZoneGenerationResult,
        palette: BioluminescentPalette,
        originX: Double,
        originZ: Double,
        frontRadius: Double,
        intensity: Float
    ) {
        if (intensity <= 0.0f || frontRadius < FRONT_MIN_RADIUS) return
        val listener = Minecraft.getInstance().player ?: return

        val attempts = ((FRONT_BASE_ATTEMPTS + frontRadius * FRONT_ATTEMPTS_PER_BLOCK) *
            intensity * ModConfig.bioluminescenceBloomParticleDensity).toInt()
            .coerceAtMost(FRONT_MAX_ATTEMPTS)
        if (attempts <= 0) return

        val domain = data.domain
        val emission = data.emissionField
        val random = level.random

        repeat(attempts) {
            val angle = random.nextDouble() * PI * 2.0
            val distance = frontRadius + (random.nextDouble() - 0.5) * FRONT_THICKNESS
            val worldX = originX + cos(angle) * distance
            val worldZ = originZ + sin(angle) * distance
            if (
                ModUtilities.horizontalDistanceSqr(listener.x, listener.z, worldX, worldZ) >
                FRONT_MAX_DISTANCE * FRONT_MAX_DISTANCE
            ) return@repeat
            val cellIndex = domain.cellIndexAt(floor(worldX).toInt(), floor(worldZ).toInt()) ?: return@repeat
            if (!domain.isLocalCell(cellIndex)) return@repeat
            if (!emission.hasLuminousCell(cellIndex)) return@repeat

            val color = ModUtilities.lerpColor(
                palette.highlightColor,
                palette.accentColor,
                random.nextDouble() * FRONT_COLOR_BLEND
            )
            level.addParticle(
                BioluminescentWaterParticleOptions(
                    color,
                    FRONT_MIN_ALPHA + random.nextFloat() * FRONT_ALPHA_SPREAD
                ),
                worldX,
                domain.cells[cellIndex].surfaceY,
                worldZ,
                0.0, 0.0, 0.0
            )
        }
    }

    private fun emit(
        level: ClientLevel,
        bloom: BioluminescentBloom,
        palette: BioluminescentPalette,
        baseCount: Int,
        intensity: Float
    ) {
        if (intensity <= 0.0f) return
        val geometry = bloom.geometry ?: return
        val count = (baseCount * intensity * ModConfig.bioluminescenceBloomParticleDensity).toInt()
        if (count <= 0) return

        val random = level.random
        repeat(count) {
            val angle = random.nextDouble() * PI * 2.0
            val distance = sqrt(random.nextDouble()) * geometry.haloRadius
            val color = ModUtilities.lerpColor(palette.accentColor, palette.highlightColor, random.nextDouble())
            level.addParticle(
                BioluminescentWaterParticleOptions(color, MIN_ALPHA + random.nextFloat() * ALPHA_SPREAD),
                geometry.centerX + cos(angle) * distance,
                geometry.surfaceY,
                geometry.centerZ + sin(angle) * distance,
                0.0, 0.0, 0.0
            )
        }
    }
}
