package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import kotlin.math.cos
import kotlin.math.max

data class FishCollectiveSettings(
    val debugEnabled: Boolean,
    val neighborCount: Int,
    val neighborSearchRadius: Double,
    val neighborSearchRadiusSqr: Double,
    val aggregationRadius: Double,
    val aggregationRadiusSqr: Double,
    val crossSpeciesAffinity: Double,
    val activationThreshold: Int,
    val deactivationThreshold: Int,
    val activationDelayTicks: Int,
    val deactivationDelayTicks: Int,
    val separationWeight: Double,
    val alignmentWeight: Double,
    val cohesionWeight: Double,
    val obstacleAvoidanceWeight: Double,
    val threatAvoidanceWeight: Double,
    val maximumFishSpeed: Double,
    val panicFishSpeed: Double,
    val maximumTurnRate: Double,
    val verticalMovementWeight: Double,
    val verticalDriftSpeed: Double,
    val neighborRefreshInterval: Int,
    val threatRefreshInterval: Int,
    val longRangeRefreshInterval: Int,
    val threatDetectionRadius: Double,
    val threatPropagationSpeed: Double,
    val threatSignalDecay: Double,
    val neighborFovCosine: Double,
    val aggregationCohesionScale: Double,
    val cohesionSpeedResponseScale: Double,
    val separationRadius: Double,
    val separationRadiusSqr: Double,
    val maximumSeparationForce: Double,
    val cohesionFullStrengthDistance: Double,
    val herdCompression: Double
) {
    companion object {
        fun fromConfig(): FishCollectiveSettings {
            val neighborSearchRadius =
                ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_NEIGHBOR_SEARCH_RADIUS)
            val aggregationRadius = max(
                neighborSearchRadius,
                ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_AGGREGATION_RADIUS)
            )
            val activationThreshold =
                ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_ACTIVATION_THRESHOLD)
                    .coerceAtLeast(2)
            val deactivationThreshold =
                ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_DEACTIVATION_THRESHOLD)
                    .coerceIn(0, activationThreshold - 1)
            val maximumFishSpeed =
                ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_MAXIMUM_SPEED)
            val separationRadius =
                ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_SEPARATION_RADIUS)

            return FishCollectiveSettings(
                debugEnabled = ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_DEBUG),
                neighborCount = ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_NEIGHBOR_COUNT),
                neighborSearchRadius = neighborSearchRadius,
                neighborSearchRadiusSqr = neighborSearchRadius * neighborSearchRadius,
                aggregationRadius = aggregationRadius,
                aggregationRadiusSqr = aggregationRadius * aggregationRadius,
                crossSpeciesAffinity =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_CROSS_SPECIES_AFFINITY),
                activationThreshold = activationThreshold,
                deactivationThreshold = deactivationThreshold,
                activationDelayTicks =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_ACTIVATION_DELAY),
                deactivationDelayTicks =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_DEACTIVATION_DELAY),
                separationWeight =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_SEPARATION_WEIGHT),
                alignmentWeight =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_ALIGNMENT_WEIGHT),
                cohesionWeight =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_COHESION_WEIGHT),
                obstacleAvoidanceWeight =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_OBSTACLE_AVOIDANCE_WEIGHT),
                threatAvoidanceWeight =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_AVOIDANCE_WEIGHT),
                maximumFishSpeed = maximumFishSpeed,
                panicFishSpeed = max(
                    maximumFishSpeed,
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_PANIC_SPEED)
                ),
                maximumTurnRate =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_MAXIMUM_TURN_RATE),
                verticalMovementWeight =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_VERTICAL_MOVEMENT_WEIGHT),
                verticalDriftSpeed =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_VERTICAL_DRIFT_SPEED),
                neighborRefreshInterval =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_NEIGHBOR_REFRESH_INTERVAL),
                threatRefreshInterval =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_REFRESH_INTERVAL),
                longRangeRefreshInterval =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_LONG_RANGE_REFRESH_INTERVAL),
                threatDetectionRadius =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_DETECTION_RADIUS),
                threatPropagationSpeed =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_PROPAGATION_SPEED),
                threatSignalDecay =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_SIGNAL_DECAY),
                neighborFovCosine = cos(
                    Math.toRadians(
                        ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_NEIGHBOR_FOV_HALF_ANGLE)
                    )
                ),
                aggregationCohesionScale =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_AGGREGATION_COHESION_SCALE),
                cohesionSpeedResponseScale =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_COHESION_SPEED_RESPONSE_SCALE),
                separationRadius = separationRadius,
                separationRadiusSqr = separationRadius * separationRadius,
                maximumSeparationForce =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_MAXIMUM_SEPARATION_FORCE),
                cohesionFullStrengthDistance =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_COHESION_FULL_STRENGTH_DISTANCE),
                herdCompression =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_HERD_COMPRESSION)
            )
        }
    }
}
