package fr.heta__h.squ_abyssal_bloom.config.server

import fr.heta__h.squ_abyssal_bloom.config.server.types.BoolOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.ConfigOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.DoubleOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.IntOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.NoiseThresholdCalibration
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

    private fun stringList(key: String, default: List<String>, comment: String): StringListOption {
        val opt = StringListOption(key, default, comment)
        opt.spec = opt.build(builder)
        registered.add(opt)
        return opt
    }

    val BARNACLE_SPAWN_ENABLED = bool("barnacleSpawnEnabled", true, "Allows Barnacles to spawn naturally.")
    val STRICT_BARNACLE_SPAWNING = bool("strictBarnacleSpawning", true, "If true, mobs from other mods with \"barnacle\" in their name will be unable to spawn.")
    val BARNACLE_SPAWN_MAX_Y = int("barnacleSpawnMaxY", -10, -64, 63, "Highest Y level at which Barnacles may spawn naturally.")
    val BARNACLE_DETECTION_RANGE = double("barnacleDetectionRange", 55.0, 4.0, 96.0, "Maximum range at which a Barnacle can acquire prey.")
    val BARNACLE_MOVEMENT_SPEED = double("barnacleMovementSpeed", 2.0, 0.1, 4.0, "Maximum rush speed used while pursuing prey.")
    val BARNACLE_OBSTACLE_AVOIDANCE_SPEED = double("barnacleObstacleAvoidanceSpeed", 1.0, 0.1, 2.0, "Maximum speed while following a path around obstacles.")
    val BARNACLE_FLEE_SPEED = double("barnacleFleeSpeed", 3.0, 0.1, 5.0, "Maximum speed used by a critically injured Barnacle while fleeing.")
    val BARNACLE_REGEN_COOLDOWN = int("barnacleRegenCooldown", 100, 20, 1200, "Ticks out of combat between Barnacle regeneration pulses.")
    val BARNACLE_DIRECT_CORRIDOR_DISTANCE = double("barnacleDirectCorridorDistance", 6.0, 2.0, 16.0, "Distance at which a Barnacle may leave its path when the direct aquatic corridor is clear.")
    val BARNACLE_CAPTURE_DISTANCE = double("barnacleCaptureDistance", 5.5, 1.0, 12.0, "Maximum distance at which the Barnacle capture sequence can begin.")
    val BARNACLE_HOLD_DISTANCE = double("barnacleHoldDistance", 1.5, 0.25, 6.0, "Distance at which captured prey is held in front of the Barnacle.")

    val BRINE_NATURAL_SPAWNING = bool("brineNaturalSpawning", true, "Allows Brines to spawn naturally in deep ocean biomes.")
    val BRINE_DETECTION_RANGE = double("brineDetectionRange", 24.0, 4.0, 64.0, "Maximum range at which a Brine can detect players.")
    val BRINE_FOLLOW_SPEED = double("brineFollowSpeed", 0.20, 0.02, 1.0, "Horizontal speed used by a Brine while following a player.")
    val BRINE_ATTACK_SPEED = double("brineAttackSpeed", 0.10, 0.01, 0.5, "Horizontal speed used by a Brine while positioning for an attack.")
    val BRINE_ATTACK_ENTER_RADIUS = double("brineAttackEnterRadius", 2.0, 0.5, 8.0, "Horizontal distance at which a Brine enters an attack behavior.")
    val BRINE_ATTACK_EXIT_RADIUS = double("brineAttackExitRadius", 5.0, 1.0, 16.0, "Horizontal distance beyond which a Brine leaves an attack behavior.")
    val BRINE_COLUMN_ATTACK_COOLDOWN = int("brineColumnAttackCooldown", 20, 1, 200, "Ticks between bubble projectiles during the column attack.")
    val BRINE_DIRECT_ATTACK_COOLDOWN = int("brineDirectAttackCooldown", 30, 1, 200, "Ticks between direct bubble projectiles.")

    val RED_SLOBBERER_SPAWN_ENABLED = bool("redSlobbererSpawnEnabled", true, "Allows Red Slobberers to spawn naturally.")
    val RED_SLOBBERER_REEF_DEBUG = bool("redSlobbererReefDebug", false, "Displays reef, deposit and fish-refuge diagnostics and writes periodic summaries to the server log.")
    val RED_SLOBBERER_REEF_MATURITY_TICKS = int("redSlobbererReefMaturityTicks", 6000, 200, 72000, "Ticks a Red Slobberer group must remain nearby before its local reef matures.")
    val RED_SLOBBERER_MAX_REEF_FISH = int("redSlobbererMaximumReefFish", 12, 0, 48, "Maximum local fish population maintained by a mature Red Slobberer reef.")
    val RED_SLOBBERER_DEPOSIT_MIN_INTERVAL_TICKS = int("redSlobbererDepositMinimumIntervalTicks", 1200, 20, 24000, "Minimum delay in ticks between two calcareous deposit checks for one reef.")
    val RED_SLOBBERER_DEPOSIT_MAX_INTERVAL_TICKS = int("redSlobbererDepositMaximumIntervalTicks", 2400, 20, 48000, "Maximum delay in ticks between two calcareous deposit checks for one reef.")
    val RED_SLOBBERER_MAX_CALCAREOUS_DEPOSITS = int("redSlobbererMaximumCalcareousDeposits", 12, 0, 32, "Hard maximum number of tracked calcareous deposits around one reef.")
    val RED_SLOBBERER_MINIMUM_REEF_STABILITY = double("redSlobbererMinimumReefStability", 0.35, 0.0, 1.0, "Minimum collective stability required for reef maturity and calcareous activity.")
    val RED_SLOBBERER_DEPOSIT_GROWTH_CHANCE = double("redSlobbererDepositGrowthChance", 0.25, 0.0, 1.0, "Base chance for one existing calcareous deposit to gain an age during a deposit check.")
    val RED_SLOBBERER_NEW_DEPOSIT_CHANCE = double("redSlobbererNewDepositChance", 0.20, 0.0, 1.0, "Base chance to attempt placing a new calcareous deposit during a deposit check.")
    val RED_SLOBBERER_DECORATION_MIN_INTERVAL_TICKS = int("redSlobbererDecorationMinimumIntervalTicks", 1200, 20, 24000, "Minimum delay in ticks between two reef decoration checks.")
    val RED_SLOBBERER_DECORATION_MAX_INTERVAL_TICKS = int("redSlobbererDecorationMaximumIntervalTicks", 2400, 20, 48000, "Maximum delay in ticks between two reef decoration checks.")
    val RED_SLOBBERER_MAX_DECORATIONS = int("redSlobbererMaximumDecorations", 8, 0, 32, "Hard maximum number of tracked decorations around one reef.")
    val RED_SLOBBERER_DECORATION_CHANCE = double("redSlobbererDecorationChance", 0.5, 0.0, 1.0, "Base chance to attempt placing a decoration, scaled by the reef's collective activity and stability.")
    val RED_SLOBBERER_FISH_REFUGE_RADIUS = double("redSlobbererFishRefugeRadius", 24.0, 8.0, 48.0, "Maximum distance at which fish from the same reef may choose a Red Slobberer as refuge.")
    val RED_SLOBBERER_FISH_REFUGE_CAPACITY = int("redSlobbererFishRefugeCapacity", 12, 0, 48, "Maximum number of fish that one Red Slobberer may shelter at once.")
    val RED_SLOBBERER_FISH_REFUGE_MINIMUM_STAY_TICKS = int("redSlobbererFishRefugeMinimumStayTicks", 60, 0, 1200, "Minimum time a sheltered fish remains hidden after entering a Red Slobberer refuge.")
    val RED_SLOBBERER_FISH_REFUGE_MAXIMUM_STAY_TICKS = int("redSlobbererFishRefugeMaximumStayTicks", 600, 20, 72000, "Maximum time a fish may remain hidden in a Red Slobberer refuge, even while danger persists.")
    val RED_SLOBBERER_FISH_REFUGE_QUIET_RELEASE_TICKS = int("redSlobbererFishRefugeQuietReleaseTicks", 60, 0, 1200, "Time without a nearby fish threat before sheltered fish may emerge.")
    val RED_SLOBBERER_FISH_REFUGE_UNSAFE_COOLDOWN_TICKS = int("redSlobbererFishRefugeUnsafeCooldownTicks", 200, 0, 2400, "Time during which a damaged Red Slobberer cannot shelter fish.")
    val RED_SLOBBERER_REEF_RESIDENCE_RADIUS = double("redSlobbererReefResidenceRadius", 10.0, 4.0, 18.0, "Horizontal radius around its reef anchor in which a Red Slobberer naturally grazes and wanders.")

    val MACKEREL_SPAWN_ENABLED = bool("mackerelSpawnEnabled", true, "Allows Mackerel to spawn naturally.")
    val MACKEREL_MAX_SPAWN_TEMPERATURE = double("mackerelMaxSpawnTemperature", 0.2, -1.0, 1.0, "Mackerel cannot spawn in oceans whose climate temperature parameter is at or above this value (0.2 blocks lukewarm and warm oceans, 0.55 blocks warm oceans only).")

    val FISH_SCHOOL_ENABLED = bool("fishSchoolEnabled", true, "Enables collective schooling movement. When disabled, all fish keep their full vanilla AI.")
    val FISH_SCHOOL_DEBUG = bool("fishSchoolDebug", false, "Displays collective-state particles and writes periodic fish-school summaries to the server log.")
    val FISH_SCHOOL_NEIGHBOR_COUNT = int("fishSchoolNeighborCount", 7, 1, 12, "Maximum number of nearest compatible fish influencing one fish.")
    val FISH_SCHOOL_NEIGHBOR_SEARCH_RADIUS = double("fishSchoolNeighborSearchRadius", 7.0, 2.0, 16.0, "Radius used only to discover candidate topological neighbors.")
    val FISH_SCHOOL_AGGREGATION_RADIUS = double("fishSchoolAggregationRadius", 32.0, 4.0, 64.0, "Maximum distance for a weak temporary link that lets nearby schools merge.")
    val FISH_SCHOOL_CROSS_SPECIES_AFFINITY = double("fishSchoolCrossSpeciesAffinity", 0.78, 0.1, 1.0, "Relative social influence of a fish from another compatible species.")
    val FISH_SCHOOL_ACTIVATION_THRESHOLD = int("fishSchoolActivationThreshold", 2, 2, 31, "Nearby compatible fish required before collective movement may activate.")
    val FISH_SCHOOL_DEACTIVATION_THRESHOLD = int("fishSchoolDeactivationThreshold", 1, 0, 30, "Collective movement deactivates at or below this number of nearby compatible fish.")
    val FISH_SCHOOL_ACTIVATION_DELAY = int("fishSchoolActivationDelay", 40, 0, 600, "Ticks for which activation density must remain stable before collective movement starts.")
    val FISH_SCHOOL_DEACTIVATION_DELAY = int("fishSchoolDeactivationDelay", 100, 0, 1200, "Ticks for which local density must remain too low before vanilla movement resumes.")
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
    val FISH_SCHOOL_MAXIMUM_TURN_RATE = double("fishSchoolMaximumTurnRate", 7.0, 1.0, 30.0, "Maximum horizontal heading change in degrees per tick; vertical turns use acceleration smoothing.")
    val FISH_SCHOOL_VERTICAL_MOVEMENT_WEIGHT = double("fishSchoolVerticalMovementWeight", 0.3, 0.0, 1.0, "Relative strength of non-obstacle vertical steering.")
    val FISH_SCHOOL_VERTICAL_DRIFT_SPEED = double("fishSchoolVerticalDriftSpeed", 0.012, 0.0, 0.05, "Maximum vertical speed of the slow shared depth current that lets whole schools rise and sink over time.")
    val FISH_SCHOOL_FRIENDLY_ENTITIES = stringList(
        "fishSchoolFriendlyEntities",
        emptyList(),
        "Extra entities exempted from fish threat detection, on top of whatever is currently tagged fish_school_friendly."
    )
    val FISH_SCHOOL_NEIGHBOR_FOV_HALF_ANGLE = double("fishSchoolNeighborFovHalfAngle", 150.0, 30.0, 180.0, "Half-angle in degrees of the forward cone used to gate alignment and cohesion; separation stays omnidirectional. 180 disables the cone.")
    val FISH_SCHOOL_AGGREGATION_COHESION_SCALE = double("fishSchoolAggregationCohesionScale", 0.65, 0.0, 2.0, "Fraction of cohesion weight applied to the long-range pull that merges separate nearby schools.")
    val FISH_SCHOOL_COHESION_SPEED_RESPONSE_SCALE = double("fishSchoolCohesionSpeedResponseScale", 0.55, 0.0, 1.0, "Independent strength of the local crowding/spacing speed adjustment, decoupled from positional cohesion strength.")
    val FISH_SCHOOL_SEPARATION_RADIUS = double("fishSchoolSeparationRadius", 2.25, 0.5, 6.0, "Distance below which fish actively push apart to avoid anticipated collisions.")
    val FISH_SCHOOL_MAXIMUM_SEPARATION_FORCE = double("fishSchoolMaximumSeparationForce", 2.4, 0.1, 6.0, "Hard cap on the combined separation steering vector.")
    val FISH_SCHOOL_COHESION_FULL_STRENGTH_DISTANCE = double("fishSchoolCohesionFullStrengthDistance", 3.0, 0.5, 8.0, "Distance from the local flock center at which cohesion reaches full strength.")
    val FISH_SCHOOL_HERD_COMPRESSION = double("fishSchoolHerdCompression", 0.6, 0.0, 2.0, "Strength with which panicking fish additionally compress toward their local group center.")

    val BIOLUMINESCENCE_ENABLED = bool("bioluminescenceEnabled", true, "Enables server-authoritative bioluminescent wave events.")
    val BIOLUMINESCENCE_OPPORTUNITY_MEAN_NIGHT_TICKS = int("bioluminescenceOpportunityMeanNightTicks", 36000, 1, 2000000, "Mean number of eligible night ticks between two personal bioluminescence opportunities.")
    val BIOLUMINESCENCE_OPPORTUNITY_MIN_NIGHT_TICKS = int("bioluminescenceOpportunityMinimumNightTicks", 12000, 1, 2000000, "Minimum exponentially sampled delay, in eligible night ticks.")
    val BIOLUMINESCENCE_OPPORTUNITY_MAX_NIGHT_TICKS = int("bioluminescenceOpportunityMaximumNightTicks", 240000, 1, 2000000, "Maximum exponentially sampled delay, in eligible night ticks.")
    val BIOLUMINESCENCE_ACTIVE_CHANCE = double("bioluminescenceActiveChance", 0.6, 0.0, 1.0, "Chance that a wave is ACTIVE (always glowing) instead of INACTIVE (invisible until disturbed by nearby movement).")
    val BIOLUMINESCENCE_LARGE_CHANCE = double("bioluminescenceLargeChance", 2.0 / 3.0, 0.0, 1.0, "Chance that a normal wave is large. The small chance is derived from this value.")
    val BIOLUMINESCENCE_DURATION_MEAN_TICKS = int("bioluminescenceDurationMeanTicks", 4200, 1, 72000, "Mean duration of a normal bioluminescent wave, in ticks.")
    val BIOLUMINESCENCE_DURATION_STANDARD_DEVIATION_TICKS = int("bioluminescenceDurationStandardDeviationTicks", 1200, 0, 72000, "Standard deviation of normal bioluminescent wave durations, in ticks.")
    val BIOLUMINESCENCE_DURATION_MIN_TICKS = int("bioluminescenceDurationMinimumTicks", 1200, 1, 72000, "Minimum duration of a normal bioluminescent wave, in ticks.")
    val BIOLUMINESCENCE_DURATION_MAX_TICKS = int("bioluminescenceDurationMaximumTicks", 7200, 1, 72000, "Maximum duration of a normal bioluminescent wave, in ticks.")
    val BIOLUMINESCENCE_VISIBILITY_RADIUS = double("bioluminescenceVisibilityRadius", 128.0, 16.0, 512.0, "Server synchronization radius around an active wave.")
    val BIOLUMINESCENCE_OVERLAP_MARGIN = double("bioluminescenceOverlapMargin", 16.0, 0.0, 128.0, "Additional tolerance around a wave's real visible radius used to reuse nearby events without extending their rendered coverage.")
    val BIOLUMINESCENCE_PREPARATION_DELAY_TICKS = int("bioluminescencePreparationDelayTicks", 40, 0, 1200, "Server-controlled delay between wave creation and its visual timeline.")
    val BIOLUMINESCENCE_TOTAL_NIGHT_CHANCE = double("bioluminescenceTotalNightChance", 1.0 / 25.0, 0.0, 1.0, "Chance, rolled once at the beginning of each night and level, for a total bioluminescent night.")
    val BIOLUMINESCENCE_NIGHT_START_TICK = int("bioluminescenceNightStartTick", 13000, 0, 23999, "Day-time tick at which bioluminescence considers night to begin.")
    val BIOLUMINESCENCE_NIGHT_END_TICK = int("bioluminescenceNightEndTick", 23000, 0, 23999, "Day-time tick at which bioluminescence considers night to end.")
    val BIOLUMINESCENCE_WATER_SEARCH_RADIUS = int("bioluminescenceWaterSearchRadius", 12, 1, 32, "Horizontal radius used to confirm that a beach entry is next to compatible water.")
    val BIOLUMINESCENCE_MINIMUM_NEARBY_WATER_CELLS = int("bioluminescenceMinimumNearbyWaterCells", 12, 1, 81, "Minimum compatible water samples required around a detected beach entry.")

    val BIOLUMINESCENCE_BLOOM_ENABLED = bool("bioluminescenceBloomEnabled", true, "Enables plankton blooms inside bioluminescent waves.")
    val BIOLUMINESCENCE_BLOOM_HARVEST_RADIUS = double("bioluminescenceBloomHarvestRadius", 2.5, 0.5, 8.0, "Radius around a bloom's center within which it can be harvested.")
    val BIOLUMINESCENCE_BLOOM_ACTIVATION_RADIUS = double("bioluminescenceBloomActivationRadius", 8.0, 1.0, 32.0, "Radius around a dormant bloom scanned for nearby movement to activate it.")
    val BIOLUMINESCENCE_BLOOM_ACTIVATION_MIN_DISPLACEMENT = double("bioluminescenceBloomActivationMinDisplacement", 0.15, 0.01, 2.0, "Minimum distance an entity must travel between scans to activate a dormant bloom.")
    val BIOLUMINESCENCE_BLOOM_SMALL_MIN_COUNT = int("bioluminescenceBloomSmallMinCount", 1, 0, 8, "Minimum number of blooms placed in a small wave.")
    val BIOLUMINESCENCE_BLOOM_SMALL_MAX_COUNT = int("bioluminescenceBloomSmallMaxCount", 2, 0, 8, "Maximum number of blooms placed in a small wave.")
    val BIOLUMINESCENCE_BLOOM_LARGE_MIN_COUNT = int("bioluminescenceBloomLargeMinCount", 2, 0, 8, "Minimum number of blooms placed in a large wave.")
    val BIOLUMINESCENCE_BLOOM_LARGE_MAX_COUNT = int("bioluminescenceBloomLargeMaxCount", 4, 0, 8, "Maximum number of blooms placed in a large wave.")
    val BIOLUMINESCENCE_BLOOM_MIN_HARVESTS = int("bioluminescenceBloomMinHarvests", 2, 1, 16, "Minimum number of times a single bloom can be harvested.")
    val BIOLUMINESCENCE_BLOOM_MAX_HARVESTS = int("bioluminescenceBloomMaxHarvests", 4, 1, 16, "Maximum number of times a single bloom can be harvested.")

    val BIOLUMINESCENCE_CRYSTAL_JELLY_ENABLED = bool("bioluminescenceCrystalJellyEnabled", true, "Allows Crystal Jellies to populate bioluminescent waves.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_SMALL_MIN_COUNT = int("bioluminescenceCrystalJellySmallMinCount", 3, 0, 32, "Fewest Crystal Jellies a small wave can hold.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_SMALL_MAX_COUNT = int("bioluminescenceCrystalJellySmallMaxCount", 8, 0, 32, "Most Crystal Jellies a small wave can hold.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_LARGE_MIN_COUNT = int("bioluminescenceCrystalJellyLargeMinCount", 8, 0, 64, "Fewest Crystal Jellies a large wave can hold.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_LARGE_MAX_COUNT = int("bioluminescenceCrystalJellyLargeMaxCount", 18, 0, 64, "Most Crystal Jellies a large wave can hold.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_CELLS_PER_JELLY = int("bioluminescenceCrystalJellyCellsPerJelly", 120, 20, 500, "Water surface cells of a wave required per Crystal Jelly before the per-size bounds are applied.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_INACTIVE_MULTIPLIER = double("bioluminescenceCrystalJellyInactiveMultiplier", 0.5, 0.0, 2.0, "Population multiplier applied to Crystal Jellies in an inactive wave.")
    val BIOLUMINESCENCE_CRYSTAL_JELLY_TOTAL_NIGHT_MULTIPLIER = double("bioluminescenceCrystalJellyTotalNightMultiplier", 1.5, 0.0, 4.0, "Population multiplier applied to Crystal Jellies in a total night wave.")

    val CRYSTAL_JELLY_MIN_PULSES = int("crystalJellyMinimumPulses", 2, 1, 16, "Fewest propulsions a Crystal Jelly chains together in one burst.")
    val CRYSTAL_JELLY_MAX_PULSES = int("crystalJellyMaximumPulses", 5, 1, 16, "Most propulsions a Crystal Jelly chains together in one burst.")
    val CRYSTAL_JELLY_MIN_PULSE_DISTANCE = double("crystalJellyMinimumPulseDistance", 0.5, 0.05, 4.0, "Shortest distance covered by a single Crystal Jelly propulsion.")
    val CRYSTAL_JELLY_MAX_PULSE_DISTANCE = double("crystalJellyMaximumPulseDistance", 0.9, 0.05, 4.0, "Longest distance covered by a single Crystal Jelly propulsion.")
    val CRYSTAL_JELLY_MAX_TILT_DEGREES = double("crystalJellyMaximumTiltDegrees", 30.0, 0.0, 90.0, "Widest angle from straight up a Crystal Jelly may aim a burst at.")
    val CRYSTAL_JELLY_DRIFT_MEAN_TICKS = int("crystalJellyDriftMeanTicks", 60, 10, 1200, "Mean of the exponential law drawing how long a Crystal Jelly sinks between two bursts.")
    val CRYSTAL_JELLY_SURFACE_PROXIMITY = int("crystalJellySurfaceProximityBlocks", 1, 0, 16, "Distance to the water surface under which a Crystal Jelly stops propelling and sinks back down.")
    val CRYSTAL_JELLY_MIN_SURFACE_RETREAT = double("crystalJellyMinimumSurfaceRetreat", 2.0, 0.5, 16.0, "Shortest depth a Crystal Jelly sinks after reaching the surface.")
    val CRYSTAL_JELLY_MAX_SURFACE_RETREAT = double("crystalJellyMaximumSurfaceRetreat", 3.0, 0.5, 16.0, "Deepest a Crystal Jelly sinks after reaching the surface.")

    val CRYSTAL_JELLY_BOTTLE_HARVEST_ENABLED = bool("crystalJellyBottleHarvestEnabled", true, "Allows players to harvest a living Crystal Jelly into a bottle of bioluminescent plankton.")
    val CRYSTAL_JELLY_BOTTLE_HARVEST_COOLDOWN = int("crystalJellyBottleHarvestCooldownTicks", 2700, 20, 72000, "Ticks a Crystal Jelly needs before it can be harvested into a bottle again.")
    val CRYSTAL_JELLY_BOTTLE_FILLS_REQUIRED = int("crystalJellyBottleFillsRequired", 3, 1, 8, "Crystal Jellies that must be harvested to fill one bottle of bioluminescent plankton.")
    val PROPULSION_AMPLIFIER_SCALE = double("propulsionAmplifierScale", 0.5, 0.0, 2.0, "Extra Propulsion strength per effect level above the first.")
    val PROPULSION_CYCLE_TICKS = int("propulsionCycleTicks", 16, 4, 60, "Length of one Propulsion charge-and-jet cycle, matching the Crystal Jelly's own pulse.")
    val PROPULSION_CHARGE_TICKS = int("propulsionChargeTicks", 4, 1, 30, "Ticks a swimmer coils up before each Propulsion jet.")
    val PROPULSION_BURST_SPEED = double("propulsionBurstSpeed", 0.25, 0.01, 1.5, "Peak speed added at the start of a Propulsion jet, in blocks per tick.")
    val PROPULSION_SPEED_DECAY = double("propulsionSpeedDecay", 2.0, 0.5, 5.0, "How sharply a Propulsion jet fades out; matches the Crystal Jelly's own pulse profile.")

    val SHALLOW_DEEP_BOUNDARY = double("shallowDeepBoundary", -0.330, -0.915, -0.19, "Continentalness boundary between shallow and deep ocean. Vanilla: -0.455")
    val DEEP_ABYSSAL_BOUNDARY = double("deepAbyssalBoundary", -0.516, -0.965, -0.24, "Continentalness boundary between deep ocean and abyssal zone.")
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
    val FAULT_FREQUENCY_PERCENT = double("faultFrequencyPercent", NoiseThresholdCalibration.percentForFaultThreshold(0.012), 0.0, 100.0, "Percentage of the abyssal floor covered by fault lines.")
    val FAULT_BLEND = double("faultBlend", 0.045, 0.001, 0.2, "Blend width for fault line edges.")
    val FAULT_OFFSET_AMP = double("faultOffsetAmp", 10.0, 0.0, 22.0, "Maximum vertical offset caused by fault lines (blocks).")
    val TRENCH_FREQUENCY_PERCENT = double("trenchFrequencyPercent", NoiseThresholdCalibration.percentForTrenchThreshold(0.88), 0.0, 100.0, "Percentage of the abyssal floor eligible to carve into trenches.")
    val TRENCH_WALL_STEEPNESS = double("trenchWallSteepness", 2.0, 0.5, 6.0, "Shape exponent of trench walls. Higher values make the bedrock-reaching fissures narrower and steeper.")
    val TERRACE_STEP = double("terraceStep", 13.0, 2.0, 40.0, "Vertical height of each terrace step (blocks).")
    val TERRACE_FREQUENCY_PERCENT = double("terraceFrequencyPercent", NoiseThresholdCalibration.percentForTerraceThreshold(0.80), 0.0, 100.0, "Percentage of eligible abyssal slopes that can develop terraces.")
    val OCEAN_TERRITORY_EXTRA_ZOOMS = int("oceanTerritoryExtraZooms", 6, 0, 10, "Size of ocean territories per mod, in number of extra zooms. Higher = larger territories.")
    val OCEAN_TERRITORY_DEFAULT_WEIGHT = int("oceanTerritoryDefaultWeight", 10, 1, 1000, "Default weight of an unknown namespace in territorial competition.")
    val OCEAN_TERRITORY_OWN_WEIGHT = int("oceanTerritoryOwnWeight", 20, 1, 1000, "Weight of Abyssal Bloom in territorial competition, relative to the default weight.")
    val OCEAN_TERRITORY_INCLUDE_VANILLA = bool("oceanTerritoryIncludeVanilla", false, "If disabled, vanilla ocean biomes act as a safety net rather than competing with mods for territory.")

    val ABYSSAL_ORE_ENRICHMENT_ENABLED = bool("abyssalOreEnrichmentEnabled", true, "Runs additional deterministic placement passes for ore features already present in abyssal biomes.")
    val ABYSSAL_ORE_BONUS_PASSES = int("abyssalOreBonusPasses", 1, 0, 4, "Additional placement passes for each detected ore feature in abyssal chunks. One bonus pass doubles the normal attempts.")
    val ABYSSAL_ORE_CATALOG_DEBUG = bool("abyssalOreCatalogDebug", false, "Logs the placed features detected as ores when a server starts.")
    val ABYSSAL_ORE_FEATURE_ALLOWLIST = stringList("abyssalOreFeatureAllowlist", emptyList(), "Placed feature identifiers always treated as ores, including custom feature implementations.")
    val ABYSSAL_ORE_FEATURE_DENYLIST = stringList("abyssalOreFeatureDenylist", emptyList(), "Placed feature identifiers excluded from abyssal enrichment.")

    val SPEC: ModConfigSpec = builder.build()

    val options: List<ConfigOption<*>> = registered.toList().also { registered.clear() }
}
