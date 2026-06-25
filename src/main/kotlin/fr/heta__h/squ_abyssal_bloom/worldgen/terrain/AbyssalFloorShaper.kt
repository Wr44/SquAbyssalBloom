package fr.heta__h.squ_abyssal_bloom.worldgen.terrain

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalShapingContext
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.ColumnMods
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.ColumnSample
import net.minecraft.world.level.levelgen.DensityFunction
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

object AbyssalFloorShaper {

    const val MUSHROOM_MIN = -1.05
    const val MUSHROOM_TRANSITION = 0.05
    const val CONTINENTAL_FULL = -0.8
    const val ABYSSAL_DEEP_MARGIN = 0.015
    const val OCEAN_MAX_CONT = -0.19

    const val ABYSSAL_SOFT_ZONE = 15.0

    const val WARP_SCALE = 0.004
    const val WARP2_SCALE = 0.011

    const val TOPO_LARGE_SCALE = 0.003
    const val TOPO_SCALE = 0.010
    const val TOPO_MID_SCALE = 0.030
    const val WALL_SCALE = 0.050
    const val DETAIL_SCALE = 0.120
    const val MICRO_SCALE = 0.280

    const val FLOOR_CLAMP_FADE = 2.0
    const val FLOOR_CLAMP_BUMP_SCALE = 0.08
    const val FLOOR_CLAMP_BUMP_AMP = 5.0
    const val FLOOR_CLAMP_MID_SCALE = 0.20
    const val FLOOR_CLAMP_MID_AMP = 7.0
    const val FLOOR_CLAMP_FINE_SCALE = 0.34
    const val FLOOR_CLAMP_FINE_AMP = 10.0

    const val TRENCH_SCALE = 0.006

    const val ANISO_SCALE_X = 0.0014
    const val ANISO_SCALE_Z = 0.0052
    const val ANISO_AMP = 5.0

    const val SLOPE_SAMPLE_DELTA = 6.0
    const val SLOPE_ROUGHNESS_GAIN = 38.0
    const val SLOPE_ROUGHNESS_MAX = 2.1

    const val TERRACE_STEEP_CUTOFF = 0.72
    const val TERRACE_MASK_SCALE = 0.003
    const val TERRACE_MASK2_SCALE = 0.012
    const val TERRACE_MASK_AMP = 0.25
    const val TERRACE_MASK2_AMP = 0.25
    const val TERRACE_MASK_OFFSET = 0.70

    const val FAULT_SCALE = 0.0009

    const val DEEP_LARGE_SCALE = 0.0015
    const val DEEP_LARGE_AMP = 6.0
    const val DEEP_TOPO_SCALE = 0.003
    const val DEEP_TOPO_AMP = 14.0
    const val DEEP_MID_SCALE = 0.008
    const val DEEP_MID_AMP = 6.0
    const val DEEP_RIDGE_SCALE = 0.007
    const val DEEP_RIDGE_AMP = 5.0
    const val DEEP_DETAIL_AMP = 4.0
    const val DEEP_MICRO_AMP = 2.5
    const val DEEP_SWELL_SCALE = 0.0006
    const val DEEP_SWELL_AMP = 8.0
    const val DEEP_ANISO_AMP = 2.5

    const val SHALLOW_COAST_FADE = -0.30
    const val SHALLOW_LARGE_SCALE = 0.001
    const val SHALLOW_LARGE_AMP = 12.0
    const val SHALLOW_TOPO_SCALE = 0.003
    const val SHALLOW_TOPO_AMP = 18.0
    const val SHALLOW_MID_SCALE = 0.009
    const val SHALLOW_MID_AMP = 9.0
    const val SHALLOW_RIDGE_SCALE = 0.005
    const val SHALLOW_RIDGE_AMP = 7.0
    const val SHALLOW_DETAIL_AMP = 5.0
    const val SHALLOW_MICRO_AMP = 3.0

    const val TRANSITION_WEIRDNESS_AMP = 0.025
    const val TEMP_FLOOR_AMP = 3.5
    const val HUMIDITY_DETAIL_FACTOR = 0.18
    const val WEIRDNESS_WALL_FACTOR = 0.25

