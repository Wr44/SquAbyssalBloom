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

    val STRICT_BARNACLE_SPAWNING = bool("strictBarnacleSpawning", true, "If true, mobs from other mods with \"barnacle\" in their name will be unable to spawn.")
    val BARNACLE_SPAWN_MAX_Y = int("barnacleSpawnMaxY", 24, -64, 63, "Highest Y level at which Barnacles may spawn naturally.")
    val BARNACLE_DETECTION_RANGE = double("barnacleDetectionRange", 55.0, 4.0, 96.0, "Maximum range at which a Barnacle can acquire prey.")
    val BARNACLE_MOVEMENT_SPEED = double("barnacleMovementSpeed", 2.0, 0.1, 4.0, "Maximum rush speed used while pursuing prey.")
    val BARNACLE_OBSTACLE_AVOIDANCE_SPEED = double("barnacleObstacleAvoidanceSpeed", 1.0, 0.1, 2.0, "Maximum speed while following a path around obstacles.")
    val BARNACLE_FLEE_SPEED = double("barnacleFleeSpeed", 3.0, 0.1, 5.0, "Maximum speed used by a critically injured Barnacle while fleeing.")
    val BARNACLE_REGEN_COOLDOWN = int("barnacleRegenCooldown", 100, 20, 1200, "Ticks out of combat between Barnacle regeneration pulses.")
    val BARNACLE_DIRECT_CORRIDOR_DISTANCE = double("barnacleDirectCorridorDistance", 6.0, 2.0, 16.0, "Distance at which a Barnacle may leave its path when the direct aquatic corridor is clear.")
    val BARNACLE_CAPTURE_DISTANCE = double("barnacleCaptureDistance", 5.5, 1.0, 12.0, "Maximum distance at which the Barnacle capture sequence can begin.")
    val BARNACLE_HOLD_DISTANCE = double("barnacleHoldDistance", 1.5, 0.25, 6.0, "Distance at which captured prey is held in front of the Barnacle.")

    val BRINE_NATURAL_SPAWNING = bool("brineNaturalSpawning", true, "Allows Brines to spawn naturally in ocean biomes.")
    val BRINE_SPAWN_MAX_Y = int("brineSpawnMaxY", 24, -64, 63, "Highest Y level at which Brines may spawn naturally.")
    val BRINE_DETECTION_RANGE = double("brineDetectionRange", 24.0, 4.0, 64.0, "Maximum range at which a Brine can detect players.")
    val BRINE_FOLLOW_SPEED = double("brineFollowSpeed", 0.20, 0.02, 1.0, "Horizontal speed used by a Brine while following a player.")
    val BRINE_ATTACK_SPEED = double("brineAttackSpeed", 0.10, 0.01, 0.5, "Horizontal speed used by a Brine while positioning for an attack.")
    val BRINE_ATTACK_ENTER_RADIUS = double("brineAttackEnterRadius", 2.0, 0.5, 8.0, "Horizontal distance at which a Brine enters an attack behavior.")
    val BRINE_ATTACK_EXIT_RADIUS = double("brineAttackExitRadius", 5.0, 1.0, 16.0, "Horizontal distance beyond which a Brine leaves an attack behavior.")
    val BRINE_COLUMN_ATTACK_COOLDOWN = int("brineColumnAttackCooldown", 20, 1, 200, "Ticks between bubble projectiles during the column attack.")
    val BRINE_DIRECT_ATTACK_COOLDOWN = int("brineDirectAttackCooldown", 30, 1, 200, "Ticks between direct bubble projectiles.")

    val RED_SLOBBERER_REEF_MATURITY_TICKS = int("redSlobbererReefMaturityTicks", 6000, 200, 72000, "Ticks a Red Slobberer group must remain nearby before its local reef matures.")
    val RED_SLOBBERER_MAX_REEF_FISH = int("redSlobbererMaximumReefFish", 12, 0, 48, "Maximum local fish population maintained by a mature Red Slobberer reef.")

    val FISH_SCHOOL_DEBUG = bool("fishSchoolDebug", false, "Displays collective-state particles and writes periodic fish-school summaries to the server log.")
    val FISH_SCHOOL_NEIGHBOR_COUNT = int("fishSchoolNeighborCount", 7, 1, 12, "Maximum number of nearest compatible fish influencing one fish.")
    val FISH_SCHOOL_NEIGHBOR_SEARCH_RADIUS = double("fishSchoolNeighborSearchRadius", 7.0, 2.0, 16.0, "Radius used only to discover candidate topological neighbors.")
    val FISH_SCHOOL_AGGREGATION_RADIUS = double("fishSchoolAggregationRadius", 32.0, 4.0, 64.0, "Maximum distance for a weak temporary link that lets nearby schools merge.")
    val FISH_SCHOOL_CROSS_SPECIES_AFFINITY = double("fishSchoolCrossSpeciesAffinity", 0.78, 0.1, 1.0, "Relative social influence of a fish from another compatible species.")
    val FISH_SCHOOL_ACTIVATION_THRESHOLD = int("fishSchoolActivationThreshold", 2, 2, 31, "Nearby compatible fish required before collective movement may activate.")
    val FISH_SCHOOL_DEACTIVATION_THRESHOLD = int("fishSchoolDeactivationThreshold", 1, 0, 30, "Collective movement deactivates at or below this number of nearby compatible fish.")
    val FISH_SCHOOL_ACTIVATION_DELAY = int("fishSchoolActivationDelay", 40, 0, 600, "Ticks for which activation density must remain stable before collective movement starts.")
    val FISH_SCHOOL_DEACTIVATION_DELAY = int("fishSchoolDeactivationDelay", 100, 0, 1200, "Ticks for which local density must remain too low before vanilla movement resumes.")
    val FISH_SCHOOL_NEIGHBOR_REFRESH_INTERVAL = int("fishSchoolNeighborRefreshInterval", 6, 2, 40, "Ticks between staggered local neighbor and threat searches.")
    val FISH_SCHOOL_SEPARATION_WEIGHT = double("fishSchoolSeparationWeight", 1.8, 0.0, 5.0, "Strength with which fish avoid crowding and anticipated collisions.")
    val FISH_SCHOOL_ALIGNMENT_WEIGHT = double("fishSchoolAlignmentWeight", 0.7, 0.0, 5.0, "Strength with which fish progressively align with local neighbors.")
    val FISH_SCHOOL_COHESION_WEIGHT = double("fishSchoolCohesionWeight", 0.55, 0.0, 5.0, "Strength attracting fish toward the local center of their nearest neighbors.")
    val FISH_SCHOOL_OBSTACLE_AVOIDANCE_WEIGHT = double("fishSchoolObstacleAvoidanceWeight", 2.25, 0.0, 6.0, "Strength of progressive avoidance for blocks and large entities.")
    val FISH_SCHOOL_THREAT_AVOIDANCE_WEIGHT = double("fishSchoolThreatAvoidanceWeight", 2.8, 0.0, 8.0, "Strength of the local escape direction carried by a threat signal.")
    val FISH_SCHOOL_THREAT_DETECTION_RADIUS = double("fishSchoolThreatDetectionRadius", 12.0, 2.0, 32.0, "Radius in which an individual fish can directly detect a threat.")
    val FISH_SCHOOL_THREAT_PROPAGATION_SPEED = double("fishSchoolThreatPropagationSpeed", 0.78, 0.0, 1.0, "Fraction of a local threat signal transmitted from one fish to its neighbors.")
    val FISH_SCHOOL_THREAT_SIGNAL_DECAY = double("fishSchoolThreatSignalDecay", 0.012, 0.001, 0.2, "Threat-signal intensity lost per tick after direct danger disappears.")
    val FISH_SCHOOL_MAXIMUM_SPEED = double("fishSchoolMaximumSpeed", 0.17, 0.03, 0.4, "Maximum configured speed used by collective movement.")
    val FISH_SCHOOL_PANIC_SPEED = double("fishSchoolPanicSpeed", 0.24, 0.03, 0.5, "Maximum speed approached while a local threat signal is strong.")
    val FISH_SCHOOL_MAXIMUM_TURN_RATE = double("fishSchoolMaximumTurnRate", 7.0, 1.0, 30.0, "Maximum horizontal direction change in degrees per tick.")
    val FISH_SCHOOL_VERTICAL_MOVEMENT_WEIGHT = double("fishSchoolVerticalMovementWeight", 0.3, 0.0, 1.0, "Relative strength of non-obstacle vertical steering.")
    val FISH_SCHOOL_VERTICAL_DRIFT_SPEED = double("fishSchoolVerticalDriftSpeed", 0.012, 0.0, 0.05, "Maximum vertical speed of the slow shared depth current that lets whole schools rise and sink over time.")

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
