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

    val SPEC: ModConfigSpec = SPEC_BUILDER.build()
}