    const val BLEND_HALF_WIDTH = 0.04
    const val BLEND_RIDGE_FACTOR = 0.8
    const val SLOPE_POWER = 2.0
    const val ABYSSAL_BLEND_WIDTH = 0.02
    const val CONT_PERTURB_SCALE = 0.002
    const val CONT_PERTURB_AMP = 0.03
    const val SLOPE_PERTURB_SCALE = 0.012
    const val SLOPE_PERTURB_AMP = 0.35
    const val TRANSITION_LOCAL_SCALE = 0.025
    const val TRANSITION_LOCAL_AMP = 4.0
    const val ABYSSAL_COMPENSATION = -12.0

    fun isWithinOceanBand(shaping: AbyssalShapingContext, cont: Double): Boolean {
        return cont in MUSHROOM_MIN..OCEAN_MAX_CONT
    }

    fun computeFloor(shaping: AbyssalShapingContext, column: ColumnSample, finalDensityDf: DensityFunction): Int {
        val mods = columnMods(shaping, column)

        val contPerturb = shaping.wallNoise.getValue(column.worldX * CONT_PERTURB_SCALE, 1300.0, column.worldZ * CONT_PERTURB_SCALE) * CONT_PERTURB_AMP
        val effectiveCont = (column.cont + contPerturb).coerceIn(MUSHROOM_MIN, OCEAN_MAX_CONT)

        val effectiveBlendWidth = BLEND_HALF_WIDTH * (1.0 + abs(column.ridges) * BLEND_RIDGE_FACTOR)
        val shallowBlendLo = shaping.shallowDeepEdge - effectiveBlendWidth
        val shallowBlendHi = shaping.shallowDeepEdge + effectiveBlendWidth
        val abyssalBlendHi = shaping.abyssalDeepSplit + ABYSSAL_BLEND_WIDTH

        val baseFloor = when {
            effectiveCont > shallowBlendHi -> shallowFloor(shaping, column, mods, finalDensityDf, effectiveCont)
            effectiveCont > shallowBlendLo -> blendShallowDeep(shaping, column, mods, finalDensityDf, effectiveCont, shallowBlendLo, shallowBlendHi)
            effectiveCont > abyssalBlendHi -> deepFloor(shaping, column, mods)
            effectiveCont > shaping.abyssalDeepSplit -> blendDeepAbyssal(shaping, column, mods, finalDensityDf, effectiveCont, shaping.abyssalDeepSplit, abyssalBlendHi)
            else -> abyssalFloor(shaping, column, mods, finalDensityDf, effectiveCont)
        }

        val fault = faultOffset(shaping, column)
        return (baseFloor + fault).toInt().coerceAtLeast(shaping.abyssalHardLimit)
    }

    private fun faultOffset(shaping: AbyssalShapingContext, column: ColumnSample): Double {
        val faultThreshold = ModServerConfig.FAULT_THRESHOLD.get()
        val faultBlend = ModServerConfig.FAULT_BLEND.get()
        val line = shaping.wallNoise.getValue(column.worldX * FAULT_SCALE, 7700.0, column.worldZ * FAULT_SCALE)
        val dist = abs(line)

        if (dist > faultThreshold + faultBlend) return 0.0

        val centerSmoothWidth = 0.08
        val rawSide = line / centerSmoothWidth
        val clampedSide = rawSide.coerceIn(-1.0, 1.0)
        val side = clampedSide * clampedSide * (3.0 - 2.0 * abs(clampedSide)) * (if (line >= 0) 1.0 else -1.0)

        val t = ((faultThreshold + faultBlend - dist) / faultBlend).coerceIn(0.0, 1.0)
        val smooth = t * t * (3.0 - 2.0 * t)

        return side * ModServerConfig.FAULT_OFFSET_AMP.get() * smooth
    }

    private fun scanVanillaFloor(shaping: AbyssalShapingContext, column: ColumnSample, finalDensityDf: DensityFunction, downTo: Int): Int {
        for (y in shaping.seaLevel - 1 downTo downTo step 2) {
            if (finalDensityDf.compute(DensityFunction.SinglePointContext(column.worldX, y, column.worldZ)) > 0.0) {
                return if (finalDensityDf.compute(DensityFunction.SinglePointContext(column.worldX, y + 1, column.worldZ)) > 0.0) {
                    y + 1
                } else {
                    y
                }
            }
        }
        return downTo
    }

