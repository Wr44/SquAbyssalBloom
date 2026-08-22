package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.footprint

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentRenderOpacity.ACTIVE_WAVE_MAXIMUM_OPACITY
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentRenderOpacity.INACTIVE_WAVE_MAXIMUM_OPACITY
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.player.Player
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import java.util.ArrayDeque
import java.util.UUID

object BioluminescentFootprintField {
    private const val STEP_SPACING = 0.56
    private const val FOOT_LATERAL_OFFSET = 0.13
    private const val MAX_MOVEMENT_PER_TICK = 1.5
    private const val MAX_VERTICAL_MOVEMENT_PER_TICK = 0.75
    private const val MAX_STEPS_PER_PLAYER_PER_TICK = 4
    private const val MAX_FOOTPRINTS = 512
    private const val MINIMUM_WAVE_LIFECYCLE = 0.02f

    data class PlayerStepState(
        var lastX: Double,
        var lastY: Double,
        var lastZ: Double,
        var lastGameTime: Long,
        var distanceSinceStep: Double = 0.0,
        var nextFootIsLeft: Boolean = true,
        var stepIndex: Long = 0L
    )

    private val footprints = ArrayDeque<BioluminescentFootprint>()
    private val playerStates = hashMapOf<UUID, PlayerStepState>()

    val hasFootprints: Boolean
        get() = footprints.isNotEmpty()

    fun tick(level: ClientLevel, zones: Collection<BioluminescentZone>, gameTime: Long) {
        if (
            !ModConfig.enableBioluminescenceFootprints ||
            ModConfig.bioluminescenceFootprintOpacity <= 0.0
        ) {
            clear()
            return
        }
        discardExpired(gameTime.toDouble())

        val eligibleZones = zones.filter { zone ->
            zone.isReady && zone.spatialData != null &&
                zone.lifecycleIntensityAt(gameTime.toDouble()) > MINIMUM_WAVE_LIFECYCLE
        }
        if (eligibleZones.isEmpty()) {
            playerStates.clear()
            return
        }

        val seenPlayers = HashSet<UUID>()
        for (player in level.players()) {
            seenPlayers.add(player.uuid)
            tickPlayer(level, player, eligibleZones, gameTime)
        }
        val stateIterator = playerStates.keys.iterator()
        while (stateIterator.hasNext()) {
            if (stateIterator.next() !in seenPlayers) stateIterator.remove()
        }
    }

    fun clear() {
        footprints.clear()
        playerStates.clear()
    }

    fun forEachVisible(renderGameTime: Double, action: (BioluminescentFootprint, Float) -> Unit) {
        for (footprint in footprints) {
            val opacity = footprint.opacityAt(renderGameTime)
            if (opacity > 0.0f) action(footprint, opacity)
        }
    }

    private fun tickPlayer(
        level: ClientLevel,
        player: Player,
        zones: List<BioluminescentZone>,
        gameTime: Long
    ) {
        val state = playerStates.getOrPut(player.uuid) {
            PlayerStepState(player.x, player.y, player.z, gameTime)
        }
        val previousX = state.lastX
        val previousY = state.lastY
        val previousZ = state.lastZ
        val elapsedTicks = gameTime - state.lastGameTime
        state.lastX = player.x
        state.lastY = player.y
        state.lastZ = player.z
        state.lastGameTime = gameTime

        if (elapsedTicks !in 1L..2L || !canLeaveFootprints(player)) {
            state.distanceSinceStep = 0.0
            return
        }

        val movementX = player.x - previousX
        val movementY = player.y - previousY
        val movementZ = player.z - previousZ
        val movementDistance = sqrt(movementX * movementX + movementZ * movementZ)
        if (movementDistance <= 1.0e-4) return
        if (movementDistance > MAX_MOVEMENT_PER_TICK || abs(movementY) > MAX_VERTICAL_MOVEMENT_PER_TICK) {
            state.distanceSinceStep = 0.0
            return
        }

        val forwardX = movementX / movementDistance
        val forwardZ = movementZ / movementDistance
        var distanceToNextStep = STEP_SPACING - state.distanceSinceStep
        var emittedSteps = 0
        while (
            distanceToNextStep <= movementDistance + 1.0e-6 &&
            emittedSteps < MAX_STEPS_PER_PLAYER_PER_TICK
        ) {
            val pathProgress = (distanceToNextStep / movementDistance).coerceIn(0.0, 1.0)
            val pathX = previousX + movementX * pathProgress
            val pathY = previousY + movementY * pathProgress
            val pathZ = previousZ + movementZ * pathProgress
            val isLeftFoot = state.nextFootIsLeft
            val side = if (isLeftFoot) -1.0 else 1.0
            val footX = pathX - forwardZ * FOOT_LATERAL_OFFSET * side
            val footZ = pathZ + forwardX * FOOT_LATERAL_OFFSET * side

            tryCreateFootprint(
                level, player, zones,
                footX, pathY, footZ,
                forwardX, forwardZ,
                state.stepIndex,
                gameTime
            )

            state.nextFootIsLeft = !state.nextFootIsLeft
            state.stepIndex++
            emittedSteps++
            distanceToNextStep += STEP_SPACING
        }

        state.distanceSinceStep = if (
            emittedSteps >= MAX_STEPS_PER_PLAYER_PER_TICK &&
            distanceToNextStep <= movementDistance
        ) {
            0.0
        } else {
            (state.distanceSinceStep + movementDistance) % STEP_SPACING
        }
    }

