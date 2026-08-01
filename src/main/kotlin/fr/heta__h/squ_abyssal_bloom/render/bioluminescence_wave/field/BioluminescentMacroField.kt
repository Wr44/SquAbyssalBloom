package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopology
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
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
    private val noiseSampler: BioluminescentNoiseSampler,
    private val centerX: Double,
    private val centerZ: Double,
    private val envelopeRadiusAlong: Double,
    private val envelopeRadiusAcross: Double,
    private val envelopeExponent: Double,
    private val curvePhase: Double
) {
    private val anchorWorldX = domain.cells[domain.anchorIndex].waterPos.x + 0.5
    private val anchorWorldZ = domain.cells[domain.anchorIndex].waterPos.z + 0.5
    private val radialFadeStart = domain.geodesicRadius * RADIAL_FADE_START_RATIO
    private val radialFadeEnd = minOf(
        domain.analysisGeodesicRadius - RADIAL_DOMAIN_MARGIN,
        domain.geodesicRadius * RADIAL_FADE_END_RATIO
    ).coerceAtLeast(radialFadeStart + RADIAL_MINIMUM_FADE_WIDTH)

    val macroCellCount: Int
        get() = macroCells.count { it }

    companion object {
        const val CONNECTION_FIELD_STRENGTH = 0.74
        const val SKELETON_ENVELOPE_LIFT = 0.82
        const val CORE_VARIATION_SCALE = 0.74
        const val CORE_VARIATION_MINIMUM = 0.70
        const val CORE_VARIATION_RANGE = 0.42
        const val CONNECTION_NOISE_SCALE = 0.34
        const val CONNECTION_NOISE_MINIMUM = 0.74
        const val CONNECTION_NOISE_RANGE = 0.34
        const val COLONY_BODY_NOISE_SCALE = 0.105
        const val COLONY_BODY_DETAIL_SCALE = 0.235
        const val COLONY_BODY_NOISE_MINIMUM = 0.18
        const val COLONY_BODY_NOISE_MAXIMUM = 0.82
        const val COLONY_BODY_MINIMUM = 0.16
        const val COLONY_BODY_VARIATION = 0.48
        const val COLONY_BODY_RIDGE_WEIGHT = 0.12
        const val COLONY_BODY_CORE_WEIGHT = 0.24
        const val ENVELOPE_NOISE_SCALE = 0.22
        const val ENVELOPE_DETAIL_SCALE = 0.46
        const val ENVELOPE_NOISE_STRENGTH = 0.20
        const val ENVELOPE_DETAIL_STRENGTH = 0.07
        const val ENVELOPE_FADE_START = 0.68
        const val ENVELOPE_FADE_END = 1.06
        const val CURVE_STRENGTH = 0.11
        const val THRESHOLD_FEATHER_RATIO = 0.28
        const val RADIAL_FADE_START_RATIO = 0.66
        const val RADIAL_FADE_END_RATIO = 1.10
        const val RADIAL_DOMAIN_MARGIN = 2.0
        const val RADIAL_MINIMUM_FADE_WIDTH = 4.0
        const val RADIAL_NOISE_SCALE = 0.075
        const val RADIAL_NOISE_WARP = 0.24
        const val RADIAL_RAW_MINIMUM = 0.30
    }

    fun containsCell(cellIndex: Int): Boolean = macroCells[cellIndex]

    fun supportAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        return calculateSupport(worldX, worldZ, cellIndex, null)
    }

    internal fun sampleAt(
        worldX: Double,
        worldZ: Double,
        cellIndex: Int,
        result: BioluminescentMacroSample
    ) {
        result.support = calculateSupport(worldX, worldZ, cellIndex, result)
    }

    private fun calculateSupport(
        worldX: Double,
        worldZ: Double,
        cellIndex: Int,
        result: BioluminescentMacroSample?
    ): Double {
        val radialAttenuation = radialAttenuationAt(worldX, worldZ)
        if (radialAttenuation <= 0.0) {
            result?.clear()
            return 0.0
        }
        val raw = baseFieldAt(worldX, worldZ, cellIndex, result) * radialRawBias(radialAttenuation)
        if (raw <= 0.0) return 0.0
        val feather = max(Math.ulp(threshold) * 8.0, threshold * THRESHOLD_FEATHER_RATIO)
        return ModUtilities.smooth(threshold - feather, threshold + feather, raw) * radialAttenuation
    }

    fun rawAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        val radialAttenuation = radialAttenuationAt(worldX, worldZ)
        return baseFieldAt(worldX, worldZ, cellIndex, null) * radialRawBias(radialAttenuation)
    }

    private fun baseFieldAt(
        worldX: Double,
        worldZ: Double,
        cellIndex: Int,
        result: BioluminescentMacroSample?
    ): Double {
        val envelope = envelopeAt(worldX, worldZ)
        val core = coreInfluenceAt(worldX, worldZ)
        val coreVariation = noiseSampler.sampleLarge(
            (worldX - 61.0) * CORE_VARIATION_SCALE,
            (worldZ + 43.0) * CORE_VARIATION_SCALE
        )
        val variedCore = (core * (CORE_VARIATION_MINIMUM + coreVariation * CORE_VARIATION_RANGE))
            .coerceIn(0.0, 1.0)
        val connection = connectionInfluenceAt(worldX, worldZ, cellIndex)
        result?.core = core
        result?.connection = connection
        val colonyBody = colonyBodyAt(worldX, worldZ, core)
        val connectedCores = boundedUnion(
            boundedUnion(variedCore, colonyBody),
            connection * CONNECTION_FIELD_STRENGTH
        )
        val reinforcedEnvelope = maxOf(
            envelope,
            connection * SKELETON_ENVELOPE_LIFT
        )
        return reinforcedEnvelope * connectedCores
    }

    private fun radialAttenuationAt(worldX: Double, worldZ: Double): Double {
        val distance = hypot(worldX - anchorWorldX, worldZ - anchorWorldZ)
        val radialNoise = noiseSampler.sampleLarge(
            (worldX + 617.0) * RADIAL_NOISE_SCALE,
            (worldZ - 541.0) * RADIAL_NOISE_SCALE
        )
        val warpedDistance = distance * (1.0 + (radialNoise - 0.5) * RADIAL_NOISE_WARP)
        return 1.0 - ModUtilities.smooth(radialFadeStart, radialFadeEnd, warpedDistance)
    }

    private fun radialRawBias(radialAttenuation: Double): Double {
        return RADIAL_RAW_MINIMUM + radialAttenuation * (1.0 - RADIAL_RAW_MINIMUM)
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
        val modulation = CONNECTION_NOISE_MINIMUM + ModUtilities.smooth(0.18, 0.82, slowNoise) *
            CONNECTION_NOISE_RANGE
        return (skeletonInfluence * modulation).coerceIn(0.0, 1.0)
    }

    private fun colonyBodyAt(worldX: Double, worldZ: Double, core: Double): Double {
        val broadNoise = noiseSampler.sampleLarge(
            (worldX - 419.0) * COLONY_BODY_NOISE_SCALE,
            (worldZ + 367.0) * COLONY_BODY_NOISE_SCALE
        )
        val detailNoise = noiseSampler.sampleLarge(
            (worldX + 251.0) * COLONY_BODY_DETAIL_SCALE,
            (worldZ - 307.0) * COLONY_BODY_DETAIL_SCALE
        )
        val bodyNoise = broadNoise * 0.68 + detailNoise * 0.32
        val organicMass = ModUtilities.smooth(
            COLONY_BODY_NOISE_MINIMUM,
            COLONY_BODY_NOISE_MAXIMUM,
            bodyNoise
        )
        val ridge = 1.0 - abs(detailNoise * 2.0 - 1.0)
        val coreHalo = ModUtilities.smooth(0.02, 0.55, core)
        return (
            COLONY_BODY_MINIMUM + organicMass * COLONY_BODY_VARIATION +
                ridge * COLONY_BODY_RIDGE_WEIGHT + coreHalo * COLONY_BODY_CORE_WEIGHT
            ).coerceIn(0.0, 1.0)
    }

    private fun envelopeAt(worldX: Double, worldZ: Double): Double {
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
        return organicEnvelope
    }

    private fun boundedUnion(first: Double, second: Double): Double {
        return (1.0 - (1.0 - first) * (1.0 - second)).coerceIn(0.0, 1.0)
    }
}
