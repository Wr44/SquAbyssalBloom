package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.gamerules.GameRules

class RedSlobbererGrazeBloodSeagrassGoal(
    private val redSlobberer: RedSlobbererEntity,
    speedModifier: Double
) : MoveToBlockGoal(redSlobberer, speedModifier, SEARCH_RADIUS, VERTICAL_SEARCH_RANGE) {

    private var grazingTicks = 0
    private var consumedTarget = false

    override fun canUse(): Boolean = canGraze() && super.canUse()

    override fun canContinueToUse(): Boolean = canGraze() && !consumedTarget && super.canContinueToUse()

    override fun start() {
        grazingTicks = 0
        consumedTarget = false
        super.start()
    }

    override fun tick() {
        super.tick()
        if (!isReachedTarget()) {
            grazingTicks = 0
            return
        }

        redSlobberer.navigation.stop()
        grazingTicks++
        if (grazingTicks < GRAZING_DURATION_TICKS) return

        val serverLevel = redSlobberer.level() as? ServerLevel ?: return
        if (!serverLevel.gameRules.get(GameRules.MOB_GRIEFING)) return
        val state = serverLevel.getBlockState(blockPos)
        if (state.`is`(ModBlocks.BLOOD_SEAGRASS.get()) || state.`is`(ModBlocks.TALL_BLOOD_SEAGRASS.get())) {
            serverLevel.destroyBlock(blockPos, false, redSlobberer)
        }
        consumedTarget = true
    }

    override fun stop() {
        grazingTicks = 0
        consumedTarget = false
        super.stop()
    }

    override fun acceptedDistance(): Double = redSlobberer.bbWidth.toDouble() * 0.5 + 0.9

    override fun getMoveToTarget(): BlockPos = blockPos

    override fun moveMobToBlock() {
        redSlobberer.navigation.moveTo(
            blockPos.x + 0.5,
            blockPos.y.toDouble(),
            blockPos.z + 0.5,
            speedModifier
        )
    }

    override fun isValidTarget(level: LevelReader, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (state.`is`(ModBlocks.BLOOD_SEAGRASS.get())) return true
        return state.`is`(ModBlocks.TALL_BLOOD_SEAGRASS.get()) &&
            state.getValue(TallSeagrassBlock.HALF) == DoubleBlockHalf.LOWER
    }

    private fun canGraze(): Boolean {
        val serverLevel = redSlobberer.level() as? ServerLevel ?: return false
        return serverLevel.gameRules.get(GameRules.MOB_GRIEFING)
    }

    private companion object {
        const val SEARCH_RADIUS = 12
        const val VERTICAL_SEARCH_RANGE = 4
        const val GRAZING_DURATION_TICKS = 40
    }
}