    private fun columnMods(shaping: AbyssalShapingContext, column: ColumnSample): ColumnMods {
        val x = column.worldX.toDouble()
        val z = column.worldZ.toDouble()
        val warpAmp = ModServerConfig.WARP_AMP.get()
        val warp2Amp = ModServerConfig.WARP2_AMP.get()
        val warp1X = x + shaping.topoNoise.getValue(x * WARP_SCALE, 100.0, z * WARP_SCALE) * warpAmp
        val warp1Z = z + shaping.topoNoise.getValue(x * WARP_SCALE, 200.0, z * WARP_SCALE) * warpAmp
        val warpX = warp1X + shaping.topoNoise.getValue(warp1X * WARP2_SCALE, 300.0, warp1Z * WARP2_SCALE) * warp2Amp
        val warpZ = warp1Z + shaping.topoNoise.getValue(warp1X * WARP2_SCALE, 400.0, warp1Z * WARP2_SCALE) * warp2Amp

        return ColumnMods(
            erosionFactor = (1.0 - column.erosion * 0.50).coerceIn(0.32, 1.55),
            tempDepthMod = -column.temperature * TEMP_FLOOR_AMP,
            humidityDetailMod = (1.0 + column.vegetation * HUMIDITY_DETAIL_FACTOR).coerceIn(0.75, 1.35),
            weirdnessWallMod = (1.0 + abs(column.ridges) * WEIRDNESS_WALL_FACTOR).coerceIn(0.80, 1.50),
            effectiveAbyssalSplit = shaping.abyssalDeepSplit + column.ridges * TRANSITION_WEIRDNESS_AMP,
            wallVal = shaping.wallNoise.getValue(warpX * WALL_SCALE, 0.0, warpZ * WALL_SCALE),
            detailVal = shaping.detailNoise.getValue(x * DETAIL_SCALE, 0.0, z * DETAIL_SCALE),
            microVal = shaping.detailNoise.getValue(x * MICRO_SCALE, 0.0, z * MICRO_SCALE),
            warpX = warpX,
            warpZ = warpZ,
            warp1X = warp1X,
            warp1Z = warp1Z
        )
    }

    private fun slopeRoughness(shaping: AbyssalShapingContext, wx: Double, wz: Double): Double {
        val c = shaping.topoNoise.getValue(wx * TOPO_LARGE_SCALE, 0.0, wz * TOPO_LARGE_SCALE)
        val dx = shaping.topoNoise.getValue((wx + SLOPE_SAMPLE_DELTA) * TOPO_LARGE_SCALE, 0.0, wz * TOPO_LARGE_SCALE)
        val dz = shaping.topoNoise.getValue(wx * TOPO_LARGE_SCALE, 0.0, (wz + SLOPE_SAMPLE_DELTA) * TOPO_LARGE_SCALE)
        val grad = abs(dx - c) + abs(dz - c)
        return (1.0 + grad * SLOPE_ROUGHNESS_GAIN).coerceIn(1.0, SLOPE_ROUGHNESS_MAX)
    }

    private fun anisotropyVar(shaping: AbyssalShapingContext, mods: ColumnMods, steepT: Double): Double {
        val v = shaping.topoNoise.getValue(mods.warpX * ANISO_SCALE_X, 5500.0, mods.warpZ * ANISO_SCALE_Z)
        return v * ANISO_AMP * steepT
    }

