package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.goal

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveManager
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveSettings
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.fish.Pufferfish
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FishSchoolMovementGoal(
    private val fish: AbstractFish
) : Goal() {
    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        val serverLevel = fish.level() as? ServerLevel ?: return false
        if (!canCollectivelySwim()) return false

        return FishCollectiveManager.forLevel(serverLevel)
            .shouldUseCollectiveMovement(fish)
    }

    override fun canContinueToUse(): Boolean {
        val serverLevel = fish.level() as? ServerLevel ?: return false
        if (!canCollectivelySwim()) return false

        return FishCollectiveManager.forLevel(serverLevel)
            .shouldUseCollectiveMovement(fish)
    }

    override fun start() {
        fish.navigation.stop()
        val serverLevel = fish.level() as? ServerLevel ?: return
        FishCollectiveManager.forLevel(serverLevel)
            .setMovementControllerRunning(fish, true)
    }

    override fun stop() {
        val serverLevel = fish.level() as? ServerLevel ?: return
        FishCollectiveManager.forLevel(serverLevel)
            .setMovementControllerRunning(fish, false)
    }

    override fun tick() {
        val serverLevel = fish.level() as? ServerLevel ?: return
        val manager = FishCollectiveManager.forLevel(serverLevel)
        if (!fish.navigation.isDone) {
            fish.navigation.stop()
            manager.debugRecordNavigationSuppression(fish)
        }
        val desiredVelocity = manager.desiredVelocity(fish) ?: return
        if (fish.isRemoved) return
        applyDesiredVelocity(desiredVelocity, manager.currentSettings(), manager)
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun canCollectivelySwim(): Boolean {
        return fish.isAlive &&
            fish.isInWater &&
            !isDefensivePufferfish()
    }

    private fun applyDesiredVelocity(
        desiredVelocity: Vec3,
        settings: FishCollectiveSettings,
        manager: FishCollectiveManager
    ) {
        val storedVelocity = fish.deltaMovement
        val postTravelSinking = if (fish.target == null) VANILLA_FISH_SINKING else 0.0
        val currentVelocity = Vec3(
            storedVelocity.x / VANILLA_WATER_DRAG,
            (storedVelocity.y + postTravelSinking) / VANILLA_WATER_DRAG,
            storedVelocity.z / VANILLA_WATER_DRAG
        )
        val turnLimitedVelocity = limitHorizontalTurn(
            currentVelocity,
            desiredVelocity,
            settings.maximumTurnRate
        )

        val interpolatedVelocity = Vec3(
            Mth.lerp(HORIZONTAL_VELOCITY_INTERPOLATION, currentVelocity.x, turnLimitedVelocity.x),
            Mth.lerp(VERTICAL_VELOCITY_INTERPOLATION, currentVelocity.y, turnLimitedVelocity.y),
            Mth.lerp(HORIZONTAL_VELOCITY_INTERPOLATION, currentVelocity.z, turnLimitedVelocity.z)
        )
        var acceleration = interpolatedVelocity.subtract(currentVelocity)
        acceleration = Vec3(
            acceleration.x,
            acceleration.y.coerceIn(-MAXIMUM_VERTICAL_ACCELERATION, MAXIMUM_VERTICAL_ACCELERATION),
            acceleration.z
        )
        val accelerationLength = acceleration.length()
        if (accelerationLength > MAXIMUM_ACCELERATION_PER_TICK) {
            acceleration = acceleration.scale(MAXIMUM_ACCELERATION_PER_TICK / accelerationLength)
        }

        var nextVelocity = currentVelocity.add(acceleration)
        val nextSpeed = nextVelocity.length()
        if (nextSpeed > settings.panicFishSpeed) {
            nextVelocity = nextVelocity.scale(settings.panicFishSpeed / nextSpeed)
        }
        nextVelocity = Vec3(
            nextVelocity.x,
            nextVelocity.y.coerceIn(-MAXIMUM_VERTICAL_SPEED, MAXIMUM_VERTICAL_SPEED),
            nextVelocity.z
        )
        nextVelocity = manager.avoidPanicFishCollisions(fish, nextVelocity)

        val pendingVanillaBuoyancy = if (fish.isEyeInFluid(FluidTags.WATER)) {
            VANILLA_FISH_BUOYANCY
        } else {
            0.0
        }
        fish.deltaMovement = Vec3(
            nextVelocity.x,
            nextVelocity.y - pendingVanillaBuoyancy,
            nextVelocity.z
        )
        manager.debugRecordVelocityApplication(
            fish,
            currentVelocity,
            turnLimitedVelocity,
            nextVelocity
        )
    }

    private fun limitHorizontalTurn(
        currentVelocity: Vec3,
        desiredVelocity: Vec3,
        maximumTurnRateDegrees: Double
    ): Vec3 {
        val currentHorizontalSpeed = horizontalLength(currentVelocity)
        val desiredHorizontalSpeed = horizontalLength(desiredVelocity)
        if (
            currentHorizontalSpeed <= MIN_ROTATION_SPEED ||
            desiredHorizontalSpeed <= MIN_ROTATION_SPEED
        ) {
            return desiredVelocity
        }

        val currentAngle = atan2(currentVelocity.z, currentVelocity.x)
        val desiredAngle = atan2(desiredVelocity.z, desiredVelocity.x)
        val maximumTurnRadians = Math.toRadians(maximumTurnRateDegrees)
        val angleDifference = desiredAngle - currentAngle
        val turn = atan2(sin(angleDifference), cos(angleDifference))
            .coerceIn(-maximumTurnRadians, maximumTurnRadians)
        val limitedAngle = currentAngle + turn
        return Vec3(
            cos(limitedAngle) * desiredHorizontalSpeed,
            desiredVelocity.y,
            sin(limitedAngle) * desiredHorizontalSpeed
        )
    }

    private fun horizontalLength(vector: Vec3): Double {
        return sqrt(vector.x * vector.x + vector.z * vector.z)
    }

    private fun isDefensivePufferfish(): Boolean {
        return fish is Pufferfish && fish.puffState > 0
    }

    private companion object {
        const val MIN_ROTATION_SPEED = 1.0E-4
        const val VANILLA_WATER_DRAG = 0.9
        const val VANILLA_FISH_BUOYANCY = 0.005
        const val VANILLA_FISH_SINKING = 0.005
        const val HORIZONTAL_VELOCITY_INTERPOLATION = 0.5
        const val VERTICAL_VELOCITY_INTERPOLATION = 0.12
        const val MAXIMUM_ACCELERATION_PER_TICK = 0.018
        const val MAXIMUM_VERTICAL_ACCELERATION = 0.006
        const val MAXIMUM_VERTICAL_SPEED = 0.065
    }
}
