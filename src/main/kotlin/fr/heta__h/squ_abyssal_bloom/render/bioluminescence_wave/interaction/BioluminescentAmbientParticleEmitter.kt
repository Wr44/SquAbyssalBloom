package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water.BioluminescentWaterParticleOptions
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import net.minecraft.client.multiplayer.ClientLevel

class BioluminescentAmbientParticleEmitter {
    companion object {
        private const val SPAWN_ATTEMPTS_PER_TICK = 10
        private const val SPAWN_CHANCE = 0.6
        private const val MIN_ALPHA_TO_SPAWN = 0.05f
    }

    fun tick(level: ClientLevel, data: BioluminescentZoneGenerationResult, intensity: Float) {
        if (intensity <= 0f) return
        val domain = data.domain
        val localIndices = domain.localCellIndices
        if (localIndices.isEmpty()) return
        val emission = data.emissionField
        val random = level.random

        repeat(SPAWN_ATTEMPTS_PER_TICK) {
            if (random.nextFloat() >= SPAWN_CHANCE * intensity) return@repeat
            val cellIndex = localIndices[random.nextInt(localIndices.size)]
            if (!emission.hasLuminousCell(cellIndex)) return@repeat

            val localPixelX = random.nextInt(BioluminescentEmissionField.PIXELS_PER_BLOCK)
            val localPixelZ = random.nextInt(BioluminescentEmissionField.PIXELS_PER_BLOCK)
            val alpha = emission.alphaAt(cellIndex, localPixelX, localPixelZ)
            if (alpha < MIN_ALPHA_TO_SPAWN) return@repeat
            val color = emission.colorAt(cellIndex, localPixelX, localPixelZ)

            val cell = domain.cells[cellIndex]
            val worldX = cell.waterPos.x +
                (localPixelX + random.nextFloat()).toDouble() / BioluminescentEmissionField.PIXELS_PER_BLOCK
            val worldZ = cell.waterPos.z +
                (localPixelZ + random.nextFloat()).toDouble() / BioluminescentEmissionField.PIXELS_PER_BLOCK

            level.addParticle(
                BioluminescentWaterParticleOptions(color, alpha),
                worldX,
                cell.surfaceY,
                worldZ,
                0.0, 0.0, 0.0
            )
        }
    }
}
