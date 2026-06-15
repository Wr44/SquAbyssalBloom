package fr.heta__h.squ_abyssal_bloom.config

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

data class ServerConfigData(
    val strictBarnacleSpawning: Boolean,
    val shallowDeepBoundary: Double,
    val deepAbyssalBoundary: Double,
    val shallowFloorY: Int,
    val deepFloorTarget: Int,
    val targetFloorY: Int,
    val shallowClearance: Int,
    val guyotClearance: Int,
    val warpAmp: Double,
    val warp2Amp: Double,
    val topoLargeAmp: Double,
    val topoAmp: Double,
    val topoMidAmp: Double,
    val wallAmp: Double,
    val detailAmp: Double,
    val microAmp: Double,
    val weirdnessAmp: Double,
    val faultThreshold: Double,
    val faultBlend: Double,
    val faultOffsetAmp: Double,
    val trenchThreshold: Double,
    val trenchDepthAmp: Double,
    val seamountThreshold: Double,
    val seamountAmp: Double,
    val terraceStep: Double,
    val terraceMaskThreshold: Double,
) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ServerConfigData> = StreamCodec.of(
            { buf, v ->
                buf.writeBoolean(v.strictBarnacleSpawning)
                buf.writeDouble(v.shallowDeepBoundary)
                buf.writeDouble(v.deepAbyssalBoundary)
                buf.writeVarInt(v.shallowFloorY)
                buf.writeVarInt(v.deepFloorTarget)
                buf.writeVarInt(v.targetFloorY)
                buf.writeVarInt(v.shallowClearance)
                buf.writeVarInt(v.guyotClearance)
                buf.writeDouble(v.warpAmp)
                buf.writeDouble(v.warp2Amp)
                buf.writeDouble(v.topoLargeAmp)
                buf.writeDouble(v.topoAmp)
                buf.writeDouble(v.topoMidAmp)
                buf.writeDouble(v.wallAmp)
                buf.writeDouble(v.detailAmp)
                buf.writeDouble(v.microAmp)
                buf.writeDouble(v.weirdnessAmp)
                buf.writeDouble(v.faultThreshold)
                buf.writeDouble(v.faultBlend)
                buf.writeDouble(v.faultOffsetAmp)
                buf.writeDouble(v.trenchThreshold)
                buf.writeDouble(v.trenchDepthAmp)
                buf.writeDouble(v.seamountThreshold)
                buf.writeDouble(v.seamountAmp)
                buf.writeDouble(v.terraceStep)
                buf.writeDouble(v.terraceMaskThreshold)
            },
            { buf ->
                ServerConfigData(
                    strictBarnacleSpawning = buf.readBoolean(),
                    shallowDeepBoundary = buf.readDouble(),
                    deepAbyssalBoundary = buf.readDouble(),
                    shallowFloorY = buf.readVarInt(),
                    deepFloorTarget = buf.readVarInt(),
                    targetFloorY = buf.readVarInt(),
                    shallowClearance = buf.readVarInt(),
                    guyotClearance = buf.readVarInt(),
                    warpAmp = buf.readDouble(),
                    warp2Amp = buf.readDouble(),
                    topoLargeAmp = buf.readDouble(),
                    topoAmp = buf.readDouble(),
                    topoMidAmp = buf.readDouble(),
                    wallAmp = buf.readDouble(),
                    detailAmp = buf.readDouble(),
                    microAmp = buf.readDouble(),
                    weirdnessAmp = buf.readDouble(),
                    faultThreshold = buf.readDouble(),
                    faultBlend = buf.readDouble(),
                    faultOffsetAmp = buf.readDouble(),
                    trenchThreshold = buf.readDouble(),
                    trenchDepthAmp = buf.readDouble(),
                    seamountThreshold = buf.readDouble(),
                    seamountAmp = buf.readDouble(),
                    terraceStep = buf.readDouble(),
                    terraceMaskThreshold = buf.readDouble(),
                )
            }
        )

        fun fromSpec(): ServerConfigData = ServerConfigData(
            strictBarnacleSpawning = ModServerConfig.STRICT_BARNACLE_SPAWNING.get(),
            shallowDeepBoundary = ModServerConfig.SHALLOW_DEEP_BOUNDARY.get(),
            deepAbyssalBoundary = ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get(),
            shallowFloorY = ModServerConfig.SHALLOW_FLOOR_Y.get(),
            deepFloorTarget = ModServerConfig.DEEP_FLOOR_TARGET.get(),
            targetFloorY = ModServerConfig.TARGET_FLOOR_Y.get(),
            shallowClearance = ModServerConfig.SHALLOW_CLEARANCE.get(),
            guyotClearance = ModServerConfig.GUYOT_CLEARANCE.get(),
            warpAmp = ModServerConfig.WARP_AMP.get(),
            warp2Amp = ModServerConfig.WARP2_AMP.get(),
            topoLargeAmp = ModServerConfig.TOPO_LARGE_AMP.get(),
            topoAmp = ModServerConfig.TOPO_AMP.get(),
            topoMidAmp = ModServerConfig.TOPO_MID_AMP.get(),
            wallAmp = ModServerConfig.WALL_AMP.get(),
            detailAmp = ModServerConfig.DETAIL_AMP.get(),
            microAmp = ModServerConfig.MICRO_AMP.get(),
            weirdnessAmp = ModServerConfig.WEIRDNESS_AMP.get(),
            faultThreshold = ModServerConfig.FAULT_THRESHOLD.get(),
            faultBlend = ModServerConfig.FAULT_BLEND.get(),
            faultOffsetAmp = ModServerConfig.FAULT_OFFSET_AMP.get(),
            trenchThreshold = ModServerConfig.TRENCH_THRESHOLD.get(),
            trenchDepthAmp = ModServerConfig.TRENCH_DEPTH_AMP.get(),
            seamountThreshold = ModServerConfig.SEAMOUNT_THRESHOLD.get(),
            seamountAmp = ModServerConfig.SEAMOUNT_AMP.get(),
            terraceStep = ModServerConfig.TERRACE_STEP.get(),
            terraceMaskThreshold = ModServerConfig.TERRACE_MASK_THRESHOLD.get(),
        )
    }

    fun applyToSpec() {
        ModServerConfig.STRICT_BARNACLE_SPAWNING.set(strictBarnacleSpawning)
        ModServerConfig.SHALLOW_DEEP_BOUNDARY.set(shallowDeepBoundary)
        ModServerConfig.DEEP_ABYSSAL_BOUNDARY.set(deepAbyssalBoundary)
        ModServerConfig.SHALLOW_FLOOR_Y.set(shallowFloorY)
        ModServerConfig.DEEP_FLOOR_TARGET.set(deepFloorTarget)
        ModServerConfig.TARGET_FLOOR_Y.set(targetFloorY)
        ModServerConfig.SHALLOW_CLEARANCE.set(shallowClearance)
        ModServerConfig.GUYOT_CLEARANCE.set(guyotClearance)
        ModServerConfig.WARP_AMP.set(warpAmp)
        ModServerConfig.WARP2_AMP.set(warp2Amp)
        ModServerConfig.TOPO_LARGE_AMP.set(topoLargeAmp)
        ModServerConfig.TOPO_AMP.set(topoAmp)
        ModServerConfig.TOPO_MID_AMP.set(topoMidAmp)
        ModServerConfig.WALL_AMP.set(wallAmp)
        ModServerConfig.DETAIL_AMP.set(detailAmp)
        ModServerConfig.MICRO_AMP.set(microAmp)
        ModServerConfig.WEIRDNESS_AMP.set(weirdnessAmp)
        ModServerConfig.FAULT_THRESHOLD.set(faultThreshold)
        ModServerConfig.FAULT_BLEND.set(faultBlend)
        ModServerConfig.FAULT_OFFSET_AMP.set(faultOffsetAmp)
        ModServerConfig.TRENCH_THRESHOLD.set(trenchThreshold)
        ModServerConfig.TRENCH_DEPTH_AMP.set(trenchDepthAmp)
        ModServerConfig.SEAMOUNT_THRESHOLD.set(seamountThreshold)
        ModServerConfig.SEAMOUNT_AMP.set(seamountAmp)
        ModServerConfig.TERRACE_STEP.set(terraceStep)
        ModServerConfig.TERRACE_MASK_THRESHOLD.set(terraceMaskThreshold)
        ModServerConfig.SPEC.save()
    }
}