    private fun shallowComputedFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods): Int {
        val wx = mods.warp1X
        val wz = mods.warp1Z

        val shallowLarge = shaping.topoNoise.getValue(wx * SHALLOW_LARGE_SCALE, 8000.0, wz * SHALLOW_LARGE_SCALE)
        val shallowTopo = shaping.topoNoise.getValue(wx * SHALLOW_TOPO_SCALE, 5250.0, wz * SHALLOW_TOPO_SCALE)
        val shallowMid = shaping.topoNoise.getValue(wx * SHALLOW_MID_SCALE, 1900.0, wz * SHALLOW_MID_SCALE)
        val shallowRidge = shaping.wallNoise.getValue(wx * SHALLOW_RIDGE_SCALE, 11000.0, wz * SHALLOW_RIDGE_SCALE)

        val cLarge = shallowLarge * SHALLOW_LARGE_AMP
        val cTopo = shallowTopo * SHALLOW_TOPO_AMP
        val cMid = shallowMid * SHALLOW_MID_AMP
        val cRidge = shallowRidge * SHALLOW_RIDGE_AMP * mods.weirdnessWallMod
        val rawTerrain = cLarge + cTopo + cMid + cRidge
        val terrain = rawTerrain * mods.erosionFactor
        val detail = (mods.detailVal * SHALLOW_DETAIL_AMP + mods.microVal * SHALLOW_MICRO_AMP) * mods.humidityDetailMod

        val uncoerced = ModServerConfig.SHALLOW_FLOOR_Y.get() + terrain + detail + mods.tempDepthMod
        val result = uncoerced.toInt()
            .coerceAtMost(shaping.seaLevel - ModServerConfig.SHALLOW_CLEARANCE.get())
            .coerceAtLeast(shaping.deepHardLimit)

        return result
    }

    private fun shallowFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods, finalDensityDf: DensityFunction, effectiveCont: Double): Int {
        val rawT = ((effectiveCont - OCEAN_MAX_CONT) / (SHALLOW_COAST_FADE - OCEAN_MAX_CONT)).coerceIn(0.0, 1.0)
        val smoothT = rawT * rawT * (3.0 - 2.0 * rawT)
        val computed = shallowComputedFloor(shaping, column, mods)

        if (smoothT >= 1.0) return computed

        val vanillaFloor = scanVanillaFloor(shaping, column, finalDensityDf, shaping.deepHardLimit)
        return (vanillaFloor + (computed - vanillaFloor) * smoothT).toInt()
    }

    private fun deepFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods): Int {
        val wx = mods.warp1X
        val wz = mods.warp1Z

        val deepLarge = shaping.topoNoise.getValue(wx * DEEP_LARGE_SCALE, 6300.0, wz * DEEP_LARGE_SCALE)
        val deepTopo = shaping.topoNoise.getValue(wx * DEEP_TOPO_SCALE, 5202.0, wz * DEEP_TOPO_SCALE)
        val deepMid = shaping.topoNoise.getValue(wx * DEEP_MID_SCALE, 2800.0, wz * DEEP_MID_SCALE)
        val deepRidge = shaping.wallNoise.getValue(wx * DEEP_RIDGE_SCALE, 1600.0, wz * DEEP_RIDGE_SCALE)

        val cLarge = deepLarge * DEEP_LARGE_AMP
        val cTopo = deepTopo * DEEP_TOPO_AMP
        val cMid = deepMid * DEEP_MID_AMP
        val cRidge = deepRidge * DEEP_RIDGE_AMP * mods.weirdnessWallMod
        val rawTerrain = cLarge + cTopo + cMid + cRidge
        val terrain = rawTerrain * mods.erosionFactor
        val roughness = slopeRoughness(shaping, wx, wz)
        val detail = (mods.detailVal * DEEP_DETAIL_AMP + mods.microVal * DEEP_MICRO_AMP) * mods.humidityDetailMod * roughness
        val swell = shaping.topoNoise.getValue(wx * DEEP_SWELL_SCALE, 3900.0, wz * DEEP_SWELL_SCALE) * DEEP_SWELL_AMP
        val aniso = shaping.topoNoise.getValue(wx * ANISO_SCALE_X, 5500.0, wz * ANISO_SCALE_Z) * DEEP_ANISO_AMP

        val uncoerced = ModServerConfig.DEEP_FLOOR_TARGET.get() + terrain + detail + swell + aniso + mods.tempDepthMod
        val result = uncoerced.toInt().coerceAtLeast(shaping.deepHardLimit)

        return result
    }

    private fun blendShallowDeep(
        shaping: AbyssalShapingContext,
        column: ColumnSample,
        mods: ColumnMods,
        finalDensityDf: DensityFunction,
        effectiveCont: Double,
        blendLo: Double,
        blendHi: Double
    ): Int {
        val shallowY = shallowFloor(shaping, column, mods, finalDensityDf, effectiveCont)
        val deepY = deepFloor(shaping, column, mods)

        val slopeVar = shaping.wallNoise.getValue(mods.warp1X * SLOPE_PERTURB_SCALE, 1600.0, mods.warp1Z * SLOPE_PERTURB_SCALE) * SLOPE_PERTURB_AMP
        val rawT = ((effectiveCont - blendLo) / (blendHi - blendLo) + slopeVar * 0.5).coerceIn(0.0, 1.0)
        val biasedT = rawT.pow(SLOPE_POWER)
        val smoothT = biasedT * biasedT * (3.0 - 2.0 * biasedT)

        val localVar = shaping.detailNoise.getValue(
            column.worldX * TRANSITION_LOCAL_SCALE, 1400.0, column.worldZ * TRANSITION_LOCAL_SCALE
        ) * TRANSITION_LOCAL_AMP * (1.0 - abs(smoothT - 0.5) * 2.0)

        return (deepY + (shallowY - deepY) * smoothT + localVar).toInt()
    }

    private fun blendDeepAbyssal(
        shaping: AbyssalShapingContext,
        column: ColumnSample,
        mods: ColumnMods,
        finalDensityDf: DensityFunction,
        effectiveCont: Double,
        blendLo: Double,
        blendHi: Double
    ): Int {
        val deepY = deepFloor(shaping, column, mods)
        val abyssalY = abyssalFloor(shaping, column, mods, finalDensityDf, effectiveCont)

        val rawT = ((effectiveCont - blendLo) / (blendHi - blendLo)).coerceIn(0.0, 1.0)
        val smoothT = rawT * rawT * (3.0 - 2.0 * rawT)

        val localVar = shaping.detailNoise.getValue(
            column.worldX * TRANSITION_LOCAL_SCALE, 1700.0, column.worldZ * TRANSITION_LOCAL_SCALE
        ) * TRANSITION_LOCAL_AMP * (1.0 - abs(smoothT - 0.5) * 2.0)

        return (abyssalY + (deepY - abyssalY) * smoothT + localVar).toInt()
            .coerceAtLeast(shaping.abyssalHardLimit)
    }

    private fun softClampAbyssalFloor(value: Double, hardLimit: Int): Double {
        val limit = hardLimit.toDouble()
        val softStart = limit + ABYSSAL_SOFT_ZONE
        if (value >= softStart) return value
        val t = ((value - limit) / ABYSSAL_SOFT_ZONE).coerceIn(0.0, 1.0)
        return limit + ABYSSAL_SOFT_ZONE * t * t * (3.0 - 2.0 * t)
    }

    private fun abyssalFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods, finalDensityDf: DensityFunction, effectiveCont: Double = 0.0): Int {
        val rawT = ((effectiveCont - mods.effectiveAbyssalSplit) / (CONTINENTAL_FULL - mods.effectiveAbyssalSplit)).coerceIn(0.0, 1.0)
        val steepT = (1.0 - (1.0 - rawT) * (1.0 - rawT) * (1.0 - rawT)).pow(0.5)
        val mushroomProxT = ((effectiveCont - MUSHROOM_MIN) / MUSHROOM_TRANSITION).coerceIn(0.0, 1.0)
        val effectiveSteepT = steepT * mushroomProxT

        val localDeepY = deepFloor(shaping, column, mods).toDouble()

        val shallowLift = if (mushroomProxT < 0.99) {
            val mushroomFloor = scanVanillaFloor(shaping, column, finalDensityDf, shaping.deepHardLimit).toDouble()
            (mushroomFloor - localDeepY) * (1.0 - mushroomProxT)
        } else {
            0.0
        }

        val base = localDeepY + effectiveSteepT * (ModServerConfig.TARGET_FLOOR_Y.get().toDouble() - localDeepY) + shallowLift

        val topoLarge = shaping.topoNoise.getValue(mods.warpX * TOPO_LARGE_SCALE, 3700.0, mods.warpZ * TOPO_LARGE_SCALE)
        val topo = shaping.topoNoise.getValue(mods.warpX * TOPO_SCALE, 5202.0, mods.warpZ * TOPO_SCALE)
        val topoMid = shaping.topoNoise.getValue(mods.warpX * TOPO_MID_SCALE, 2800.0, mods.warpZ * TOPO_MID_SCALE)

        val cLargeAbyss = topoLarge * ModServerConfig.TOPO_LARGE_AMP.get()
        val cTopoAbyss = topo * ModServerConfig.TOPO_AMP.get()
        val cMidAbyss = topoMid * ModServerConfig.TOPO_MID_AMP.get()

        val topoCoarseRaw = cLargeAbyss * effectiveSteepT * mods.erosionFactor
        val topoFineRaw = (cTopoAbyss + cMidAbyss) * effectiveSteepT * mods.erosionFactor

        val wallAmp = ModServerConfig.WALL_AMP.get()
        val rawWall = mods.wallVal * wallAmp * effectiveSteepT * mods.erosionFactor * mods.weirdnessWallMod
        val wallExpectedAverage = (wallAmp * effectiveSteepT * mods.erosionFactor) * 0.25
        val wallVar = rawWall.coerceAtLeast(0.0) - wallExpectedAverage

        val roughness = slopeRoughness(shaping, mods.warpX, mods.warpZ)
        val detailVar = (mods.detailVal * ModServerConfig.DETAIL_AMP.get() + mods.microVal * ModServerConfig.MICRO_AMP.get()) * effectiveSteepT * mods.humidityDetailMod * roughness
        val weirdnessVar = column.ridges * ModServerConfig.WEIRDNESS_AMP.get() * effectiveSteepT
        val tempVar = mods.tempDepthMod * effectiveSteepT
        val anisoVar = anisotropyVar(shaping, mods, effectiveSteepT)

        val interpolationCompensation = ABYSSAL_COMPENSATION * effectiveSteepT

        val terrainCoarse = base + topoCoarseRaw
        val terrainFine = topoFineRaw + wallVar + detailVar + weirdnessVar + tempVar + anisoVar + interpolationCompensation
        val terrainBase = terrainCoarse + terrainFine

        val terraceMaskThreshold = ModServerConfig.TERRACE_MASK_THRESHOLD.get()
        val maskCoarse = shaping.topoNoise.getValue(mods.warpX * TERRACE_MASK_SCALE, 6100.0, mods.warpZ * TERRACE_MASK_SCALE)
        val maskFine = shaping.topoNoise.getValue(mods.warpX * TERRACE_MASK2_SCALE, 6150.0, mods.warpZ * TERRACE_MASK2_SCALE)
        val terraceMask = maskCoarse * TERRACE_MASK_AMP + maskFine * TERRACE_MASK2_AMP + TERRACE_MASK_OFFSET
        val terraceActive = terraceMask > terraceMaskThreshold && effectiveSteepT < TERRACE_STEEP_CUTOFF

        val terraceStep = ModServerConfig.TERRACE_STEP.get()
        val rawQuantized = floor(terrainCoarse / terraceStep) * terraceStep
        val coarseQuantized = rawQuantized.coerceAtLeast(shaping.abyssalHardLimit.toDouble())
        val finalTerrainBase = if (terraceActive) coarseQuantized + terrainFine else terrainBase

        val trenchT = trenchFactor(shaping, mods)
        val trenchDepthAmp = ModServerConfig.TRENCH_DEPTH_AMP.get()
        val trenchBudget = (finalTerrainBase - shaping.abyssalHardLimit).coerceAtLeast(0.0)
        val trenchVar = -trenchT * trenchT * trenchDepthAmp * effectiveSteepT * (trenchBudget / trenchDepthAmp).coerceIn(0.0, 1.0)

        val finalY = if (trenchT > 0.0) {
            finalTerrainBase + trenchVar
        } else {
            softClampAbyssalFloor(finalTerrainBase, shaping.abyssalHardLimit)
        }

        val softClamped = softClampAbyssalFloor(finalY, shaping.abyssalHardLimit)
        val clampProx = ((shaping.abyssalHardLimit + FLOOR_CLAMP_FADE - softClamped) / FLOOR_CLAMP_FADE).coerceIn(0.0, 1.0)

        val bumpBase = abs(shaping.detailNoise.getValue(mods.warpX * FLOOR_CLAMP_BUMP_SCALE, 9900.0, mods.warpZ * FLOOR_CLAMP_BUMP_SCALE)) * FLOOR_CLAMP_BUMP_AMP
        val bumpMid = abs(shaping.topoNoise.getValue(mods.warpX * FLOOR_CLAMP_MID_SCALE, 9920.0, mods.warpZ * FLOOR_CLAMP_MID_SCALE) * FLOOR_CLAMP_MID_AMP)
        val bumpFine = shaping.detailNoise.getValue(mods.warpX * FLOOR_CLAMP_FINE_SCALE, 9960.0, mods.warpZ * FLOOR_CLAMP_FINE_SCALE) * FLOOR_CLAMP_FINE_AMP

        val floorBump = (bumpBase + bumpMid + bumpFine) * clampProx

        return (softClamped + floorBump).toInt()
            .coerceAtLeast(shaping.abyssalHardLimit)
            .coerceAtMost(shaping.seaLevel - ModServerConfig.SHALLOW_CLEARANCE.get())
    }

    private fun trenchFactor(shaping: AbyssalShapingContext, mods: ColumnMods): Double {
        val raw = 1.0 - abs(shaping.wallNoise.getValue(mods.warpX * TRENCH_SCALE, 500.0, mods.warpZ * TRENCH_SCALE))
        val trenchThreshold = ModServerConfig.TRENCH_THRESHOLD.get()
        return ((raw - trenchThreshold) / (1.0 - trenchThreshold)).coerceIn(0.0, 1.0)
    }
}