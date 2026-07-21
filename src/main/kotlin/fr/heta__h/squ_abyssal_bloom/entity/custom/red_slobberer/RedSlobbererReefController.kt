package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SeaPickleBlock
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.floor

class RedSlobbererReefController(
    private val redSlobberer: RedSlobbererEntity,
    private val groupController: RedSlobbererGroupController
) {

    private var reefAnchor: BlockPos? = null
    private var residenceTicks = 0
    private var placedDecorations = 0
    private var nextEcologyTick = 0
    private var nextDecorationTick = 0

    fun tick(serverLevel: ServerLevel) {
        if (redSlobberer.tickCount < nextEcologyTick) return
        nextEcologyTick = redSlobberer.tickCount + ECOLOGY_INTERVAL_TICKS

        val members = groupController.members()
        if (members.size < MINIMUM_GROUP_SIZE || !groupController.isLeader()) return

        val groupCenter = groupController.center()
        val reefMaturityTicks = ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS.get()
        updateResidence(groupCenter)
        maintainFishPopulation(serverLevel, members.size, reefMaturityTicks)

        if (
            residenceTicks >= reefMaturityTicks &&
            placedDecorations < MAX_DECORATIONS_PER_REEF &&
            redSlobberer.tickCount >= nextDecorationTick
        ) {
            nextDecorationTick = redSlobberer.tickCount + DECORATION_INTERVAL_TICKS
            if (serverLevel.gameRules.get(GameRules.MOB_GRIEFING) && tryPlaceReefDecoration(serverLevel)) {
                placedDecorations++
            }
        }
    }

    fun addAdditionalSaveData(output: ValueOutput) {
        reefAnchor?.let { anchor ->
            output.putBoolean(TAG_HAS_REEF_ANCHOR, true)
            output.putLong(TAG_REEF_ANCHOR, anchor.asLong())
        }
        output.putInt(TAG_RESIDENCE_TICKS, residenceTicks)
        output.putInt(TAG_PLACED_DECORATIONS, placedDecorations)
    }

    fun readAdditionalSaveData(input: ValueInput) {
        reefAnchor = if (input.getBooleanOr(TAG_HAS_REEF_ANCHOR, false)) {
            BlockPos.of(input.getLongOr(TAG_REEF_ANCHOR, BlockPos.ZERO.asLong()))
        } else {
            null
        }
        residenceTicks = input.getIntOr(TAG_RESIDENCE_TICKS, 0)
            .coerceIn(0, MAX_TRACKED_RESIDENCE_TICKS)
        placedDecorations = input.getIntOr(TAG_PLACED_DECORATIONS, 0)
            .coerceIn(0, MAX_DECORATIONS_PER_REEF)
    }

    private fun updateResidence(groupCenter: Vec3) {
        val currentAnchor = reefAnchor
        if (currentAnchor == null || hasLeftReefArea(currentAnchor, groupCenter)) {
            reefAnchor = BlockPos.containing(groupCenter)
            residenceTicks = 0
            placedDecorations = 0
            return
        }

        residenceTicks = (residenceTicks + ECOLOGY_INTERVAL_TICKS).coerceAtMost(MAX_TRACKED_RESIDENCE_TICKS)
    }

    private fun hasLeftReefArea(anchor: BlockPos, groupCenter: Vec3): Boolean {
        val dx = groupCenter.x - (anchor.x + 0.5)
        val dz = groupCenter.z - (anchor.z + 0.5)
        return dx * dx + dz * dz > REEF_RESIDENCE_RADIUS_SQR ||
            abs(groupCenter.y - anchor.y) > MAX_RESIDENCE_VERTICAL_DRIFT
    }

    private fun maintainFishPopulation(
        serverLevel: ServerLevel,
        groupSize: Int,
        reefMaturityTicks: Int
    ) {
        if (!serverLevel.gameRules.get(GameRules.SPAWN_MOBS)) return

        val maturityBonus = if (residenceTicks < reefMaturityTicks) {
            0
        } else {
            (1 + (residenceTicks - reefMaturityTicks) / FISH_MATURITY_STEP_TICKS)
                .coerceAtMost(MAX_MATURE_REEF_FISH_BONUS)
        }
        val desiredFish = (BASE_FISH_COUNT + groupSize * FISH_PER_GROUP_MEMBER + maturityBonus)
            .coerceAtMost(ModServerConfig.RED_SLOBBERER_MAX_REEF_FISH.get())

        val nearbyFish = serverLevel.getEntitiesOfClass(
            AbstractFish::class.java,
            redSlobberer.boundingBox.inflate(FISH_COUNT_RADIUS, FISH_COUNT_VERTICAL_RADIUS, FISH_COUNT_RADIUS)
        ) { fish -> fish.isAlive && fish.isInWater }.size

        if (nearbyFish >= desiredFish || redSlobberer.random.nextFloat() > FISH_SPAWN_ATTEMPT_CHANCE) return
        trySpawnFish(serverLevel, groupController.center())
    }

    private fun trySpawnFish(serverLevel: ServerLevel, groupCenter: Vec3) {
        val centerY = floor(groupCenter.y).toInt()

        repeat(FISH_SPAWN_POSITION_ATTEMPTS) {
            val dx = redSlobberer.random.nextInt(-FISH_SPAWN_RADIUS, FISH_SPAWN_RADIUS + 1)
            val dz = redSlobberer.random.nextInt(-FISH_SPAWN_RADIUS, FISH_SPAWN_RADIUS + 1)
            if (dx * dx + dz * dz < MIN_FISH_SPAWN_DISTANCE_SQR) return@repeat

            val pos = BlockPos(
                floor(groupCenter.x).toInt() + dx,
                centerY + MIN_FISH_HEIGHT + redSlobberer.random.nextInt(FISH_HEIGHT_VARIATION),
                floor(groupCenter.z).toInt() + dz
            )
            if (!serverLevel.isLoaded(pos)) return@repeat
            if (!serverLevel.getFluidState(pos).`is`(FluidTags.WATER)) return@repeat
            if (!serverLevel.getFluidState(pos.above()).`is`(FluidTags.WATER)) return@repeat

            if (EntityType.TROPICAL_FISH.spawn(serverLevel, pos, EntitySpawnReason.NATURAL) != null) return
        }
    }

    private fun tryPlaceReefDecoration(serverLevel: ServerLevel): Boolean {
        val anchor = reefAnchor ?: return false

        positionAttempt@ for (positionAttempt in 0 until DECORATION_POSITION_ATTEMPTS) {
            val x = anchor.x + redSlobberer.random.nextInt(-DECORATION_RADIUS, DECORATION_RADIUS + 1)
            val z = anchor.z + redSlobberer.random.nextInt(-DECORATION_RADIUS, DECORATION_RADIUS + 1)

            val candidate = BlockPos.MutableBlockPos(x, anchor.y + DECORATION_SEARCH_ABOVE, z)

            for (verticalAttempt in 0 until DECORATION_VERTICAL_SEARCH) {
                if (!serverLevel.isLoaded(candidate)) continue@positionAttempt

                val candidateState = serverLevel.getBlockState(candidate)
                val supportPos = candidate.below()
                val supportState = serverLevel.getBlockState(supportPos)
                val hasSupport = !supportState.getCollisionShape(serverLevel, supportPos).isEmpty

                if (candidateState.`is`(Blocks.WATER) && hasSupport) {
                    val decoration = if (redSlobberer.random.nextFloat() < SEA_PICKLE_CHANCE) {
                        Blocks.SEA_PICKLE.defaultBlockState().setValue(
                            SeaPickleBlock.PICKLES,
                            1 + redSlobberer.random.nextInt(MAX_NEW_PICKLES)
                        )
                    } else {
                        Blocks.FIRE_CORAL_FAN.defaultBlockState()
                    }

                    if (
                        decoration.canSurvive(serverLevel, candidate) &&
                        serverLevel.setBlock(candidate, decoration, Block.UPDATE_ALL)
                    ) {
                        return true
                    }
                }

                candidate.move(Direction.DOWN)
            }
        }

        return false
    }

    private companion object {
        const val TAG_HAS_REEF_ANCHOR = "RedSlobbererHasReefAnchor"
        const val TAG_REEF_ANCHOR = "RedSlobbererReefAnchor"
        const val TAG_RESIDENCE_TICKS = "RedSlobbererReefResidenceTicks"
        const val TAG_PLACED_DECORATIONS = "RedSlobbererReefDecorations"

        const val MINIMUM_GROUP_SIZE = 2
        const val ECOLOGY_INTERVAL_TICKS = 100
        const val MAX_TRACKED_RESIDENCE_TICKS = 24000
        const val REEF_RESIDENCE_RADIUS_SQR = 144.0
        const val MAX_RESIDENCE_VERTICAL_DRIFT = 6.0

        const val BASE_FISH_COUNT = 2
        const val FISH_PER_GROUP_MEMBER = 2
        const val MAX_MATURE_REEF_FISH_BONUS = 4
        const val FISH_MATURITY_STEP_TICKS = 1200
        const val FISH_COUNT_RADIUS = 24.0
        const val FISH_COUNT_VERTICAL_RADIUS = 10.0
        const val FISH_SPAWN_ATTEMPT_CHANCE = 0.65f
        const val FISH_SPAWN_POSITION_ATTEMPTS = 8
        const val FISH_SPAWN_RADIUS = 12
        const val MIN_FISH_SPAWN_DISTANCE_SQR = 25
        const val MIN_FISH_HEIGHT = 2
        const val FISH_HEIGHT_VARIATION = 5

        const val DECORATION_INTERVAL_TICKS = 1200
        const val MAX_DECORATIONS_PER_REEF = 8
        const val DECORATION_POSITION_ATTEMPTS = 8
        const val DECORATION_RADIUS = 8
        const val DECORATION_SEARCH_ABOVE = 4
        const val DECORATION_VERTICAL_SEARCH = 10
        const val SEA_PICKLE_CHANCE = 0.7f
        const val MAX_NEW_PICKLES = 2
    }
}
