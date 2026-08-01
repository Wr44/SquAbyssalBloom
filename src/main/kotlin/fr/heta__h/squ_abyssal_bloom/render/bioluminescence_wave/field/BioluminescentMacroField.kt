package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopology
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class BioluminescentMacroField internal constructor(
    val domain: BioluminescentWaterDomain,
    val topology: BioluminescentTopology,
    val targetCoverage: Double,
    val achievedCoverage: Double,
    val threshold: Double,
    private val macroCells: BooleanArray,
    private val preset: BioluminescentZonePreset,
    private val noiseSampler: BioluminescentNoiseSampler,
    private val centerX: Double,
    private val centerZ: Double,
    private val envelopeRadiusAlong: Double,
    private val envelopeRadiusAcross: Double,
    private val envelopeExponent: Double,
    private val curvePhase: Double
) {
    val macroCellCount: Int
        get() = macroCells.count { it }

    companion object {
        const val CONNECTION_FIELD_STRENGTH = 0.96
        const val SKELETON_ENVELOPE_LIFT = 1.0
        const val CORE_VARIATION_SCALE = 0.74
        const val CORE_VARIATION_MINIMUM = 0.70
        const val CORE_VARIATION_RANGE = 0.42
        const val CONNECTION_NOISE_SCALE = 0.34
        const val ENVELOPE_NOISE_SCALE = 0.22
        const val ENVELOPE_DETAIL_SCALE = 0.46
        const val ENVELOPE_NOISE_STRENGTH = 0.20
        const val ENVELOPE_DETAIL_STRENGTH = 0.07
        const val ENVELOPE_FADE_START = 0.68
        const val ENVELOPE_FADE_END = 1.06
        const val CURVE_STRENGTH = 0.11
        const val THRESHOLD_FEATHER_RATIO = 0.28
        const val BOUNDARY_FADE_NOISE_SCALE = 0.31
        const val BOUNDARY_FADE_WARP = 2.4
    }

    fun containsCell(cellIndex: Int): Boolean = macroCells[cellIndex]

    fun supportAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        val raw = rawAt(worldX, worldZ, cellIndex)
        if (raw <= 0.0) return 0.0
        val feather = max(Math.ulp(threshold) * 8.0, threshold * THRESHOLD_FEATHER_RATIO)
        return ModUtilities.smooth(threshold - feather, threshold + feather, raw)
    }

    fun rawAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        val envelope = envelopeAt(worldX, worldZ, cellIndex)
        val core = coreInfluenceAt(worldX, worldZ)
        val coreVariation = noiseSampler.sampleLarge(
            (worldX - 61.0) * CORE_VARIATION_SCALE,
            (worldZ + 43.0) * CORE_VARIATION_SCALE
        )
        val variedCore = (core * (CORE_VARIATION_MINIMUM + coreVariation * CORE_VARIATION_RANGE))
            .coerceIn(0.0, 1.0)
        val connection = connectionInfluenceAt(worldX, worldZ, cellIndex)
        val connectedCores = boundedUnion(variedCore, connection * CONNECTION_FIELD_STRENGTH)
        val artificialBoundaryFade = artificialBoundaryFadeAt(cellIndex)
        val reinforcedEnvelope = maxOf(
            envelope,
            connection * SKELETON_ENVELOPE_LIFT * artificialBoundaryFade
        )
        return reinforcedEnvelope * connectedCores
    }

    fun coreInfluenceAt(worldX: Double, worldZ: Double): Double {
        var inverseProduct = 1.0
        for (core in topology.cores) {
            inverseProduct *= 1.0 - core.influenceAt(worldX, worldZ)
        }
        return 1.0 - inverseProduct
    }

    fun connectionInfluenceAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        val skeletonInfluence = topology.skeleton.influenceAt(worldX, worldZ, cellIndex)
        if (skeletonInfluence <= 0.0) return 0.0
        val slowNoise = noiseSampler.sampleLarge(
            (worldX + 173.0) * CONNECTION_NOISE_SCALE,
            (worldZ - 211.0) * CONNECTION_NOISE_SCALE
        )
        return skeletonInfluence * (0.86 + slowNoise * 0.14)
    }

    private fun envelopeAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        val directionX = topology.coastDirectionX
        val directionZ = topology.coastDirectionZ
        val deltaX = worldX - centerX
        val deltaZ = worldZ - centerZ
        val along = deltaX * directionX + deltaZ * directionZ
        var across = -deltaX * directionZ + deltaZ * directionX
        across -= sin(along / envelopeRadiusAlong * PI + curvePhase) * envelopeRadiusAcross * CURVE_STRENGTH
        val normalized = (
            (abs(along) / envelopeRadiusAlong).pow(envelopeExponent) +
                (abs(across) / envelopeRadiusAcross).pow(envelopeExponent)
            ).pow(1.0 / envelopeExponent)
        val edgeNoise = noiseSampler.sampleLarge(
            worldX * ENVELOPE_NOISE_SCALE,
            worldZ * ENVELOPE_NOISE_SCALE
        )
        val detailWarp = noiseSampler.sampleDetail(
            (worldX - 97.0) * ENVELOPE_DETAIL_SCALE,
            (worldZ + 137.0) * ENVELOPE_DETAIL_SCALE
        )
        val edgeShift = (edgeNoise - 0.5) * ENVELOPE_NOISE_STRENGTH +
            (detailWarp - 0.5) * ENVELOPE_DETAIL_STRENGTH
        val organicEnvelope = 1.0 - ModUtilities.smooth(
            ENVELOPE_FADE_START + edgeShift,
            ENVELOPE_FADE_END + edgeShift,
            normalized
        )
        return organicEnvelope * artificialBoundaryFadeAt(cellIndex)
    }

    private fun artificialBoundaryFadeAt(cellIndex: Int): Double {
        val distance = domain.artificialBoundaryDistance[cellIndex].toDouble()
        if (distance <= 0.0) return 0.0
        val position = domain.cells[cellIndex].waterPos
        val boundaryNoise = noiseSampler.sampleLarge(
            (position.x - 283.0) * BOUNDARY_FADE_NOISE_SCALE,
            (position.z + 229.0) * BOUNDARY_FADE_NOISE_SCALE
        )
        val interiorProgress = ModUtilities.smooth(0.0, preset.artificialBoundaryFade, distance)
        val warpedDistance = distance +
            (boundaryNoise - 0.5) * BOUNDARY_FADE_WARP * interiorProgress
        return ModUtilities.smooth(
            0.0,
            preset.artificialBoundaryFade,
            warpedDistance
        )
    }

    private fun boundedUnion(first: Double, second: Double): Double {
        return (1.0 - (1.0 - first) * (1.0 - second)).coerceIn(0.0, 1.0)
    }
}
