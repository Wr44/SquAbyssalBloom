package fr.heta__h.squ_abyssal_bloom.worldgen.terrain

import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalShapingContext
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.ColumnMods
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.ColumnSample
import net.minecraft.world.level.levelgen.DensityFunction
import kotlin.math.abs

object AbyssalFloorShaper {

    const val MUSHROOM_MIN = -1.05
    const val MUSHROOM_TRANSITION = 0.05
    const val CONTINENTAL_FULL = -0.92
    const val ABYSSAL_DEEP_MARGIN = 0.015
    const val OCEAN_MAX_CONT = -0.19

    const val SHALLOW_FLOOR_Y = 35
    const val DEEP_FLOOR_TARGET = 10
    const val TARGET_FLOOR_Y = -40
    const val GUYOT_CLEARANCE = 12
    const val SHALLOW_CLEARANCE = 8

    const val WARP_SCALE = 0.004
    const val WARP_AMP = 60.0

    const val TOPO_LARGE_SCALE = 0.003
    const val TOPO_SCALE = 0.010
    const val TOPO_MID_SCALE = 0.030
    const val WALL_SCALE = 0.050
    const val DETAIL_SCALE = 0.120
    const val MICRO_SCALE = 0.280

    const val TOPO_LARGE_AMP = 42.0
    const val TOPO_AMP = 20.0
    const val TOPO_MID_AMP = 11.0
    const val WALL_AMP = 14.0
    const val DETAIL_AMP = 7.0
    const val MICRO_AMP = 4.0
    const val WEIRDNESS_AMP = 11.0

    const val TRENCH_SCALE = 0.006
    const val TRENCH_THRESHOLD = 0.88
    const val TRENCH_DEPTH_AMP = 45.0

    const val SEAMOUNT_SCALE = 0.0015
    const val SEAMOUNT_THRESHOLD = 0.62
    const val SEAMOUNT_AMP = 70.0

    const val DEEP_LARGE_SCALE = 0.0015
    const val DEEP_LARGE_AMP = 16.0
    const val DEEP_TOPO_SCALE = 0.003
    const val DEEP_TOPO_AMP = 10.0
    const val DEEP_MID_SCALE = 0.008
    const val DEEP_MID_AMP = 6.0
    const val DEEP_RIDGE_SCALE = 0.007
    const val DEEP_RIDGE_AMP = 5.0
    const val DEEP_DETAIL_AMP = 4.0
    const val DEEP_MICRO_AMP = 2.5

    const val SHALLOW_COAST_FADE = -0.35
    const val SHALLOW_DRIFT_SCALE = 0.0004
    const val SHALLOW_DRIFT_AMP = 12.0
    const val SHALLOW_LARGE_SCALE = 0.001
    const val SHALLOW_LARGE_AMP = 22.0
    const val SHALLOW_TOPO_SCALE = 0.003
    const val SHALLOW_TOPO_AMP = 14.0
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
    const val ABYSSAL_BLEND_WIDTH = 0.02
    const val CONT_PERTURB_SCALE = 0.002
    const val CONT_PERTURB_AMP = 0.03
    const val SLOPE_PERTURB_SCALE = 0.012
    const val SLOPE_PERTURB_AMP = 0.35
    const val TRANSITION_LOCAL_SCALE = 0.025
    const val TRANSITION_LOCAL_AMP = 4.0

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