    private fun canLeaveFootprints(player: Player): Boolean {
        return player.isAlive && !player.isSpectator && !player.isPassenger &&
            !player.isInWater && !player.abilities.flying && player.onGround()
    }

    private fun tryCreateFootprint(
        level: ClientLevel,
        player: Player,
        zones: List<BioluminescentZone>,
        worldX: Double,
        sampledSurfaceY: Double,
        worldZ: Double,
        forwardX: Double,
        forwardZ: Double,
        stepIndex: Long,
        gameTime: Long
    ) {
        val blockX = floor(worldX).toInt()
        val blockZ = floor(worldZ).toInt()
        val proximity = BioluminescentFootprintPlacement.nearestEligibleZone(zones, blockX, blockZ) ?: return
        val zone = proximity.zone
        val normalizedDistance = sqrt(proximity.distanceSquared.toDouble()) / proximity.radius
        val fadeStart = ModConfig.bioluminescenceFootprintDistanceFadeStart.coerceIn(0.0, 0.95)
        val distanceOpacity = 1.0 - ModUtilities.smooth(fadeStart, 1.0, normalizedDistance)
        if (distanceOpacity <= 1.0e-3) return
        val surfaceY = BioluminescentFootprintPlacement.resolveWalkedSurface(
            level,
            worldX,
            sampledSurfaceY,
            worldZ
        ) ?: return
        if (
            !BioluminescentFootprintPlacement.hasCompleteFootprintSupport(
                level,
                worldX,
                surfaceY,
                worldZ,
                forwardX,
                forwardZ
            )
        ) return

        val color = BioluminescentFootprintPlacement.continuousFootprintColor(zone, player, stepIndex)
        val lifecycle = zone.lifecycleIntensityAt(gameTime.toDouble()).coerceIn(0.0f, 1.0f)
        val waveMaximumOpacity = when (zone.activity) {
            BioluminescentZoneActivity.ACTIVE -> ACTIVE_WAVE_MAXIMUM_OPACITY
            BioluminescentZoneActivity.INACTIVE -> INACTIVE_WAVE_MAXIMUM_OPACITY
        }

        footprints.addLast(
            BioluminescentFootprint(
                centerX = worldX,
                surfaceY = surfaceY,
                centerZ = worldZ,
                forwardX = forwardX,
                forwardZ = forwardZ,
                color = color,
                visibilityFactor = (lifecycle * distanceOpacity).toFloat(),
                waveMaximumOpacity = waveMaximumOpacity,
                createdAtGameTime = gameTime
            )
        )
        while (footprints.size > MAX_FOOTPRINTS) footprints.removeFirst()
    }

    private fun discardExpired(renderGameTime: Double) {
        while (true) {
            val first = footprints.peekFirst() ?: return
            if (first.opacityAt(renderGameTime) > 0.0f) return
            footprints.removeFirst()
        }
    }
}
