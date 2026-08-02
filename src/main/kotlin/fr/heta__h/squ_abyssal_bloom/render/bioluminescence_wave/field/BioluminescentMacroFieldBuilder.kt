package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.core.BioluminescentCore
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopology
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.PI
import kotlin.math.ceil

class BioluminescentMacroFieldBuilder(
    private val domain: BioluminescentWaterDomain,
    private val topology: BioluminescentTopology,
    private val preset: BioluminescentZonePreset,
    private val zoneSeed: Long,
    private val targetCoverage: Double
) {
    companion object {
        const val ENVELOPE_SEED_SALT = 0x3C6EF372FE94F82AL
        const val EXPONENT_SALT = 0x254FF53A5F1D36F1L
        const val CURVE_SALT = 0x510E527FADE682D1L
        const val MAX_ENVELOPE_EXPANSIONS = 3
        const val ENVELOPE_EXPANSION_FACTOR = 1.18
        const val ENVELOPE_DOMAIN_MARGIN = 2.0
    }

    private val sampler = BioluminescentNoiseSampler(zoneSeed)
    private val rawCellValues = DoubleArray(domain.size)
    private val centerX = topology.cores.sumOf(BioluminescentCore::worldX) / topology.cores.size
    private val centerZ = topology.cores.sumOf(BioluminescentCore::worldZ) / topology.cores.size
    private val maxEnvelopeRadius =
        (domain.analysisGeodesicRadius - ENVELOPE_DOMAIN_MARGIN).coerceAtLeast(1.0) /
            BioluminescentMacroField.ENVELOPE_FADE_END
    private var envelopeRadiusAlong: Double
    private var envelopeRadiusAcross: Double
    private val envelopeExponent: Double
    private val curvePhase: Double
    private val requestedTargetCount = (domain.localSize * targetCoverage)
        .toInt().coerceIn(1, domain.localSize)
    private var probeField: BioluminescentMacroField
    private var cursor = 0
    private var expansionPass = 0
    private var samplingComplete = false

    val complete: Boolean
        get() = samplingComplete

    init {
        val mixed = RandomSupport.mixStafford13(zoneSeed xor ENVELOPE_SEED_SALT)
        val first = ModUtilities.stableUnitValue(mixed)
        val second = ModUtilities.stableUnitValue(RandomSupport.mixStafford13(mixed))
        val coverageExpansion = 0.82 + targetCoverage * 0.45
        envelopeRadiusAlong = minOf(
            domain.geodesicRadius * (0.92 + first * 0.18) * coverageExpansion,
            maxEnvelopeRadius
        )
        envelopeRadiusAcross = minOf(
            domain.geodesicRadius * (0.78 + second * 0.20) * coverageExpansion,
            maxEnvelopeRadius
        )
        envelopeExponent = 2.2 +
            ModUtilities.stableUnitValue(RandomSupport.mixStafford13(mixed xor EXPONENT_SALT)) * 2.4
        curvePhase = ModUtilities.stableUnitValue(
            RandomSupport.mixStafford13(mixed xor CURVE_SALT)
        ) * PI * 2.0
        probeField = provisionalField(0.0, 0.0, BooleanArray(domain.size))
    }

    fun advance(budget: Int) {
        if (samplingComplete) return
        val end = minOf(domain.size, cursor + budget)
        for (index in cursor until end) {
            val position = domain.cells[index].waterPos
            rawCellValues[index] = probeField.rawAt(position.x + 0.5, position.z + 0.5, index)
        }
        cursor = end
        if (cursor >= domain.size) finishSamplingPass()
    }

    fun build(): BioluminescentMacroField {
        check(complete)
        val positiveCount = domain.localCellIndices.count { index -> rawCellValues[index] > 0.0 }
        val minTargetCount = ceil(domain.localSize * preset.macroCoverageRange.start)
            .toInt().coerceIn(1, domain.localSize)
        check(positiveCount >= minTargetCount) {
            "enveloppe macroscopique limitee a " +
                "${(positiveCount.toDouble() / domain.localSize * 100.0).toInt()}% du domaine local"
        }
        val targetCount = minOf(requestedTargetCount, positiveCount)
        val skeletonMask = BooleanArray(domain.size)
        topology.skeleton.cellIndices.forEach { index -> skeletonMask[index] = true }
        val mask = skeletonMask.copyOf()
        val mandatoryLocalCount = topology.skeleton.cellIndices.count(domain::isLocalCell)
        val remainingTargetCount = (targetCount - mandatoryLocalCount).coerceAtLeast(0)
        val rankedCandidates = domain.localCellIndices
            .asSequence()
            .filter { index -> !skeletonMask[index] && rawCellValues[index] > 0.0 }
            .sortedWith(compareByDescending<Int> { index -> rawCellValues[index] }.thenBy { it })
            .toList()
        check(rankedCandidates.size >= remainingTargetCount) {
            "support macroscopique insuffisant hors squelette"
        }
        for (candidateIndex in 0 until remainingTargetCount) {
            mask[rankedCandidates[candidateIndex]] = true
        }
        val threshold = if (remainingTargetCount > 0) {
            rawCellValues[rankedCandidates[remainingTargetCount - 1]]
        } else {
            topology.skeleton.cellIndices
                .asSequence()
                .map { index -> rawCellValues[index] }
                .filter { value -> value > 0.0 }
                .minOrNull() ?: Double.MIN_VALUE
        }
        check(threshold > 0.0) {
            "enveloppe macroscopique trop petite pour ${(targetCoverage * 100.0).toInt()}%"
        }
        val achieved = domain.localCellIndices.count { index -> mask[index] }.toDouble() / domain.localSize
        return provisionalField(threshold, achieved, mask)
    }

    private fun finishSamplingPass() {
        val positiveCount = domain.localCellIndices.count { index -> rawCellValues[index] > 0.0 }
        if (positiveCount >= requestedTargetCount || expansionPass >= MAX_ENVELOPE_EXPANSIONS) {
            samplingComplete = true
            return
        }
        expansionPass++
        envelopeRadiusAlong = minOf(
            envelopeRadiusAlong * ENVELOPE_EXPANSION_FACTOR,
            maxEnvelopeRadius
        )
        envelopeRadiusAcross = minOf(
            envelopeRadiusAcross * ENVELOPE_EXPANSION_FACTOR,
            maxEnvelopeRadius
        )
        rawCellValues.fill(0.0)
        cursor = 0
        probeField = provisionalField(0.0, 0.0, BooleanArray(domain.size))
    }

    private fun provisionalField(
        threshold: Double,
        achievedCoverage: Double,
        mask: BooleanArray
    ): BioluminescentMacroField {
        return BioluminescentMacroField(
            domain,
            topology,
            targetCoverage,
            achievedCoverage,
            threshold,
            mask,
            sampler,
            centerX,
            centerZ,
            envelopeRadiusAlong,
            envelopeRadiusAcross,
            envelopeExponent,
            curvePhase
        )
    }

}
