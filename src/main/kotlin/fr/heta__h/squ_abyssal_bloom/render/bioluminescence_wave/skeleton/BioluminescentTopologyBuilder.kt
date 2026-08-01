package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.core.BioluminescentCore
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.core.BioluminescentCoreLobe
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.core.BioluminescentCoreShape
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sin

class BioluminescentTopologyBuilder(
    private val domain: BioluminescentWaterDomain,
    private val preset: BioluminescentZonePreset,
    private val zoneSeed: Long
) {
    enum class Stage {
        SELECT_CORES,
        COMPUTE_PATHS,
        COMPLETE
    }

    private val selectedIndices = ArrayList<Int>()
    private val shortestPaths = ArrayList<PathSearch>()
    private val random = XoroshiroRandomSource(RandomSupport.mixStafford13(zoneSeed xor TOPOLOGY_SALT))
    private val coastDirection = principalDirection()
    private val targetCoreCount = chooseTargetCoreCount()

    var stage: Stage = Stage.SELECT_CORES
        private set

    val selectedCoreCount: Int
        get() = selectedIndices.size

    fun advance() {
        when (stage) {
            Stage.SELECT_CORES -> advanceCoreSelection()
            Stage.COMPUTE_PATHS -> advancePathSearch()
            Stage.COMPLETE -> Unit
        }
    }

    fun build(): BioluminescentTopology {
        check(stage == Stage.COMPLETE)
        val cores = createCores()
        return BioluminescentTopology(
            cores,
            createSkeleton(),
            coastDirection.first,
            coastDirection.second
        )
    }

    private fun advanceCoreSelection() {
        var exhausted = false
        if (selectedIndices.isEmpty()) {
            selectedIndices.add(selectFirstCore())
        } else if (selectedIndices.size < targetCoreCount) {
            val distances = multiSourceDistances(selectedIndices)
            val next = selectFarthestCore(distances)
            if (next >= 0) {
                selectedIndices.add(next)
            } else {
                exhausted = true
            }
        }
        if (selectedIndices.size >= targetCoreCount || exhausted) {
            check(selectedIndices.size >= preset.coreCountRange.first) {
                "noyaux insuffisants: ${selectedIndices.size}/${preset.coreCountRange.first}"
            }
            stage = Stage.COMPUTE_PATHS
        }
    }

    private fun advancePathSearch() {
        if (shortestPaths.size < selectedIndices.size) {
            shortestPaths.add(shortestPathsFrom(selectedIndices[shortestPaths.size]))
        }
        if (shortestPaths.size == selectedIndices.size) stage = Stage.COMPLETE
    }

    private fun chooseTargetCoreCount(): Int {
        val capacity = (1 + domain.localSize / CELLS_PER_CORE)
            .coerceIn(preset.coreCountRange.first, preset.coreCountRange.last)
        val reduction = if (capacity > preset.coreCountRange.first && random.nextDouble() < CORE_REDUCTION_CHANCE) {
            1
        } else {
            0
        }
        return (capacity - reduction).coerceAtLeast(preset.coreCountRange.first)
    }

    private fun selectFirstCore(): Int {
        val searchDistance = maxOf(4, domain.geodesicRadius / 3)
        val artificialSafety = ceil(preset.artificialBoundaryFade).toInt() + 1
        val safeCandidateExists = domain.localCellIndices.any { index ->
            domain.geodesicDistanceFromAnchor[index] <= searchDistance &&
                domain.artificialBoundaryDistance[index] >= artificialSafety
        }
        var bestIndex = domain.anchorIndex
        var bestScore = Double.NEGATIVE_INFINITY
        for (index in domain.localCellIndices) {
            val distance = domain.geodesicDistanceFromAnchor[index]
            if (distance > searchDistance) continue
            if (safeCandidateExists && domain.artificialBoundaryDistance[index] < artificialSafety) continue
            val cell = domain.cells[index]
            val tie = stableUnitValue(
                RandomSupport.mixStafford13(
                    zoneSeed xor ModUtilities.bioluminescentCellKey(cell.waterPos.x, cell.waterPos.z)
                )
            )
            val score = domain.boundaryDepth[index] * 3.0 +
                minOf(domain.artificialBoundaryDistance[index], 8) * 0.55 -
                distance * 0.28 + tie
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun selectFarthestCore(distances: IntArray): Int {
        val minimumDistance = (preset.coreRadiusRange.start * MINIMUM_CORE_SPACING_FACTOR).toInt()
            .coerceAtLeast(3)
        val artificialSafety = ceil(preset.artificialBoundaryFade).toInt() + 1
        val safeCandidateExists = domain.localCellIndices.any { index ->
            distances[index] >= minimumDistance &&
                domain.artificialBoundaryDistance[index] >= artificialSafety
        }
        var bestIndex = -1
        var bestScore = Double.NEGATIVE_INFINITY
        var bestTie = Long.MIN_VALUE
        for (index in domain.localCellIndices) {
            if (index in selectedIndices || distances[index] < minimumDistance) continue
            if (safeCandidateExists && domain.artificialBoundaryDistance[index] < artificialSafety) continue
            val position = domain.cells[index].waterPos
            val interior = 0.62 + ModUtilities.smooth(
                0.0,
                preset.coreRadiusRange.start * 0.75,
                domain.boundaryDepth[index].toDouble()
            ) * 0.38
            val projection = abs(
                (position.x - domain.cells[domain.anchorIndex].waterPos.x) * coastDirection.first +
                    (position.z - domain.cells[domain.anchorIndex].waterPos.z) * coastDirection.second
            )
            val coastSpread = 0.92 + 0.16 * (projection / domain.geodesicRadius).coerceIn(0.0, 1.0)
            val artificialInterior = 0.30 + ModUtilities.smooth(
                0.0,
                artificialSafety.toDouble(),
                domain.artificialBoundaryDistance[index].toDouble()
            ) * 0.70
            val score = distances[index] * interior * coastSpread * artificialInterior
            val tie = RandomSupport.mixStafford13(
                zoneSeed xor CORE_SELECTION_SALT xor
                    ModUtilities.bioluminescentCellKey(position.x, position.z)
            )
            if (score > bestScore || score == bestScore && tie > bestTie) {
                bestIndex = index
                bestScore = score
                bestTie = tie
            }
        }
        return bestIndex
    }

    private fun createCores(): List<BioluminescentCore> {
        val baseAngle = atan2(coastDirection.second, coastDirection.first)
        val shapeOffset = Math.floorMod(
            RandomSupport.mixStafford13(zoneSeed xor CORE_SHAPE_SALT),
            BioluminescentCoreShape.entries.size.toLong()
        ).toInt()
        return selectedIndices.mapIndexed { coreIndex, cellIndex ->
            val coreRandom = XoroshiroRandomSource(
                RandomSupport.mixStafford13(zoneSeed xor CORE_SHAPE_SALT xor coreIndex.toLong())
            )
            val shape = BioluminescentCoreShape.entries[
                (shapeOffset + coreIndex) % BioluminescentCoreShape.entries.size
            ]
            val baseRadius = randomBetween(coreRandom, preset.coreRadiusRange)
            val anisotropy = 0.62 + coreRandom.nextDouble() * 0.55
            val radiusX = when (shape) {
                BioluminescentCoreShape.ELONGATED -> baseRadius * (1.28 + coreRandom.nextDouble() * 0.42)
                BioluminescentCoreShape.CRESCENT -> baseRadius * 1.12
                else -> baseRadius
            }
            val radiusZ = baseRadius * anisotropy
            val lobes = if (shape == BioluminescentCoreShape.CLUSTERED) {
                List(3 + coreRandom.nextInt(3)) {
                    BioluminescentCoreLobe(
                        (coreRandom.nextDouble() - 0.5) * radiusX * 0.95,
                        (coreRandom.nextDouble() - 0.5) * radiusZ * 0.95,
                        radiusX * (0.38 + coreRandom.nextDouble() * 0.34),
                        radiusZ * (0.42 + coreRandom.nextDouble() * 0.38)
                    )
                }
            } else {
                emptyList()
            }
            val position = domain.cells[cellIndex].waterPos
            BioluminescentCore(
                cellIndex = cellIndex,
                worldX = position.x + 0.5,
                worldZ = position.z + 0.5,
                radiusX = radiusX,
                radiusZ = radiusZ,
                rotationRadians = baseAngle + (coreRandom.nextDouble() - 0.5) * 1.15,
                superellipseExponent = 3.0 + coreRandom.nextDouble() * 6.0,
                weight = 0.88 + coreRandom.nextDouble() * 0.24,
                falloff = 1.55 + coreRandom.nextDouble() * 0.85,
                shape = shape,
                lobes = lobes,
                crescentOffset = radiusX * (0.22 + coreRandom.nextDouble() * 0.22)
            )
        }
    }

    private fun createSkeleton(): BioluminescentSkeleton {
        val pathPairs = minimumSpanningPairs().toMutableList()
        optionalLoopPair(pathPairs)?.let(pathPairs::add)
        val sampler = BioluminescentNoiseSampler(zoneSeed)
        val paths = pathPairs.map { pair ->
            BioluminescentSkeletonPath(pair.first, pair.second, reconstructPath(pair.first, pair.second))
        }
        val widths = FloatArray(domain.size)
        val skeletonCells = LinkedHashSet<Int>()
        var totalLength = 0
        for ((pathIndex, path) in paths.withIndex()) {
            totalLength += path.cellIndices.size - 1
            val widthRandom = XoroshiroRandomSource(
                RandomSupport.mixStafford13(zoneSeed xor PATH_WIDTH_SALT xor pathIndex.toLong())
            )
            val baseWidth = randomBetween(widthRandom, preset.connectionWidthRange)
            path.cellIndices.forEachIndexed { index, cellIndex ->
                skeletonCells.add(cellIndex)
                val progress = if (path.cellIndices.size <= 1) 0.0 else {
                    index.toDouble() / (path.cellIndices.size - 1)
                }
                val middleTaper = 1.0 - sin(progress * PI) * 0.34
                val position = domain.cells[cellIndex].waterPos
                val widthNoise = sampler.sampleLarge(
                    position.x * PATH_WIDTH_NOISE_SCALE,
                    position.z * PATH_WIDTH_NOISE_SCALE
                )
                val width = baseWidth * middleTaper * (0.78 + widthNoise * 0.44)
                widths[cellIndex] = maxOf(widths[cellIndex], width.toFloat())
            }
        }
        if (skeletonCells.isEmpty()) {
            skeletonCells.add(selectedIndices.first())
            widths[selectedIndices.first()] = preset.connectionWidthRange.start.toFloat()
        }
        val nearest = nearestSkeletonData(skeletonCells, widths)
        return BioluminescentSkeleton(
            paths,
            skeletonCells.toIntArray(),
            totalLength,
            nearest.first,
            nearest.second,
            nearest.third
        )
    }

    private fun minimumSpanningPairs(): List<Pair<Int, Int>> {
        if (selectedIndices.size <= 1) return emptyList()
        val inTree = BooleanArray(selectedIndices.size)
        val result = ArrayList<Pair<Int, Int>>()
        inTree[0] = true
        repeat(selectedIndices.size - 1) {
            var bestSource = -1
            var bestDestination = -1
            var bestDistance = Int.MAX_VALUE
            for (source in selectedIndices.indices) {
                if (!inTree[source]) continue
                for (destination in selectedIndices.indices) {
                    if (inTree[destination]) continue
                    val distance = shortestPaths[source].distances[selectedIndices[destination]]
                    if (distance >= 0 && distance < bestDistance) {
                        bestSource = source
                        bestDestination = destination
                        bestDistance = distance
                    }
                }
            }
            check(bestSource >= 0 && bestDestination >= 0) { "noyaux geodesiques non raccordables" }
            result.add(bestSource to bestDestination)
            inTree[bestDestination] = true
        }
        return result
    }

    private fun optionalLoopPair(existing: List<Pair<Int, Int>>): Pair<Int, Int>? {
        if (selectedIndices.size < 4 || stableUnitValue(
                RandomSupport.mixStafford13(zoneSeed xor OPTIONAL_LOOP_SALT)
            ) > OPTIONAL_LOOP_CHANCE
        ) return null
        val used = existing.flatMap { pair -> listOf(pair, pair.second to pair.first) }.toHashSet()
        return selectedIndices.indices.asSequence()
            .flatMap { source ->
                (source + 1 until selectedIndices.size).asSequence().map { destination -> source to destination }
            }
            .filterNot(used::contains)
            .filter { pair -> shortestPaths[pair.first].distances[selectedIndices[pair.second]] > 0 }
            .minByOrNull { pair -> shortestPaths[pair.first].distances[selectedIndices[pair.second]] }
    }

    private fun reconstructPath(sourceCore: Int, destinationCore: Int): IntArray {
        val sourceCell = selectedIndices[sourceCore]
        var cursor = selectedIndices[destinationCore]
        val reversed = ArrayList<Int>()
        while (cursor != sourceCell) {
            reversed.add(cursor)
            cursor = shortestPaths[sourceCore].parents[cursor]
            check(cursor >= 0) { "chemin geodesique incomplet" }
        }
        reversed.add(sourceCell)
        reversed.reverse()
        return reversed.toIntArray()
    }

    private fun shortestPathsFrom(source: Int): PathSearch {
        val distances = IntArray(domain.size) { -1 }
        val parents = IntArray(domain.size) { -1 }
        val queue = java.util.PriorityQueue<PathQueueEntry>(compareBy(PathQueueEntry::distance))
        distances[source] = 0
        queue.add(PathQueueEntry(source, 0))
        while (queue.isNotEmpty()) {
            val entry = queue.remove()
            val index = entry.cellIndex
            if (entry.distance != distances[index]) continue
            domain.forEachNeighbor(index) { neighbor ->
                val distance = distances[index] + 1 + artificialBoundaryPenalty(neighbor)
                if (distances[neighbor] >= 0 && distances[neighbor] <= distance) return@forEachNeighbor
                distances[neighbor] = distance
                parents[neighbor] = index
                queue.add(PathQueueEntry(neighbor, distance))
            }
        }
        return PathSearch(distances, parents)
    }

    private fun artificialBoundaryPenalty(cellIndex: Int): Int {
        val safety = ceil(preset.artificialBoundaryFade).toInt() + 1
        return (safety - domain.artificialBoundaryDistance[cellIndex]).coerceAtLeast(0) *
            ARTIFICIAL_BOUNDARY_PATH_PENALTY
    }

    private fun multiSourceDistances(sources: Collection<Int>): IntArray {
        val distances = IntArray(domain.size) { -1 }
        val queue = java.util.ArrayDeque<Int>()
        for (source in sources) {
            distances[source] = 0
            queue.addLast(source)
        }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            domain.forEachNeighbor(index) { neighbor ->
                if (distances[neighbor] >= 0) return@forEachNeighbor
                distances[neighbor] = distances[index] + 1
                queue.addLast(neighbor)
            }
        }
        return distances
    }

    private fun nearestSkeletonData(
        skeletonCells: Set<Int>,
        widths: FloatArray
    ): Triple<IntArray, IntArray, FloatArray> {
        val distances = IntArray(domain.size) { -1 }
        val nearestX = IntArray(domain.size)
        val nearestZ = IntArray(domain.size)
        val nearestWidth = FloatArray(domain.size)
        val queue = java.util.ArrayDeque<Int>()
        for (index in skeletonCells) {
            val position = domain.cells[index].waterPos
            distances[index] = 0
            nearestX[index] = position.x
            nearestZ[index] = position.z
            nearestWidth[index] = widths[index]
            queue.addLast(index)
        }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            domain.forEachNeighbor(index) { neighbor ->
                if (distances[neighbor] >= 0) return@forEachNeighbor
                distances[neighbor] = distances[index] + 1
                nearestX[neighbor] = nearestX[index]
                nearestZ[neighbor] = nearestZ[index]
                nearestWidth[neighbor] = nearestWidth[index]
                queue.addLast(neighbor)
            }
        }
        return Triple(nearestX, nearestZ, nearestWidth)
    }

    private fun principalDirection(): Pair<Double, Double> {
        val meanX = domain.localCellIndices.sumOf { index ->
            domain.cells[index].waterPos.x.toDouble()
        } / domain.localSize
        val meanZ = domain.localCellIndices.sumOf { index ->
            domain.cells[index].waterPos.z.toDouble()
        } / domain.localSize
        var covarianceX = 0.0
        var covarianceZ = 0.0
        var covarianceXZ = 0.0
        for (index in domain.localCellIndices) {
            val cell = domain.cells[index]
            val deltaX = cell.waterPos.x - meanX
            val deltaZ = cell.waterPos.z - meanZ
            covarianceX += deltaX * deltaX
            covarianceZ += deltaZ * deltaZ
            covarianceXZ += deltaX * deltaZ
        }
        val anisotropy = hypot(covarianceX - covarianceZ, 2.0 * covarianceXZ) /
            (covarianceX + covarianceZ).coerceAtLeast(1.0)
        val angle = if (anisotropy < MINIMUM_DIRECTION_ANISOTROPY) {
            stableUnitValue(RandomSupport.mixStafford13(zoneSeed xor DIRECTION_SALT)) * PI * 2.0
        } else {
            0.5 * atan2(2.0 * covarianceXZ, covarianceX - covarianceZ)
        }
        return cos(angle) to sin(angle)
    }

    private fun randomBetween(
        source: XoroshiroRandomSource,
        range: ClosedFloatingPointRange<Double>
    ): Double {
        return range.start + source.nextDouble() * (range.endInclusive - range.start)
    }

    private fun stableUnitValue(value: Long): Double {
        return ((value ushr 40) and 0xFFFFFFL).toDouble() / 0xFFFFFFL.toDouble()
    }

    private data class PathSearch(val distances: IntArray, val parents: IntArray)

    private data class PathQueueEntry(val cellIndex: Int, val distance: Int)

    private companion object {
        const val CELLS_PER_CORE = 360
        const val CORE_REDUCTION_CHANCE = 0.16
        const val ARTIFICIAL_BOUNDARY_PATH_PENALTY = 6
        const val MINIMUM_CORE_SPACING_FACTOR = 0.72
        const val MINIMUM_DIRECTION_ANISOTROPY = 0.08
        const val OPTIONAL_LOOP_CHANCE = 0.48
        const val PATH_WIDTH_NOISE_SCALE = 0.32
        const val TOPOLOGY_SALT = 0x6A09E667F3BCC909L
        const val CORE_SELECTION_SALT = 0x510E527FADE682D1L
        const val CORE_SHAPE_SALT = 0x1F83D9ABFB41BD6BL
        const val PATH_WIDTH_SALT = 0x5BE0CD19137E2179L
        const val OPTIONAL_LOOP_SALT = 0x243F6A8885A308D3L
        const val DIRECTION_SALT = 0x13198A2E03707344L
    }
}
