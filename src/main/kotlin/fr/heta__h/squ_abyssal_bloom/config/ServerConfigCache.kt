package fr.heta__h.squ_abyssal_bloom.config

import net.neoforged.fml.loading.FMLEnvironment
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist

object ServerConfigCache {
    var strictBarnacleSpawning: Boolean = true
    var shallowDeepBoundary: Double = -0.45
    var deepAbyssalBoundary: Double = -0.70
    var shallowFloorY: Int = 32
    var deepFloorTarget: Int = 11
    var targetFloorY: Int = -35
    var shallowClearance: Int = 8
    var guyotClearance: Int = 5
    var warpAmp: Double = 60.0
    var warp2Amp: Double = 26.0
    var topoLargeAmp: Double = 55.0
    var topoAmp: Double = 24.0
    var topoMidAmp: Double = 11.0
    var wallAmp: Double = 14.0
    var detailAmp: Double = 7.0
    var microAmp: Double = 4.0
    var weirdnessAmp: Double = 11.0
    var faultThreshold: Double = 0.012
    var faultBlend: Double = 0.045
    var faultOffsetAmp: Double = 10.0
    var trenchThreshold: Double = 0.88
    var trenchDepthAmp: Double = 10.0
    var seamountThreshold: Double = 0.62
    var seamountAmp: Double = 70.0
    var terraceStep: Double = 13.0
    var terraceMaskThreshold: Double = 0.80

    fun update(data: ServerConfigData) {
        strictBarnacleSpawning = data.strictBarnacleSpawning
        shallowDeepBoundary = data.shallowDeepBoundary
        deepAbyssalBoundary = data.deepAbyssalBoundary
        shallowFloorY = data.shallowFloorY
        deepFloorTarget = data.deepFloorTarget
        targetFloorY = data.targetFloorY
        shallowClearance = data.shallowClearance
        guyotClearance = data.guyotClearance
        warpAmp = data.warpAmp
        warp2Amp = data.warp2Amp
        topoLargeAmp = data.topoLargeAmp
        topoAmp = data.topoAmp
        topoMidAmp = data.topoMidAmp
        wallAmp = data.wallAmp
        detailAmp = data.detailAmp
        microAmp = data.microAmp
        weirdnessAmp = data.weirdnessAmp
        faultThreshold = data.faultThreshold
        faultBlend = data.faultBlend
        faultOffsetAmp = data.faultOffsetAmp
        trenchThreshold = data.trenchThreshold
        trenchDepthAmp = data.trenchDepthAmp
        seamountThreshold = data.seamountThreshold
        seamountAmp = data.seamountAmp
        terraceStep = data.terraceStep
        terraceMaskThreshold = data.terraceMaskThreshold
    }

    fun syncFromSpec() {
        update(ServerConfigData.fromSpec())
    }

    fun isSingleplayer(): Boolean {
        return if (FMLEnvironment.getDist() == Dist.CLIENT) {
            val mc = Minecraft.getInstance()
            mc.isLocalServer || mc.connection == null
        } else {
            false
        }
    }

    fun toData(): ServerConfigData = ServerConfigData(
        strictBarnacleSpawning = strictBarnacleSpawning,
        shallowDeepBoundary = shallowDeepBoundary,
        deepAbyssalBoundary = deepAbyssalBoundary,
        shallowFloorY = shallowFloorY,
        deepFloorTarget = deepFloorTarget,
        targetFloorY = targetFloorY,
        shallowClearance = shallowClearance,
        guyotClearance = guyotClearance,
        warpAmp = warpAmp,
        warp2Amp = warp2Amp,
        topoLargeAmp = topoLargeAmp,
        topoAmp = topoAmp,
        topoMidAmp = topoMidAmp,
        wallAmp = wallAmp,
        detailAmp = detailAmp,
        microAmp = microAmp,
        weirdnessAmp = weirdnessAmp,
        faultThreshold = faultThreshold,
        faultBlend = faultBlend,
        faultOffsetAmp = faultOffsetAmp,
        trenchThreshold = trenchThreshold,
        trenchDepthAmp = trenchDepthAmp,
        seamountThreshold = seamountThreshold,
        seamountAmp = seamountAmp,
        terraceStep = terraceStep,
        terraceMaskThreshold = terraceMaskThreshold,
    )

