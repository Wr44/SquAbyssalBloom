package fr.heta__h.squ_abyssal_bloom.util.conduit

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ConduitBlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import java.util.concurrent.ConcurrentHashMap

object AstralPrismarineTracker {
    private val activeBlocksByLevel = ConcurrentHashMap<ServerLevel, MutableSet<BlockPos>>()
    private val orphanIteratorsByLevel = ConcurrentHashMap<ServerLevel, MutableIterator<BlockPos>>()
    private const val MAX_TRACKED_BLOCKS = 100000

    private fun activeBlocks(level: ServerLevel): MutableSet<BlockPos> =
        activeBlocksByLevel.computeIfAbsent(level) { ConcurrentHashMap.newKeySet() }

    fun markActive(level: ServerLevel, pos: BlockPos) {
        val activeBlocks = activeBlocks(level)
        if (activeBlocks.size >= MAX_TRACKED_BLOCKS) return
        activeBlocks.add(pos.immutable())
    }

    fun markInactive(level: ServerLevel, pos: BlockPos) {
        activeBlocksByLevel[level]?.remove(pos)
    }

    fun releaseLevel(level: ServerLevel) {
        orphanIteratorsByLevel.remove(level)
        activeBlocksByLevel.remove(level)
    }

    fun checkOrphans(level: ServerLevel) {
        val activeBlocks = activeBlocksByLevel[level] ?: return
        var iterator = orphanIteratorsByLevel[level]
        if (iterator == null || !iterator.hasNext()) {
            iterator = activeBlocks.iterator()
            orphanIteratorsByLevel[level] = iterator
        }

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
                iterator.remove()
            }
        }

        if (!iterator.hasNext()) {
            orphanIteratorsByLevel.remove(level, iterator)
        }

        if (activeBlocks.isEmpty()) {
            orphanIteratorsByLevel.remove(level)
            activeBlocksByLevel.remove(level, activeBlocks)
        }
    }

    fun onChunkLoad(chunk: LevelChunk) {
        val level = chunk.level as? ServerLevel ?: return
        val sections = chunk.sections

        for (i in sections.indices) {
            val section = sections[i]

            if (section.hasOnlyAir()) continue

            if (!section.maybeHas { it.`is`(ModBlocks.ASTRAL_PRISMARINE) }) continue

            val sectionBaseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i))

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
                                markActive(level, worldPos)
                            } else {
                                level.setBlock(
                                    worldPos,
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
