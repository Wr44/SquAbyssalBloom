package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomainCollector
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionGenerator
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentMacroField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentMacroFieldBuilder
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentReactionDiffusion
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopology
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopologyBuilder
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.roundToInt

class BioluminescentZoneGenerator(
    anchor: BlockPos,
    initialCell: BioluminescentWaterCell,
    val preset: BioluminescentZonePreset,
    private val zoneSeed: Long,
    private val palette: BioluminescentPalette
) : AutoCloseable {
    val geodesicRadius = selectInt(preset.geodesicRadiusRange, RADIUS_SALT)
    val targetMacroCoverage = selectDouble(preset.macroCoverageRange, MACRO_COVERAGE_SALT)
    val targetVisibleCoverage = selectDouble(preset.visiblePixelCoverageRange, VISIBLE_COVERAGE_SALT)

    private val domainCollector = BioluminescentWaterDomainCollector(
        anchor,
        initialCell,
        geodesicRadius,
        preset.analysisMargin,
        preset.maximumWaterCells
    )
    private val preparedTiles = ArrayList<BioluminescentZoneTile>()

    private var domain: BioluminescentWaterDomain? = null
    private var topologyBuilder: BioluminescentTopologyBuilder? = null
    private var topology: BioluminescentTopology? = null
    private var macroBuilder: BioluminescentMacroFieldBuilder? = null
    private var macroField: BioluminescentMacroField? = null
    private var reactionDiffusion: BioluminescentReactionDiffusion? = null
    private var emissionGenerator: BioluminescentEmissionGenerator? = null
    private var emissionField: BioluminescentEmissionField? = null
    private var tileOrigins: List<Long> = emptyList()
    private var tileCursor = 0
    private var uploadCursor = 0
    private var completedResult: BioluminescentZoneGenerationResult? = null
    private var transferred = false

    var stage = BioluminescentZoneGenerationStage.COLLECT_WATER_DOMAIN
        private set

    var failureReason: String? = null
        private set

    var cpuNanos: Long = 0L
        private set

    val isTerminal: Boolean
        get() = stage == BioluminescentZoneGenerationStage.READY ||
            stage == BioluminescentZoneGenerationStage.FAILED

    fun advance(
        level: ClientLevel,
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maximumTileCount: Int
    ) {
        if (isTerminal) return
        val startedAt = System.nanoTime()
        try {
            var workSteps = 0
            do {
                advanceCurrentStage(level, textureManager, identifierFactory, maximumTileCount)
                workSteps++
            } while (!isTerminal &&
                workSteps < MAXIMUM_WORK_STEPS_PER_TICK &&
                System.nanoTime() - startedAt < GENERATION_TIME_SLICE_NANOS
            )
        } catch (exception: RuntimeException) {
            fail(exception.message ?: exception.javaClass.simpleName)
        } finally {
            cpuNanos += System.nanoTime() - startedAt
        }
    }

    private fun advanceCurrentStage(
        level: ClientLevel,
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maximumTileCount: Int
    ) {
        when (stage) {
            BioluminescentZoneGenerationStage.COLLECT_WATER_DOMAIN -> collectDomain(level)
            BioluminescentZoneGenerationStage.SELECT_CORES -> selectCores()
            BioluminescentZoneGenerationStage.BUILD_GEODESIC_DISTANCES -> buildDistances()
            BioluminescentZoneGenerationStage.BUILD_SKELETON -> buildSkeleton()
            BioluminescentZoneGenerationStage.BUILD_MACRO_FIELD -> buildMacroField()
            BioluminescentZoneGenerationStage.INITIALIZE_REACTION_DIFFUSION -> initializeReactionDiffusion()
            BioluminescentZoneGenerationStage.RUN_REACTION_DIFFUSION -> runReactionDiffusion()
            BioluminescentZoneGenerationStage.GENERATE_EMISSION,
            BioluminescentZoneGenerationStage.CALIBRATE_VISIBLE_COVERAGE -> generateEmission()
            BioluminescentZoneGenerationStage.CREATE_TILES -> createTile(
                textureManager,
                identifierFactory,
                maximumTileCount
            )
            BioluminescentZoneGenerationStage.UPLOAD_TILES -> uploadTiles()
            BioluminescentZoneGenerationStage.READY,
            BioluminescentZoneGenerationStage.FAILED -> Unit
        }
    }

    fun snapshot(): BioluminescentZoneGenerationSnapshot {
        val reaction = reactionDiffusion
        return BioluminescentZoneGenerationSnapshot(
            stage,
            domain?.size ?: domainCollector.cellCount,
            geodesicRadius,
            topologyBuilder?.selectedCoreCount ?: topology?.cores?.size ?: 0,
            reaction?.completedIterations ?: 0,
            reaction?.targetIterations ?: preset.reactionDiffusionIterations,
            preparedTiles.size,
            preparedTiles.count(BioluminescentZoneTile::uploaded),
            cpuNanos,
            failureReason
        )
    }

    fun takeCompletedResult(): BioluminescentZoneGenerationResult? {
        if (stage != BioluminescentZoneGenerationStage.READY || transferred) return null
        transferred = true
        return completedResult?.copy(cpuNanos = cpuNanos)
    }

    override fun close() {
        if (!transferred) preparedTiles.forEach(BioluminescentZoneTile::close)
        preparedTiles.clear()
        completedResult = null
    }

    private fun collectDomain(level: ClientLevel) {
        domainCollector.advance(level, WATER_CELL_BUDGET)
        if (!domainCollector.complete) return
        val createdDomain = domainCollector.build()
        check(createdDomain.localSize >= preset.minimumWaterCells) {
            "domaine aquatique local insuffisant: ${createdDomain.localSize}/${preset.minimumWaterCells}"
        }
        domain = createdDomain
        topologyBuilder = BioluminescentTopologyBuilder(createdDomain, preset, zoneSeed)
        stage = BioluminescentZoneGenerationStage.SELECT_CORES
    }

    private fun selectCores() {
        val builder = checkNotNull(topologyBuilder)
        builder.advance()
        if (builder.stage == BioluminescentTopologyBuilder.Stage.COMPUTE_PATHS) {
            stage = BioluminescentZoneGenerationStage.BUILD_GEODESIC_DISTANCES
        }
    }

    private fun buildDistances() {
        val builder = checkNotNull(topologyBuilder)
        builder.advance()
        if (builder.stage == BioluminescentTopologyBuilder.Stage.COMPLETE) {
            stage = BioluminescentZoneGenerationStage.BUILD_SKELETON
        }
    }

    private fun buildSkeleton() {
        val createdTopology = checkNotNull(topologyBuilder).build()
        check(createdTopology.cores.size in preset.coreCountRange) {
            "nombre de noyaux invalide: ${createdTopology.cores.size}"
        }
        check(createdTopology.skeleton.connectionCount >= createdTopology.cores.size - 1) {
            "squelette geodesique incomplet"
        }
        topology = createdTopology
        macroBuilder = BioluminescentMacroFieldBuilder(
            checkNotNull(domain),
            createdTopology,
            preset,
            zoneSeed,
            targetMacroCoverage
        )
        stage = BioluminescentZoneGenerationStage.BUILD_MACRO_FIELD
    }

    private fun buildMacroField() {
        val builder = checkNotNull(macroBuilder)
        builder.advance(MACRO_CELL_BUDGET)
        if (!builder.complete) return
        val createdField = builder.build()
        check(createdField.achievedCoverage in preset.macroCoverageRange) {
            "couverture macro hors preset: ${(createdField.achievedCoverage * 100.0).roundToInt()}%"
        }
        validateMacroConnectivity(createdField)
        macroField = createdField
        stage = BioluminescentZoneGenerationStage.INITIALIZE_REACTION_DIFFUSION
    }

    private fun initializeReactionDiffusion() {
        reactionDiffusion = BioluminescentReactionDiffusion(
            checkNotNull(macroField),
            preset,
            zoneSeed
        )
        stage = BioluminescentZoneGenerationStage.RUN_REACTION_DIFFUSION
    }

    private fun runReactionDiffusion() {
        val reaction = checkNotNull(reactionDiffusion)
        reaction.advance(REACTION_ITERATION_BUDGET)
        if (!reaction.complete) return
        emissionGenerator = BioluminescentEmissionGenerator(
            checkNotNull(macroField),
            reaction,
            preset,
            palette,
            zoneSeed,
            targetVisibleCoverage
        )
        stage = BioluminescentZoneGenerationStage.GENERATE_EMISSION
    }

    private fun generateEmission() {
        val generator = checkNotNull(emissionGenerator)
        generator.advance(
            EMISSION_SAMPLE_BUDGET,
            EMISSION_CALIBRATION_BUDGET,
            EMISSION_FINALIZATION_BUDGET
        )
        stage = when (generator.stage) {
            BioluminescentEmissionGenerator.Stage.SAMPLE_PATTERNS,
            BioluminescentEmissionGenerator.Stage.FINALIZE_EMISSION ->
                BioluminescentZoneGenerationStage.GENERATE_EMISSION
            BioluminescentEmissionGenerator.Stage.CALIBRATE_POROSITY ->
                BioluminescentZoneGenerationStage.CALIBRATE_VISIBLE_COVERAGE
            BioluminescentEmissionGenerator.Stage.COMPLETE -> {
                val createdEmission = generator.build()
                emissionField = createdEmission
                tileOrigins = collectTileOrigins(createdEmission)
                BioluminescentZoneGenerationStage.CREATE_TILES
            }
        }
    }

    private fun createTile(
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maximumTileCount: Int
    ) {
        check(tileOrigins.isNotEmpty()) { "aucune tuile lumineuse" }
        check(tileOrigins.size <= maximumTileCount) {
            "budget de tuiles insuffisant: ${tileOrigins.size}/$maximumTileCount"
        }
        if (tileCursor < tileOrigins.size) {
            val end = minOf(tileOrigins.size, tileCursor + TILE_PREPARATION_BUDGET)
            while (tileCursor < end) {
                val key = tileOrigins[tileCursor++]
                val originX = (key shr 32).toInt()
                val originZ = key.toInt()
                BioluminescentZoneTile.prepare(
                    textureManager,
                    identifierFactory(),
                    checkNotNull(emissionField),
                    originX,
                    originZ,
                    zoneSeed
                )?.let(preparedTiles::add)
            }
        }
        if (tileCursor >= tileOrigins.size) {
            check(preparedTiles.isNotEmpty()) { "aucune texture preparee" }
            stage = BioluminescentZoneGenerationStage.UPLOAD_TILES
        }
    }

    private fun uploadTiles() {
        val end = minOf(preparedTiles.size, uploadCursor + TILE_UPLOAD_BUDGET)
        for (index in uploadCursor until end) preparedTiles[index].upload()
        uploadCursor = end
        if (uploadCursor < preparedTiles.size) return

        val reaction = checkNotNull(reactionDiffusion)
        completedResult = BioluminescentZoneGenerationResult(
            checkNotNull(domain),
            checkNotNull(topology),
            checkNotNull(macroField),
            checkNotNull(emissionField),
            BioluminescentReactionDiffusionStats(
                reaction.width,
                reaction.length,
                reaction.feed,
                reaction.kill,
                reaction.completedIterations
            ),
            preparedTiles.toList(),
            cpuNanos,
            preparedTiles.sumOf(BioluminescentZoneTile::uploadCount)
        )
        stage = BioluminescentZoneGenerationStage.READY
    }

    private fun collectTileOrigins(emission: BioluminescentEmissionField): List<Long> {
        val origins = LinkedHashSet<Long>()
        for (cellIndex in emission.domain.cells.indices) {
            if (!emission.hasLuminousCell(cellIndex)) continue
            val position = emission.domain.cells[cellIndex].waterPos
            val originX = Math.floorDiv(position.x, BioluminescentZoneTile.TILE_SIZE) *
                BioluminescentZoneTile.TILE_SIZE
            val originZ = Math.floorDiv(position.z, BioluminescentZoneTile.TILE_SIZE) *
                BioluminescentZoneTile.TILE_SIZE
            origins.add(ModUtilities.bioluminescentCellKey(originX, originZ))
        }
        return origins.sortedWith(compareBy({ (it shr 32).toInt() }, { it.toInt() }))
    }

    private fun validateMacroConnectivity(field: BioluminescentMacroField) {
        val skeletonCells = field.topology.skeleton.cellIndices
        check(skeletonCells.all(field::containsCell)) { "squelette hors support macroscopique" }
        val visited = BooleanArray(field.domain.size)
        val queue = java.util.ArrayDeque<Int>()
        val start = skeletonCells.first()
        visited[start] = true
        queue.addLast(start)
        var connectedCells = 0
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            connectedCells++
            field.domain.forEachNeighbor(index) { neighbor ->
                if (visited[neighbor] || !field.containsCell(neighbor)) return@forEachNeighbor
                visited[neighbor] = true
                queue.addLast(neighbor)
            }
        }
        val connectedRatio = connectedCells.toDouble() / field.macroCellCount.coerceAtLeast(1)
        check(connectedRatio >= MINIMUM_MACRO_COMPONENT_RATIO) {
            "support macroscopique fragmente: ${(connectedRatio * 100.0).roundToInt()}% connecte"
        }
    }

    private fun fail(reason: String) {
        failureReason = reason
        preparedTiles.forEach(BioluminescentZoneTile::close)
        preparedTiles.clear()
        stage = BioluminescentZoneGenerationStage.FAILED
    }

    private fun selectInt(range: IntRange, salt: Long): Int {
        val mixed = RandomSupport.mixStafford13(zoneSeed xor salt)
        return range.first + Math.floorMod(mixed, (range.last - range.first + 1).toLong()).toInt()
    }

    private fun selectDouble(range: ClosedFloatingPointRange<Double>, salt: Long): Double {
        val unit = ((RandomSupport.mixStafford13(zoneSeed xor salt) ushr 40) and 0xFFFFFFL)
            .toDouble() / 0xFFFFFFL.toDouble()
        val centered = 0.10 + unit * 0.80
        return range.start + centered * (range.endInclusive - range.start)
    }

    private companion object {
        const val WATER_CELL_BUDGET = 768
        const val MACRO_CELL_BUDGET = 2048
        const val REACTION_ITERATION_BUDGET = 16
        const val EMISSION_SAMPLE_BUDGET = 8192
        const val EMISSION_CALIBRATION_BUDGET = 65536
        const val EMISSION_FINALIZATION_BUDGET = 8192
        const val TILE_PREPARATION_BUDGET = 1
        const val TILE_UPLOAD_BUDGET = 1
        const val MAXIMUM_WORK_STEPS_PER_TICK = 4
        const val GENERATION_TIME_SLICE_NANOS = 8_000_000L
        const val MINIMUM_MACRO_COMPONENT_RATIO = 0.95
        const val RADIUS_SALT = 0x7137449123EF65CDL
        const val MACRO_COVERAGE_SALT = 0x428A2F98D728AE22L
        const val VISIBLE_COVERAGE_SALT = 0x3956C25BF348B538L
    }
}
