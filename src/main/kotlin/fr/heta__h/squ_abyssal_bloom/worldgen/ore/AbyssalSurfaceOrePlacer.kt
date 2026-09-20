package fr.heta__h.squ_abyssal_bloom.worldgen.ore

import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature

object AbyssalSurfaceOrePlacer {

    private const val COLUMN_ATTEMPTS_PER_PASS = 12
    private const val RICH_REGION_CHUNKS = 8
    private const val RICH_REGION_CHANCE = 32
    private const val RICH_VEIN_MULTIPLIER = 2
    private const val MAX_NORMAL_VEIN_SIZE = 12
    private const val MAX_RICH_VEIN_SIZE = 24
    private const val COMMON_ORE_VEIN_CHANCE = 0.20f
    private const val PRECIOUS_ORE_VEIN_CHANCE = 0.035f
    private const val MODDED_ORE_VEIN_CHANCE = 0.08f

    private val horizontalDirections = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    private val chunkBudget = ThreadLocal.withInitial { ChunkBudget() }

    fun place(
        level: WorldGenLevel,
        random: RandomSource,
        chunkOrigin: BlockPos,
        feature: PlacedFeature,
        passes: Int
    ): Boolean {
        val configuration = AbyssalOreCatalog.configuration(feature) ?: return false
        var placed = false

        repeat(passes) {
            if (placeExposedVein(level, random, chunkOrigin, configuration, passes)) placed = true
        }

        return placed
    }

    private fun placeExposedVein(
        level: WorldGenLevel,
        random: RandomSource,
        chunkOrigin: BlockPos,
        configuration: OreConfiguration,
        configuredPasses: Int
    ): Boolean {
        val representativeOre = configuration.targetStates.firstOrNull()?.state ?: return false
        if (random.nextFloat() >= veinChance(representativeOre)) return false

        var start: BlockPos? = null
        repeat(COLUMN_ATTEMPTS_PER_PASS) {
            val x = chunkOrigin.x + random.nextInt(16)
            val z = chunkOrigin.z + random.nextInt(16)
            val candidate = exposedFloor(level, x, z) ?: return@repeat
            if (matchingTarget(level, candidate, random, configuration) != null) {
                start = candidate
                return@repeat
            }
        }

        val veinStart = start ?: return false

        val rich = isCommonOre(representativeOre) && isRichRegion(level.seed, veinStart)
        val normalLimit = (configuredPasses + 1).coerceAtMost(5)
        val chunkLimit = if (rich) normalLimit * 2 else normalLimit
        if (!reserveVein(level, chunkOrigin, chunkLimit)) return false

        val baseSize = (configuration.size * (0.30 + random.nextDouble() * 0.25)).toInt().coerceAtLeast(2)
        val targetSize = if (rich) {
            (baseSize * RICH_VEIN_MULTIPLIER).coerceAtMost(MAX_RICH_VEIN_SIZE)
        } else {
            baseSize.coerceAtMost(MAX_NORMAL_VEIN_SIZE)
        }

        val frontier = mutableListOf(veinStart)
        val visited = HashSet<Long>()
        var placed = 0

        while (frontier.isNotEmpty() && placed < targetSize) {
            val pos = frontier.removeAt(random.nextInt(frontier.size))
            if (!visited.add(pos.asLong())) continue

            val target = matchingTarget(level, pos, random, configuration) ?: continue
            level.setBlock(pos, target.state, 2)
            placed++

            horizontalDirections.forEach { (dx, dz) ->
                val neighbour = exposedFloor(level, pos.x + dx, pos.z + dz)
                if (neighbour != null && neighbour.asLong() !in visited) frontier.add(neighbour)
            }
        }

        return placed > 0
    }

    private fun exposedFloor(level: WorldGenLevel, x: Int, z: Int): BlockPos? {
        val y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1
        if (level.isOutsideBuildHeight(y)) return null

        val pos = BlockPos(x, y, z)
        if (!level.getBiome(pos).`is`(ModTags.Biomes.IS_ABYSSAL)) return null
        if (!level.getFluidState(pos.above()).`is`(FluidTags.WATER)) return null

        return pos.takeIf(level::ensureCanWrite)
    }

    private fun matchingTarget(
        level: WorldGenLevel,
        pos: BlockPos,
        random: RandomSource,
        configuration: OreConfiguration
    ): OreConfiguration.TargetBlockState? {
        val state = level.getBlockState(pos)
        return configuration.targetStates.firstOrNull { it.target.test(state, random) }
    }

    private fun isCommonOre(state: BlockState): Boolean {
        return state.`is`(BlockTags.IRON_ORES) ||
            state.`is`(BlockTags.COAL_ORES) ||
            state.`is`(BlockTags.COPPER_ORES)
    }

    private fun veinChance(state: BlockState): Float {
        return when {
            isCommonOre(state) -> COMMON_ORE_VEIN_CHANCE
            isPreciousOre(state) -> PRECIOUS_ORE_VEIN_CHANCE
            else -> MODDED_ORE_VEIN_CHANCE
        }
    }

    private fun isPreciousOre(state: BlockState): Boolean {
        return state.`is`(BlockTags.DIAMOND_ORES) ||
            state.`is`(BlockTags.EMERALD_ORES) ||
            state.`is`(BlockTags.GOLD_ORES) || state.`is`(BlockTags.LAPIS_ORES) ||
            state.`is`(BlockTags.REDSTONE_ORES)
    }

    private fun isRichRegion(seed: Long, pos: BlockPos): Boolean {
        val regionSize = RICH_REGION_CHUNKS * 16
        val regionX = Math.floorDiv(pos.x, regionSize)
        val regionZ = Math.floorDiv(pos.z, regionSize)
        var hash = seed xor
            (regionX.toLong() * -7046029254386353131L) xor
            (regionZ.toLong() * -4417276706812531889L)

        hash = (hash xor (hash ushr 30)) * -4658895280553007687L
        hash = (hash xor (hash ushr 27)) * -7723592293110705685L
        hash = hash xor (hash ushr 31)

        return Math.floorMod(hash, RICH_REGION_CHANCE.toLong()) == 0L
    }

    private fun reserveVein(level: WorldGenLevel, origin: BlockPos, limit: Int): Boolean {
        val budget = chunkBudget.get()
        val levelIdentity = System.identityHashCode(level)
        val chunkX = Math.floorDiv(origin.x, 16)
        val chunkZ = Math.floorDiv(origin.z, 16)

        if (budget.levelIdentity != levelIdentity || budget.chunkX != chunkX || budget.chunkZ != chunkZ) {
            budget.levelIdentity = levelIdentity
            budget.chunkX = chunkX
            budget.chunkZ = chunkZ
            budget.used = 0
        }

        if (budget.used >= limit) return false
        budget.used++

        return true
    }

    private class ChunkBudget(
        var levelIdentity: Int = 0,
        var chunkX: Int = Int.MIN_VALUE,
        var chunkZ: Int = Int.MIN_VALUE,
        var used: Int = 0
    )
}