        return when {
            effectiveCont > shallowBlendHi -> shallowFloor(shaping, column, mods, finalDensityDf)
            effectiveCont > shallowBlendLo -> blendShallowDeep(shaping, column, mods, finalDensityDf, effectiveCont, shallowBlendLo, shallowBlendHi)
            effectiveCont > abyssalBlendHi -> deepFloor(shaping, column, mods)
            effectiveCont > shaping.abyssalDeepSplit -> blendDeepAbyssal(shaping, column, mods, effectiveCont, shaping.abyssalDeepSplit, abyssalBlendHi)
            else -> abyssalFloor(shaping, column, mods)
        }
    }

    private fun scanVanillaFloor(shaping: AbyssalShapingContext, column: ColumnSample, finalDensityDf: DensityFunction, downTo: Int): Int {
        for (y in shaping.seaLevel - 1 downTo downTo step 4) {
            if (finalDensityDf.compute(DensityFunction.SinglePointContext(column.worldX, y, column.worldZ)) > 0.0) {
                return y
            }
        }
        return shaping.seaLevel
    }

    private fun columnMods(shaping: AbyssalShapingContext, column: ColumnSample): ColumnMods {
        val x = column.worldX.toDouble()
        val z = column.worldZ.toDouble()
        val warpX = x + shaping.topoNoise.getValue(x * WARP_SCALE, 100.0, z * WARP_SCALE) * WARP_AMP
        val warpZ = z + shaping.topoNoise.getValue(x * WARP_SCALE, 200.0, z * WARP_SCALE) * WARP_AMP

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
            warpZ = warpZ
        )
    }

    private fun shallowComputedFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods): Int {
        val drift = shaping.topoNoise.getValue(mods.warpX * SHALLOW_DRIFT_SCALE, 1200.0, mods.warpZ * SHALLOW_DRIFT_SCALE) * SHALLOW_DRIFT_AMP
        val shallowLarge = shaping.topoNoise.getValue(mods.warpX * SHALLOW_LARGE_SCALE, 800.0, mods.warpZ * SHALLOW_LARGE_SCALE)
        val shallowTopo = shaping.topoNoise.getValue(mods.warpX * SHALLOW_TOPO_SCALE, 850.0, mods.warpZ * SHALLOW_TOPO_SCALE)
        val shallowMid = shaping.topoNoise.getValue(mods.warpX * SHALLOW_MID_SCALE, 1000.0, mods.warpZ * SHALLOW_MID_SCALE)
        val shallowRidge = shaping.wallNoise.getValue(mods.warpX * SHALLOW_RIDGE_SCALE, 1100.0, mods.warpZ * SHALLOW_RIDGE_SCALE)

        val terrain = (shallowLarge * SHALLOW_LARGE_AMP
                + shallowTopo * SHALLOW_TOPO_AMP
                + shallowMid * SHALLOW_MID_AMP
                + shallowRidge * SHALLOW_RIDGE_AMP * mods.weirdnessWallMod
                ) * mods.erosionFactor

        val detail = (mods.detailVal * SHALLOW_DETAIL_AMP + mods.microVal * SHALLOW_MICRO_AMP) * mods.humidityDetailMod

        return (SHALLOW_FLOOR_Y + drift + terrain + detail + mods.tempDepthMod).toInt()
            .coerceAtMost(shaping.seaLevel - SHALLOW_CLEARANCE)
            .coerceAtLeast(shaping.deepHardLimit)
    }

    private fun shallowFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods, finalDensityDf: DensityFunction): Int {
        val coastT = ((column.cont - OCEAN_MAX_CONT) / (SHALLOW_COAST_FADE - OCEAN_MAX_CONT)).coerceIn(0.0, 1.0)
        val computed = shallowComputedFloor(shaping, column, mods)
        if (coastT >= 1.0) return computed

        val vanillaFloor = scanVanillaFloor(shaping, column, finalDensityDf, SHALLOW_FLOOR_Y - 5)
        return (vanillaFloor + (computed - vanillaFloor) * coastT).toInt()
    }

    private fun deepFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods): Int {
        val deepLarge = shaping.topoNoise.getValue(mods.warpX * DEEP_LARGE_SCALE, 600.0, mods.warpZ * DEEP_LARGE_SCALE)
        val deepTopo = shaping.topoNoise.getValue(mods.warpX * DEEP_TOPO_SCALE, 300.0, mods.warpZ * DEEP_TOPO_SCALE)
        val deepMid = shaping.topoNoise.getValue(mods.warpX * DEEP_MID_SCALE, 700.0, mods.warpZ * DEEP_MID_SCALE)
        val deepRidge = shaping.wallNoise.getValue(mods.warpX * DEEP_RIDGE_SCALE, 400.0, mods.warpZ * DEEP_RIDGE_SCALE)

        val terrain = (deepLarge * DEEP_LARGE_AMP
                + deepTopo * DEEP_TOPO_AMP
                + deepMid * DEEP_MID_AMP
                + deepRidge * DEEP_RIDGE_AMP * mods.weirdnessWallMod
                ) * mods.erosionFactor

        val detail = (mods.detailVal * DEEP_DETAIL_AMP + mods.microVal * DEEP_MICRO_AMP) * mods.humidityDetailMod

        return (DEEP_FLOOR_TARGET + terrain + detail + mods.tempDepthMod).toInt()
            .coerceAtLeast(shaping.deepHardLimit)
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
        val shallowY = shallowFloor(shaping, column, mods, finalDensityDf)
        val deepY = deepFloor(shaping, column, mods)

        val slopeVar = shaping.wallNoise.getValue(mods.warpX * SLOPE_PERTURB_SCALE, 1600.0, mods.warpZ * SLOPE_PERTURB_SCALE) * SLOPE_PERTURB_AMP
        val rawT = ((effectiveCont - blendLo) / (blendHi - blendLo) + slopeVar * 0.5).coerceIn(0.0, 1.0)
        val smoothT = rawT * rawT * (3.0 - 2.0 * rawT)

        val localVar = shaping.detailNoise.getValue(
            column.worldX * TRANSITION_LOCAL_SCALE, 1400.0, column.worldZ * TRANSITION_LOCAL_SCALE
        ) * TRANSITION_LOCAL_AMP * (1.0 - abs(smoothT - 0.5) * 2.0)

        return (deepY + (shallowY - deepY) * smoothT + localVar).toInt()
    }

    private fun blendDeepAbyssal(
        shaping: AbyssalShapingContext,
        column: ColumnSample,
        mods: ColumnMods,
        effectiveCont: Double,
        blendLo: Double,
        blendHi: Double
    ): Int {
        val deepY = deepFloor(shaping, column, mods)
        val abyssalY = abyssalFloor(shaping, column, mods)

        val rawT = ((effectiveCont - blendLo) / (blendHi - blendLo)).coerceIn(0.0, 1.0)
        val smoothT = rawT * rawT * (3.0 - 2.0 * rawT)

        val localVar = shaping.detailNoise.getValue(
            column.worldX * TRANSITION_LOCAL_SCALE, 1700.0, column.worldZ * TRANSITION_LOCAL_SCALE
        ) * TRANSITION_LOCAL_AMP * (1.0 - abs(smoothT - 0.5) * 2.0)

        return (abyssalY + (deepY - abyssalY) * smoothT + localVar).toInt()
            .coerceAtLeast(shaping.abyssalHardLimit)
    }

    private fun abyssalFloor(shaping: AbyssalShapingContext, column: ColumnSample, mods: ColumnMods): Int {
        val rawT = ((column.cont - mods.effectiveAbyssalSplit) / (CONTINENTAL_FULL - mods.effectiveAbyssalSplit)).coerceIn(0.0, 1.0)
        val steepT = 1.0 - (1.0 - rawT) * (1.0 - rawT) * (1.0 - rawT)
        val mushroomProxT = ((column.cont - MUSHROOM_MIN) / MUSHROOM_TRANSITION).coerceIn(0.0, 1.0)
        val effectiveSteepT = steepT * mushroomProxT

        val shallowLift = (SHALLOW_FLOOR_Y - 1 - DEEP_FLOOR_TARGET) * (1.0 - mushroomProxT)
        val base = DEEP_FLOOR_TARGET + effectiveSteepT * (TARGET_FLOOR_Y - DEEP_FLOOR_TARGET) + shallowLift

        val topoLarge = shaping.topoNoise.getValue(mods.warpX * TOPO_LARGE_SCALE, 0.0, mods.warpZ * TOPO_LARGE_SCALE)
        val topo = shaping.topoNoise.getValue(mods.warpX * TOPO_SCALE, 0.0, mods.warpZ * TOPO_SCALE)
        val topoMid = shaping.topoNoise.getValue(mods.warpX * TOPO_MID_SCALE, 0.0, mods.warpZ * TOPO_MID_SCALE)

        val topoRaw = (topoLarge * TOPO_LARGE_AMP + topo * TOPO_AMP + topoMid * TOPO_MID_AMP) * effectiveSteepT * mods.erosionFactor
        val topoVar = topoRaw.coerceAtLeast(-6.0 * effectiveSteepT)
        val wallVar = mods.wallVal * WALL_AMP * effectiveSteepT * mods.erosionFactor * mods.weirdnessWallMod
        val detailVar = (mods.detailVal * DETAIL_AMP + mods.microVal * MICRO_AMP) * effectiveSteepT * mods.humidityDetailMod
        val weirdnessVar = column.ridges * WEIRDNESS_AMP * effectiveSteepT
        val tempVar = mods.tempDepthMod * effectiveSteepT

        val terrainBase = (base + topoVar + wallVar + detailVar + weirdnessVar + tempVar)

        val trenchT = trenchFactor(shaping, mods)
        val trenchBudget = (terrainBase - shaping.abyssalHardLimit).coerceAtLeast(0.0)
        val trenchVar = -trenchT * trenchT * TRENCH_DEPTH_AMP * effectiveSteepT * (trenchBudget / TRENCH_DEPTH_AMP).coerceIn(0.0, 1.0)
        val seamountVar = seamountHeight(shaping, column, effectiveSteepT) * (1.0 - trenchT)

        return (terrainBase + trenchVar + seamountVar).toInt()
            .coerceAtLeast(shaping.abyssalHardLimit)
            .coerceAtMost(shaping.seaLevel - GUYOT_CLEARANCE)
    }

    private fun trenchFactor(shaping: AbyssalShapingContext, mods: ColumnMods): Double {
        val raw = 1.0 - abs(shaping.wallNoise.getValue(mods.warpX * TRENCH_SCALE, 500.0, mods.warpZ * TRENCH_SCALE))
        return ((raw - TRENCH_THRESHOLD) / (1.0 - TRENCH_THRESHOLD)).coerceIn(0.0, 1.0)
    }

    private fun seamountHeight(shaping: AbyssalShapingContext, column: ColumnSample, steepT: Double): Double {
        val raw = shaping.topoNoise.getValue(column.worldX * SEAMOUNT_SCALE, 900.0, column.worldZ * SEAMOUNT_SCALE)
        val t = ((raw - SEAMOUNT_THRESHOLD) / (1.0 - SEAMOUNT_THRESHOLD)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t) * SEAMOUNT_AMP * steepT
    }
}