    val effectiveStrictBarnacleSpawning: Boolean
        get() = if (isSingleplayer()) ModServerConfig.STRICT_BARNACLE_SPAWNING.get() else strictBarnacleSpawning

    val effectiveShallowDeep: Float
        get() = (if (isSingleplayer()) ModServerConfig.SHALLOW_DEEP_BOUNDARY.get() else shallowDeepBoundary).toFloat()

    val effectiveDeepAbyssal: Float
        get() = (if (isSingleplayer()) ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get() else deepAbyssalBoundary).toFloat()

    val effectiveShallowFloorY: Int
        get() = if (isSingleplayer()) ModServerConfig.SHALLOW_FLOOR_Y.get() else shallowFloorY

    val effectiveDeepFloorTarget: Int
        get() = if (isSingleplayer()) ModServerConfig.DEEP_FLOOR_TARGET.get() else deepFloorTarget

    val effectiveTargetFloorY: Int
        get() = if (isSingleplayer()) ModServerConfig.TARGET_FLOOR_Y.get() else targetFloorY

    val effectiveShallowClearance: Int
        get() = if (isSingleplayer()) ModServerConfig.SHALLOW_CLEARANCE.get() else shallowClearance

    val effectiveGuyotClearance: Int
        get() = if (isSingleplayer()) ModServerConfig.GUYOT_CLEARANCE.get() else guyotClearance

    val effectiveWarpAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.WARP_AMP.get() else warpAmp

    val effectiveWarp2Amp: Double
        get() = if (isSingleplayer()) ModServerConfig.WARP2_AMP.get() else warp2Amp

    val effectiveTopoLargeAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.TOPO_LARGE_AMP.get() else topoLargeAmp

    val effectiveTopoAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.TOPO_AMP.get() else topoAmp

    val effectiveTopoMidAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.TOPO_MID_AMP.get() else topoMidAmp

    val effectiveWallAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.WALL_AMP.get() else wallAmp

    val effectiveDetailAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.DETAIL_AMP.get() else detailAmp

    val effectiveMicroAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.MICRO_AMP.get() else microAmp

    val effectiveWeirdnessAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.WEIRDNESS_AMP.get() else weirdnessAmp

    val effectiveFaultThreshold: Double
        get() = if (isSingleplayer()) ModServerConfig.FAULT_THRESHOLD.get() else faultThreshold

    val effectiveFaultBlend: Double
        get() = if (isSingleplayer()) ModServerConfig.FAULT_BLEND.get() else faultBlend

    val effectiveFaultOffsetAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.FAULT_OFFSET_AMP.get() else faultOffsetAmp

    val effectiveTrenchThreshold: Double
        get() = if (isSingleplayer()) ModServerConfig.TRENCH_THRESHOLD.get() else trenchThreshold

    val effectiveTrenchDepthAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.TRENCH_DEPTH_AMP.get() else trenchDepthAmp

    val effectiveSeamountThreshold: Double
        get() = if (isSingleplayer()) ModServerConfig.SEAMOUNT_THRESHOLD.get() else seamountThreshold

    val effectiveSeamountAmp: Double
        get() = if (isSingleplayer()) ModServerConfig.SEAMOUNT_AMP.get() else seamountAmp

    val effectiveTerraceStep: Double
        get() = if (isSingleplayer()) ModServerConfig.TERRACE_STEP.get() else terraceStep

    val effectiveTerraceMaskThreshold: Double
        get() = if (isSingleplayer()) ModServerConfig.TERRACE_MASK_THRESHOLD.get() else terraceMaskThreshold
}