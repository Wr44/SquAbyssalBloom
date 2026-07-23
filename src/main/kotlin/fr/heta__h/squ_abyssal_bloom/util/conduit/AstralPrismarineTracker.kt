package fr.heta__h.squ_abyssal_bloom.util.conduit

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ConduitBlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import java.util.concurrent.ConcurrentHashMap

object AstralPrismarineTracker {
    private val activeBlocks = ConcurrentHashMap.newKeySet<BlockPos>()
    private const val MAX_TRACKED_BLOCKS = 100000

    fun markActive(pos: BlockPos) {
        if (activeBlocks.size >= MAX_TRACKED_BLOCKS) return
        activeBlocks.add(pos.immutable())
    }

    fun markInactive(pos: BlockPos) {
        activeBlocks.remove(pos)
    }

    fun checkOrphans(level: ServerLevel) {
        val iterator = activeBlocks.iterator()
        var checked = 0
        val maxChecksPerTick = 50

        while (iterator.hasNext() && checked < maxChecksPerTick) {
            val pos = iterator.next()
            checked++

            if (!level.isLoaded(pos)) continue

            val state = level.getBlockState(pos)

            if (!state.`is`(ModBlocks.ASTRAL_PRISMARINE)) {
                iterator.remove()
            } else if (!hasActiveConduitNearby(level, pos)) {
                level.setBlock(pos, state.setValue(AstralPrismarineBlock.ACTIVE, false), 3)
                level.sendBlockUpdated(pos , state, state.setValue(AstralPrismarineBlock.ACTIVE, false), 3)
                iterator.remove()
            }
        }
    }

    fun onChunkLoad(chunk: LevelChunk) {
        if (chunk.level.isClientSide) return

        val level = chunk.level
        val sections = chunk.sections

        for (i in sections.indices) {
            val section = sections[i]

            if (section.hasOnlyAir()) continue

            if (!section.maybeHas { it.`is`(ModBlocks.ASTRAL_PRISMARINE) }) continue

            val sectionBaseY = chunk.getSectionYFromSectionIndex(i)

            for (localX in 0..15) {
                for (localY in 0..15) {
                    for (localZ in 0..15) {
                        val state = section.getBlockState(localX, localY, localZ)

                        if (state.`is`(ModBlocks.ASTRAL_PRISMARINE) &&
                            state.getValue(AstralPrismarineBlock.ACTIVE)) {

                            val worldPos = BlockPos(
                                chunk.pos.minBlockX + localX,
                                sectionBaseY + localY,
                                chunk.pos.minBlockZ + localZ
                            )

                            if (hasActiveConduitNearby(level, worldPos)) {
                                markActive(worldPos)
                            } else {
                                level.setBlock(
                                    worldPos,
                                    state.setValue(AstralPrismarineBlock.ACTIVE, false),
                                    3
                                )
                                level.sendBlockUpdated(
                                    worldPos,
                                    state,
                                    state.setValue(AstralPrismarineBlock.ACTIVE, false),
                                    3
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun nearbyOffsetPositions(pos: BlockPos): List<BlockPos> {
        return listOf(
            pos.offset(0, 2, 0), pos.offset(0, -2, 0),
            pos.offset(0, 0, 2), pos.offset(0, 0, -2),
            pos.offset(2, 0, 0), pos.offset(-2, 0, 0),
            pos.offset(0, 2, 2), pos.offset(0, 2, -2),
            pos.offset(0, -2, 2), pos.offset(0, -2, -2),
            pos.offset(2, 0, 2), pos.offset(2, 0, -2),
            pos.offset(-2, 0, 2), pos.offset(-2, 0, -2),
            pos.offset(2, 2, 0), pos.offset(2, -2, 0),
            pos.offset(-2, 2, 0), pos.offset(-2, -2, 0)
        )
    }

    private fun hasActiveConduitNearby(level: Level, pos: BlockPos): Boolean {
        val possibleConduitPositions = nearbyOffsetPositions(pos)

        for (conduitPos in possibleConduitPositions) {
            val state = level.getBlockState(conduitPos)
            if (state.block == Blocks.CONDUIT) {
                val be = level.getBlockEntity(conduitPos) as? ConduitBlockEntity
                if (be?.isActive == true) return true
            }
        }

        return false
    }
}