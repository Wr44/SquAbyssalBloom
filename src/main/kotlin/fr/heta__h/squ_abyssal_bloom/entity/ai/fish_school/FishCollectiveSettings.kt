package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
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
    val threatDetectionRadius: Double,
    val threatPropagationSpeed: Double,
    val threatSignalDecay: Double
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
                threatDetectionRadius =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_DETECTION_RADIUS),
                threatPropagationSpeed =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_PROPAGATION_SPEED),
                threatSignalDecay =
                    ServerConfigCache.current(ModServerConfig.FISH_SCHOOL_THREAT_SIGNAL_DECAY)
            )
        }
    }
}
