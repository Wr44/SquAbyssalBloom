package fr.heta__h.squ_abyssal_bloom.config.server

import fr.heta__h.squ_abyssal_bloom.config.server.types.BoolOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.ConfigOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.DoubleOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.IntOption
import net.neoforged.neoforge.common.ModConfigSpec

object ModServerConfig {
    private val builder = ModConfigSpec.Builder()
    private val registered = mutableListOf<ConfigOption<*>>()

    private fun bool(key: String, default: Boolean, comment: String): BoolOption {
        val opt = BoolOption(key, default, comment)
        opt.spec = opt.build(builder)
        registered.add(opt)
        return opt
    }

    private fun double(key: String, default: Double, min: Double, max: Double, comment: String): DoubleOption {
        val opt = DoubleOption(key, default, min, max, comment)
        opt.spec = opt.build(builder)
        registered.add(opt)
        return opt
    }

    private fun int(key: String, default: Int, min: Int, max: Int, comment: String): IntOption {
        val opt = IntOption(key, default, min, max, comment)
        opt.spec = opt.build(builder)
        registered.add(opt)
        return opt
    }

    val STRICT_BARNACLE_SPAWNING = bool("strictBarnacleSpawning", true, "If true, mobs with \"barnacle\" in their name will be unable to spawn")
    val SHALLOW_DEEP_BOUNDARY = double("shallowDeepBoundary", -0.45, -1.0, -0.19, "Continentalness boundary between shallow and deep ocean. Vanilla: -0.455")
    val DEEP_ABYSSAL_BOUNDARY = double("deepAbyssalBoundary", -0.70, -1.05, -0.19, "Continentalness boundary between deep ocean and abyssal zone.")
    val SHALLOW_FLOOR_Y = int("shallowFloorY", 36, -60, 61, "Base Y target for shallow ocean floor.")
    val DEEP_FLOOR_TARGET = int("deepFloorTarget", 18, -60, 60, "Base Y target for deep ocean floor.")
    val TARGET_FLOOR_Y = int("targetFloorY", -40, -64, 20, "Base Y target for abyssal floor.")
    val SHALLOW_CLEARANCE = int("shallowClearance", 8, 1, 30, "Minimum blocks of water above shallow floor (below sea level).")
    val WARP_AMP = double("warpAmp", 60.0, 0.0, 200.0, "Amplitude of primary domain warp (blocks).")
    val WARP2_AMP = double("warp2Amp", 26.0, 0.0, 100.0, "Amplitude of secondary domain warp (blocks).")
    val TOPO_LARGE_AMP = double("topoLargeAmp", 30.0, 0.0, 150.0, "Amplitude of large-scale abyssal topography (blocks).")
    val TOPO_AMP = double("topoAmp", 24.0, 0.0, 100.0, "Amplitude of mid-scale abyssal topography (blocks).")
    val TOPO_MID_AMP = double("topoMidAmp", 11.0, 0.0, 60.0, "Amplitude of fine-scale abyssal topography (blocks).")
    val WALL_AMP = double("wallAmp", 14.0, 0.0, 80.0, "Amplitude of wall/ridge variation in abyssal zone (blocks).")
    val DETAIL_AMP = double("detailAmp", 7.0, 0.0, 40.0, "Amplitude of detail noise in abyssal zone (blocks).")
    val MICRO_AMP = double("microAmp", 4.0, 0.0, 20.0, "Amplitude of micro noise in abyssal zone (blocks).")
    val WEIRDNESS_AMP = double("weirdnessAmp", 11.0, 0.0, 50.0, "Influence of weirdness on abyssal floor height (blocks).")
    val FAULT_THRESHOLD = double("faultThreshold", 0.012, 0.0, 0.2, "Noise threshold controlling fault line density.")
    val FAULT_BLEND = double("faultBlend", 0.045, 0.001, 0.2, "Blend width for fault line edges.")
    val FAULT_OFFSET_AMP = double("faultOffsetAmp", 10.0, 0.0, 22.0, "Maximum vertical offset caused by fault lines (blocks).")
    val TRENCH_THRESHOLD = double("trenchThreshold", 0.88, 0.5, 0.99, "Noise threshold above which a trench forms (0-1).")
    val TRENCH_DEPTH_AMP = double("trenchDepthAmp", 10.0, 0.0, 22.0, "Maximum depth of trenches (blocks).")
    val TERRACE_STEP = double("terraceStep", 13.0, 2.0, 40.0, "Vertical height of each terrace step (blocks).")
    val TERRACE_MASK_THRESHOLD = double("terraceMaskThreshold", 0.80, 0.3, 0.99, "Noise threshold above which terraces appear.")
    val OCEAN_TERRITORY_EXTRA_ZOOMS = int("oceanTerritoryExtraZooms", 6, 0, 10, "Size of ocean territories per mod, in number of extra zooms. Higher = larger territories.")
    val OCEAN_TERRITORY_DEFAULT_WEIGHT = int("oceanTerritoryDefaultWeight", 10, 1, 1000, "Default weight of an unknown namespace in territorial competition.")
    val OCEAN_TERRITORY_OWN_WEIGHT = int("oceanTerritoryOwnWeight", 20, 1, 1000, "Weight of Abyssal Bloom in territorial competition, relative to the default weight.")
    val OCEAN_TERRITORY_INCLUDE_VANILLA = bool("oceanTerritoryIncludeVanilla", false, "If disabled, vanilla ocean biomes act as a safety net rather than competing with mods for territory.")

    val SPEC: ModConfigSpec = builder.build()

    val options: List<ConfigOption<*>> = registered.toList().also { registered.clear() }
}