package fr.heta__h.squ_abyssal_bloom.config

import net.neoforged.neoforge.common.ModConfigSpec

object ModServerConfig {
    private val SPEC_BUILDER = ModConfigSpec.Builder()

    val STRICT_BARNACLE_SPAWNING: ModConfigSpec.BooleanValue = SPEC_BUILDER
        .comment("If true, mobs with \"barnacle\" in their name will be unable to spawn")
        .define("strictBarnacleSpawning", true)

    val SHALLOW_DEEP_BOUNDARY: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Continentalness boundary between shallow and deep ocean. Vanilla: -0.455")
        .defineInRange("shallowDeepBoundary", -0.45, -1.0, -0.19)

    val DEEP_ABYSSAL_BOUNDARY: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Continentalness boundary between deep ocean and abyssal zone.")
        .defineInRange("deepAbyssalBoundary", -0.70, -1.05, -0.19)

    // Zones
    val SHALLOW_FLOOR_Y: ModConfigSpec.IntValue = SPEC_BUILDER
        .comment("Base Y target for shallow ocean floor.")
        .defineInRange("shallowFloorY", 32, -60, 60)

    val DEEP_FLOOR_TARGET: ModConfigSpec.IntValue = SPEC_BUILDER
        .comment("Base Y target for deep ocean floor.")
        .defineInRange("deepFloorTarget", 11, -60, 60)

    val TARGET_FLOOR_Y: ModConfigSpec.IntValue = SPEC_BUILDER
        .comment("Base Y target for abyssal floor.")
        .defineInRange("targetFloorY", -35, -64, 0)

    val SHALLOW_CLEARANCE: ModConfigSpec.IntValue = SPEC_BUILDER
        .comment("Minimum blocks of water above shallow floor (below sea level).")
        .defineInRange("shallowClearance", 8, 1, 30)

    // Topography
    val WARP_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of primary domain warp (blocks).")
        .defineInRange("warpAmp", 60.0, 0.0, 200.0)

    val WARP2_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of secondary domain warp (blocks).")
        .defineInRange("warp2Amp", 26.0, 0.0, 100.0)

    val TOPO_LARGE_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of large-scale abyssal topography (blocks).")
        .defineInRange("topoLargeAmp", 30.0, 0.0, 150.0)

    val TOPO_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of mid-scale abyssal topography (blocks).")
        .defineInRange("topoAmp", 24.0, 0.0, 100.0)

    val TOPO_MID_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of fine-scale abyssal topography (blocks).")
        .defineInRange("topoMidAmp", 11.0, 0.0, 60.0)

    val WALL_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of wall/ridge variation in abyssal zone (blocks).")
        .defineInRange("wallAmp", 14.0, 0.0, 80.0)

    val DETAIL_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of detail noise in abyssal zone (blocks).")
        .defineInRange("detailAmp", 7.0, 0.0, 40.0)

    val MICRO_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Amplitude of micro noise in abyssal zone (blocks).")
        .defineInRange("microAmp", 4.0, 0.0, 20.0)

    val WEIRDNESS_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Influence of weirdness on abyssal floor height (blocks).")
        .defineInRange("weirdnessAmp", 11.0, 0.0, 50.0)

    // Fault
    val FAULT_THRESHOLD: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Noise threshold controlling fault line density.")
        .defineInRange("faultThreshold", 0.012, 0.0, 0.2)

    val FAULT_BLEND: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Blend width for fault line edges.")
        .defineInRange("faultBlend", 0.045, 0.001, 0.2)

    val FAULT_OFFSET_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Maximum vertical offset caused by fault lines (blocks). Note: downward faults are constrained by the abyssal hard limit (~15 blocks above bedrock).")
        .defineInRange("faultOffsetAmp", 10.0, 0.0, 22.0)

    // Trench
    val TRENCH_THRESHOLD: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Noise threshold above which a trench forms (0–1).")
        .defineInRange("trenchThreshold", 0.88, 0.5, 0.99)

    val TRENCH_DEPTH_AMP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Maximum depth of trenches (blocks).")
        .defineInRange("trenchDepthAmp", 10.0, 0.0, 22.0)

    // Terrace
    val TERRACE_STEP: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Vertical height of each terrace step (blocks).")
        .defineInRange("terraceStep", 13.0, 2.0, 40.0)

    val TERRACE_MASK_THRESHOLD: ModConfigSpec.DoubleValue = SPEC_BUILDER
        .comment("Noise threshold above which terraces appear (0.2–1.2).")
        .defineInRange("terraceMaskThreshold", 0.80, 0.3, 0.99)

    val SPEC: ModConfigSpec = SPEC_BUILDER.build()
}