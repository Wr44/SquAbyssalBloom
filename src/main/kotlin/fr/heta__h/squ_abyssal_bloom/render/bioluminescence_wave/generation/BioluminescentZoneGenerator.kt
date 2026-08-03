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
import fr.heta__h.squ_abyssal_bloom.util.cache.AdaptiveWorkBudget
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import java.util.ArrayDeque
import kotlin.math.roundToInt

class BioluminescentZoneGenerator(
    private val anchor: BlockPos,
    initialCell: BioluminescentWaterCell,
    val preset: BioluminescentZonePreset,
    private val zoneSeed: Long,
    private val palette: BioluminescentPalette
) : AutoCloseable {
    companion object {
        const val TARGET_STEP_NANOS = 1_200_000L
        const val MAX_WORK_STEPS_PER_TICK = 4
        const val GENERATION_TIME_SLICE_NANOS = 4_000_000L
        const val UPLOAD_STEPS_PER_ADVANCE = 2
        const val MIN_MACRO_COMPONENT_RATIO = 0.95
        const val RADIUS_SALT = 0x7137449123EF65CDL
        const val MACRO_COVERAGE_SALT = 0x428A2F98D728AE22L
        const val VISIBLE_COVERAGE_SALT = 0x3956C25BF348B538L
    }

    val geodesicRadius = selectInt(preset.geodesicRadiusRange, RADIUS_SALT)
    val targetMacroCoverage = selectDouble(preset.macroCoverageRange, MACRO_COVERAGE_SALT)
    val targetVisibleCoverage = selectDouble(preset.visiblePixelCoverageRange, VISIBLE_COVERAGE_SALT)

    private val domainCollector = BioluminescentWaterDomainCollector(
        anchor,
        initialCell,
        geodesicRadius,
        preset.analysisMargin,
        preset.maxWaterCells
    )

    private val preparedTiles = ArrayList<BioluminescentZoneTile>()
    private val waterCellBudget = AdaptiveWorkBudget(15_000.0, 64, 4096)
    private val macroCellBudget = AdaptiveWorkBudget(8_000.0, 64, 8192)
    private val reactionInitBudget = AdaptiveWorkBudget(4_000.0, 128, 16384)
    private val reactionIterationBudget = AdaptiveWorkBudget(1_000_000.0, 1, 64)
    private val emissionSampleBudget = AdaptiveWorkBudget(6_500.0, 64, 8192)
    private val emissionCalibrationBudget = AdaptiveWorkBudget(50.0, 1024, 1_000_000)
    private val emissionFinalizationBudget = AdaptiveWorkBudget(2_000.0, 64, 16384)
    private val tilePixelRowBudget = AdaptiveWorkBudget(25_000.0, 4, BioluminescentZoneTile.TEXTURE_SIZE)
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
    private var currentTile: BioluminescentZoneTile? = null
    private var tileBudgetLimit = 0
    private var uploadCursor = 0
    private var completedResult: BioluminescentZoneGenerationResult? = null
    private var transferred = false
    private var priorityWorldX = anchor.x + 0.5
    private var priorityWorldZ = anchor.z + 0.5

    var stage = BioluminescentZoneGenerationStage.COLLECT_WATER_DOMAIN
        private set

    var failureReason: String? = null
        private set

    var cpuNanos: Long = 0L
        private set

    val isTerminal: Boolean
        get() = stage == BioluminescentZoneGenerationStage.READY ||
            stage == BioluminescentZoneGenerationStage.FAILED

    val renderableTiles: List<BioluminescentZoneTile>
        get() = preparedTiles.subList(0, uploadCursor.coerceAtMost(preparedTiles.size))

    val reservedTileCount: Int
        get() = tileBudgetLimit

    fun advance(
        level: ClientLevel,
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maxTileCount: Int,
        gameTime: Long,
        generationTimeSliceNanos: Long,
        maximumUploadSteps: Int,
        priorityWorldX: Double,
        priorityWorldZ: Double
    ) {
        if (isTerminal) return
        this.priorityWorldX = priorityWorldX
        this.priorityWorldZ = priorityWorldZ
        val startedAt = System.nanoTime()
        try {
            var workSteps = 0
            var uploadSteps = 0
            do {
                val stageBeforeStep = stage
                advanceCurrentStage(level, textureManager, identifierFactory, maxTileCount, gameTime)
                workSteps++
                if (stageBeforeStep == BioluminescentZoneGenerationStage.UPLOAD_TILES) uploadSteps++
            } while (!isTerminal &&
                workSteps < MAX_WORK_STEPS_PER_TICK &&
                System.nanoTime() - startedAt < generationTimeSliceNanos &&
                (stage != BioluminescentZoneGenerationStage.UPLOAD_TILES ||
                    uploadSteps < maximumUploadSteps.coerceAtLeast(1))
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
        maxTileCount: Int,
        gameTime: Long
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
                maxTileCount
            )
            BioluminescentZoneGenerationStage.UPLOAD_TILES -> uploadTiles(gameTime)
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
        if (!transferred) {
            preparedTiles.forEach(BioluminescentZoneTile::close)
            currentTile?.close()
        }
        preparedTiles.clear()
        currentTile = null
        completedResult = null
    }

    private fun collectDomain(level: ClientLevel) {
        val budget = waterCellBudget.suggestedUnits(TARGET_STEP_NANOS)
        val startedAt = System.nanoTime()
        domainCollector.advance(level, budget)
        waterCellBudget.recordSample(budget, System.nanoTime() - startedAt)
        if (!domainCollector.complete) return
        val createdDomain = domainCollector.build()
        check(createdDomain.localSize >= preset.minWaterCells) {
            "domaine aquatique local insuffisant: ${createdDomain.localSize}/${preset.minWaterCells}"
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
        val budget = macroCellBudget.suggestedUnits(TARGET_STEP_NANOS)
        val startedAt = System.nanoTime()
        builder.advance(budget)
        macroCellBudget.recordSample(budget, System.nanoTime() - startedAt)
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
        val reaction = reactionDiffusion ?: BioluminescentReactionDiffusion(
            checkNotNull(macroField),
            preset,
            zoneSeed
        ).also { reactionDiffusion = it }
        val budget = reactionInitBudget.suggestedUnits(TARGET_STEP_NANOS)
        val startedAt = System.nanoTime()
        val done = reaction.advanceInitialization(budget)
        reactionInitBudget.recordSample(budget, System.nanoTime() - startedAt)
        if (!done) return
        stage = BioluminescentZoneGenerationStage.RUN_REACTION_DIFFUSION
    }

    private fun runReactionDiffusion() {
        val reaction = checkNotNull(reactionDiffusion)
        val budget = reactionIterationBudget.suggestedUnits(TARGET_STEP_NANOS)
        val startedAt = System.nanoTime()
        reaction.advance(budget)
        reactionIterationBudget.recordSample(budget, System.nanoTime() - startedAt)
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
        val stageBeforeAdvance = generator.stage
        val sampleUnits = emissionSampleBudget.suggestedUnits(TARGET_STEP_NANOS)
        val calibrationUnits = emissionCalibrationBudget.suggestedUnits(TARGET_STEP_NANOS)
        val finalizationUnits = emissionFinalizationBudget.suggestedUnits(TARGET_STEP_NANOS)
        val startedAt = System.nanoTime()
        generator.advance(sampleUnits, calibrationUnits, finalizationUnits)
        val elapsedNanos = System.nanoTime() - startedAt
        when (stageBeforeAdvance) {
            BioluminescentEmissionGenerator.Stage.SAMPLE_PATTERNS ->
                emissionSampleBudget.recordSample(sampleUnits, elapsedNanos)
            BioluminescentEmissionGenerator.Stage.CALIBRATE_POROSITY ->
                emissionCalibrationBudget.recordSample(calibrationUnits, elapsedNanos)
            BioluminescentEmissionGenerator.Stage.FINALIZE_EMISSION ->
                emissionFinalizationBudget.recordSample(finalizationUnits, elapsedNanos)
            BioluminescentEmissionGenerator.Stage.COMPLETE -> Unit
        }
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
        maxTileCount: Int
    ) {
        check(tileOrigins.isNotEmpty()) { "aucune tuile lumineuse" }
        if (tileBudgetLimit < tileOrigins.size) {
            tileBudgetLimit = maxOf(tileBudgetLimit, minOf(tileOrigins.size, maxTileCount))
        }
        if (tileCursor >= tileBudgetLimit && currentTile == null && preparedTiles.isEmpty()) return
        var tile = currentTile
        while (tile == null && tileCursor < tileBudgetLimit) {
            val key = tileOrigins[tileCursor++]
            val originX = (key shr 32).toInt()
            val originZ = key.toInt()
            tile = BioluminescentZoneTile.prepare(
                textureManager,
                identifierFactory(),
                checkNotNull(emissionField),
                originX,
                originZ,
                zoneSeed
            )
        }
        currentTile = tile
        if (tile != null) {
            val budget = tilePixelRowBudget.suggestedUnits(TARGET_STEP_NANOS)
            val startedAt = System.nanoTime()
            val done = tile.fillPixelsStep(budget)
            tilePixelRowBudget.recordSample(budget, System.nanoTime() - startedAt)
            if (done) {
                preparedTiles.add(tile)
                currentTile = null
            }
            return
        }
        check(preparedTiles.isNotEmpty()) { "aucune texture preparee" }
        stage = BioluminescentZoneGenerationStage.UPLOAD_TILES
    }

    private fun uploadTiles(gameTime: Long) {
        if (uploadCursor >= preparedTiles.size) {
            finalizeResult()
            return
        }
        if (preparedTiles[uploadCursor].uploadStep(gameTime)) {
            uploadCursor++
            if (uploadCursor >= preparedTiles.size) finalizeResult()
        }
    }

    private fun finalizeResult() {
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
            origins.add(ModUtilities.horizontalPositionKey(originX, originZ))
        }
        return origins.sortedWith(
            compareBy<Long> { key ->
                val originX = (key shr 32).toInt()
                val originZ = key.toInt()
                ModUtilities.horizontalDistanceSqr(
                    originX + BioluminescentZoneTile.TILE_SIZE * 0.5,
                    originZ + BioluminescentZoneTile.TILE_SIZE * 0.5,
                    priorityWorldX,
                    priorityWorldZ
                )
            }.thenBy { key -> (key shr 32).toInt() }
                .thenBy { key -> key.toInt() }
        )
    }

    private fun validateMacroConnectivity(field: BioluminescentMacroField) {
        val skeletonCells = field.topology.skeleton.cellIndices
        check(skeletonCells.all(field::containsCell)) { "squelette hors support macroscopique" }
        val visited = BooleanArray(field.domain.size)
        val queue = ArrayDeque<Int>()
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
        check(connectedRatio >= MIN_MACRO_COMPONENT_RATIO) {
            "support macroscopique fragmente: ${(connectedRatio * 100.0).roundToInt()}% connecte"
        }
    }

    private fun fail(reason: String) {
        failureReason = reason
        preparedTiles.forEach(BioluminescentZoneTile::close)
        preparedTiles.clear()
        currentTile?.close()
        currentTile = null
        stage = BioluminescentZoneGenerationStage.FAILED
    }

    private fun selectInt(range: IntRange, salt: Long): Int {
        val mixed = RandomSupport.mixStafford13(zoneSeed xor salt)
        return range.first + Math.floorMod(mixed, (range.last - range.first + 1).toLong()).toInt()
    }

    private fun selectDouble(range: ClosedFloatingPointRange<Double>, salt: Long): Double {
        val unit = ModUtilities.stableUnitValue(RandomSupport.mixStafford13(zoneSeed xor salt))
        val centered = 0.10 + unit * 0.80
        return range.start + centered * (range.endInclusive - range.start)
    }

}
