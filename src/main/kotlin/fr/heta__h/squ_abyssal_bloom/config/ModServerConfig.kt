package fr.heta__h.squ_abyssal_bloom.config

import net.neoforged.neoforge.common.ModConfigSpec

object ModServerConfig {
    private val SPEC_BUILDER = ModConfigSpec.Builder()

    val STRICT_BARNACLE_SPAWNING: ModConfigSpec.BooleanValue = SPEC_BUILDER
        .comment("If true, mobs with \"barnacle\" in their name will be unable to spawn")
        .define("strictBarnacleSpawning", true)

    val SPEC: ModConfigSpec = SPEC_BUILDER.build()
}