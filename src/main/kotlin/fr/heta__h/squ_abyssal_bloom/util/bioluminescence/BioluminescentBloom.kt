package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB

class BioluminescentBloom(
    val anchor: BlockPos,
    val widthInBlocks: Int,
    val lengthInBlocks: Int,
    val shape: BioluminescentBloomShape,
    val rotationRadians: Double?,
    val state: BioluminescentBloomState,
    val texture: BioluminescentTexture,
    val renderType: RenderType,
    initialWaterCells: List<BioluminescentWaterCell>
) : AutoCloseable {
    val originX = anchor.x - widthInBlocks / 2
    val originZ = anchor.z - lengthInBlocks / 2
    val cellCount = widthInBlocks * lengthInBlocks
    val bounds = AABB(
        originX.toDouble(),
        (anchor.y - MASK_VERTICAL_SEARCH_RADIUS).toDouble(),
        originZ.toDouble(),
        (originX + widthInBlocks).toDouble(),
        (anchor.y + MASK_VERTICAL_SEARCH_RADIUS + 2).toDouble(),
        (originZ + lengthInBlocks).toDouble()
    )

    var validWaterCellCount = 0
        private set

    private var surfaceHeights = DoubleArray(cellCount) { Double.NaN }
    private var stagingSurfaceHeights = DoubleArray(cellCount) { Double.NaN }
    private val ownedCells = BooleanArray(cellCount)
    private var stagingValidCellCount = 0
    private var maskCursor = 0
    private var maskRefreshInProgress = true
    private var completedMaskOnce = false
    private var lastMaskRefreshTick: Long? = null
    private var lastTextureEvaluationTick: Long? = null
    private var forcedFadeStartedAt: Long? = null
    private var cellOwnershipInvalidated = false
    private var closed = false

    init {
        require(widthInBlocks > 0)
        require(lengthInBlocks > 0)
        copyInitialWaterCells(initialWaterCells)
    }

    fun surfaceYAt(index: Int): Double = surfaceHeights[index]

    fun hasWaterSurfaceAt(index: Int): Boolean = !surfaceHeights[index].isNaN()

    fun ownsCell(index: Int): Boolean = ownedCells[index]

    fun resetCellOwnership() {
        ownedCells.fill(false)
    }

    fun claimCell(index: Int) {
        ownedCells[index] = true
    }

    fun centerInfluenceAt(index: Int): Double {
        val normalizedX = ((index % widthInBlocks) + 0.5) / widthInBlocks * 2.0 - 1.0
        val normalizedZ = ((index / widthInBlocks) + 0.5) / lengthInBlocks * 2.0 - 1.0
        val normalizedDistance = kotlin.math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ) /
            kotlin.math.sqrt(2.0)
        return (1.0 - normalizedDistance).coerceIn(0.0, 1.0)
    }

    fun consumeCellOwnershipInvalidation(): Boolean {
        val invalidated = cellOwnershipInvalidated
        cellOwnershipInvalidated = false
        return invalidated
    }

    fun hasRenderableMask(): Boolean = validWaterCellCount > 0

    fun scheduleMaskRefresh(gameTime: Long) {
        if (closed || maskRefreshInProgress) return
        val lastRefresh = lastMaskRefreshTick
        val refreshDue = lastRefresh == null ||
            gameTime < lastRefresh ||
            gameTime - lastRefresh >= MASK_REFRESH_INTERVAL_TICKS
        if (!refreshDue) return

        stagingSurfaceHeights.fill(Double.NaN)
        stagingValidCellCount = 0
        maskCursor = 0
        maskRefreshInProgress = true
    }

    fun processMaskRefresh(level: ClientLevel, gameTime: Long, columnBudget: Int): Int {
        if (closed || !maskRefreshInProgress || columnBudget <= 0) return 0

        val endIndex = minOf(cellCount, maskCursor + columnBudget)
        val startY = minOf(level.maxY - 1, anchor.y + MASK_VERTICAL_SEARCH_RADIUS)
        val minimumY = maxOf(level.minY, anchor.y - MASK_VERTICAL_SEARCH_RADIUS)

        for (index in maskCursor until endIndex) {
            if (!completedMaskOnce && !stagingSurfaceHeights[index].isNaN()) continue

            val worldX = originX + index % widthInBlocks
            val worldZ = originZ + index / widthInBlocks
            if (!level.hasChunk(worldX shr 4, worldZ shr 4)) continue

            val waterBlock = ModUtilities.findWaterBlockBelow(
                level = level,
                x = worldX,
                z = worldZ,
                startY = startY,
                minimumY = minimumY
            ) ?: continue
            val topWaterBlock = ModUtilities.findTopWaterBlock(level, waterBlock) ?: continue
            if (!ModUtilities.isRenderableWaterSurface(level, topWaterBlock)) continue

            stagingSurfaceHeights[index] = ModUtilities.getFluidSurfaceHeight(level, topWaterBlock)
            stagingValidCellCount++
        }

        val processed = endIndex - maskCursor
        maskCursor = endIndex

        if (maskCursor >= cellCount) {
            var validityChanged = !completedMaskOnce
            if (!validityChanged) {
                for (index in 0 until cellCount) {
                    if (surfaceHeights[index].isNaN() != stagingSurfaceHeights[index].isNaN()) {
                        validityChanged = true
                        break
                    }
                }
            }
            val previous = surfaceHeights
            surfaceHeights = stagingSurfaceHeights
            stagingSurfaceHeights = previous
            validWaterCellCount = stagingValidCellCount
            stagingValidCellCount = 0
            maskCursor = 0
            maskRefreshInProgress = false
            completedMaskOnce = true
            lastMaskRefreshTick = gameTime
            if (validityChanged) cellOwnershipInvalidated = true

            if (validWaterCellCount in 1 until MINIMUM_WATER_CELLS && forcedFadeStartedAt == null) {
                forcedFadeStartedAt = gameTime
            }
        }

        return processed
    }

    fun createTextureUpdateRequest(
        zone: BioluminescentBloomZone,
        gameTime: Long,
        withinUpdateDistance: Boolean
    ): BioluminescentTextureUpdateRequest? {
        if (closed || !withinUpdateDistance) return null

        val lifecycleIntensity = state.lifecycleIntensityAt(gameTime)
        val pulse = zone.pulseIntensityAt(gameTime) * state.localPulseAt(gameTime)
        val sharedFlicker = zone.flickerIntensityAt(gameTime - state.flickerDelayTicks)
        val flicker = 1.0f + (sharedFlicker - 1.0f) * state.flickerStrengthScale
        val maskIntensity = maskIntensityAt(gameTime)
        val intensityScale = maskIntensity * zone.brightnessScale
        val phase = state.lifecyclePhaseAt(gameTime)
        val priority = when {
            !texture.hasUploadedVisualState -> BioluminescentTextureUpdatePriority.CREATED
            phase == BioluminescentLifecyclePhase.APPEARING -> BioluminescentTextureUpdatePriority.APPEARING
            phase == BioluminescentLifecyclePhase.DISAPPEARING ||
                phase == BioluminescentLifecyclePhase.COMPLETE ||
                maskIntensity < 0.999f -> BioluminescentTextureUpdatePriority.DISAPPEARING
            flicker > ACTIVE_FLICKER_THRESHOLD ||
                texture.hasUploadedFlickerBoost() -> BioluminescentTextureUpdatePriority.FLICKER
            else -> BioluminescentTextureUpdatePriority.STABLE
        }
        val interval = when (priority) {
            BioluminescentTextureUpdatePriority.CREATED -> CREATED_TEXTURE_INTERVAL_TICKS
            BioluminescentTextureUpdatePriority.APPEARING -> APPEARANCE_TEXTURE_INTERVAL_TICKS
            BioluminescentTextureUpdatePriority.DISAPPEARING -> DISAPPEARANCE_TEXTURE_INTERVAL_TICKS
            BioluminescentTextureUpdatePriority.FLICKER -> FLICKER_TEXTURE_INTERVAL_TICKS
            BioluminescentTextureUpdatePriority.STABLE -> STABLE_TEXTURE_INTERVAL_TICKS
        }
        val previousEvaluation = lastTextureEvaluationTick
        val elapsedSinceEvaluation = when {
            previousEvaluation == null -> Long.MAX_VALUE
            gameTime < previousEvaluation -> Long.MAX_VALUE
            else -> gameTime - previousEvaluation
        }
        val mustClear = lifecycleIntensity * intensityScale <= CLEAR_REQUEST_THRESHOLD && !texture.isClear
        if (!mustClear && elapsedSinceEvaluation < interval) return null

        if (!texture.needsUpload(state, lifecycleIntensity, pulse, flicker, intensityScale)) {
            lastTextureEvaluationTick = gameTime
            return null
        }

        val overdueTicks = if (elapsedSinceEvaluation == Long.MAX_VALUE) {
            Long.MAX_VALUE
        } else {
            (elapsedSinceEvaluation - interval).coerceAtLeast(0L)
        }
        return BioluminescentTextureUpdateRequest(
            bloom = this,
            gameTime = gameTime,
            lifecycleIntensity = lifecycleIntensity,
            pulse = pulse,
            flicker = flicker,
            intensityScale = intensityScale,
            priority = priority,
            overdueTicks = overdueTicks,
            uploadCostUnits = texture.uploadCostUnits
        )
    }

    fun applyTextureUpdate(request: BioluminescentTextureUpdateRequest): Boolean {
        if (closed || request.bloom !== this) return false
        val uploaded = texture.update(
            state = state,
            lifecycleIntensity = request.lifecycleIntensity,
            pulse = request.pulse,
            flicker = request.flicker,
            intensityScale = request.intensityScale
        )
        lastTextureEvaluationTick = request.gameTime
        return uploaded
    }

    fun shouldRemoveAt(gameTime: Long): Boolean {
        if (closed) return true
        if (state.isCompleteAt(gameTime)) return texture.isClear
        if (completedMaskOnce && validWaterCellCount == 0) return true
        val fadeStart = forcedFadeStartedAt ?: return false
        return gameTime >= fadeStart &&
            gameTime - fadeStart >= FORCED_FADE_TICKS &&
            texture.isClear
    }

    fun horizontalDistanceSquared(x: Double, z: Double): Double {
        val dx = anchor.x + 0.5 - x
        val dz = anchor.z + 0.5 - z
        return dx * dx + dz * dz
    }

    override fun close() {
        if (closed) return
        texture.close()
        surfaceHeights.fill(Double.NaN)
        stagingSurfaceHeights.fill(Double.NaN)
        ownedCells.fill(false)
        validWaterCellCount = 0
        cellOwnershipInvalidated = false
        closed = true
    }

    private fun copyInitialWaterCells(initialWaterCells: List<BioluminescentWaterCell>) {
        for (cell in initialWaterCells) {
            val cellX = cell.waterPos.x - originX
            val cellZ = cell.waterPos.z - originZ
            if (cellX !in 0 until widthInBlocks || cellZ !in 0 until lengthInBlocks) continue
            val index = cellZ * widthInBlocks + cellX
            if (!surfaceHeights[index].isNaN()) continue
            surfaceHeights[index] = cell.surfaceY
            stagingSurfaceHeights[index] = cell.surfaceY
            validWaterCellCount++
            stagingValidCellCount++
        }
    }

    private fun maskIntensityAt(gameTime: Long): Float {
        val fadeStart = forcedFadeStartedAt ?: return 1.0f
        if (gameTime <= fadeStart) return 1.0f
        return (1.0 - ModUtilities.smooth(
            0.0,
            FORCED_FADE_TICKS.toDouble(),
            (gameTime - fadeStart).toDouble()
        )).toFloat()
    }

    companion object {
        const val MASK_REFRESH_INTERVAL_TICKS = 20L
        const val CREATED_TEXTURE_INTERVAL_TICKS = 1L
        const val APPEARANCE_TEXTURE_INTERVAL_TICKS = 4L
        const val DISAPPEARANCE_TEXTURE_INTERVAL_TICKS = 3L
        const val FLICKER_TEXTURE_INTERVAL_TICKS = 4L
        const val STABLE_TEXTURE_INTERVAL_TICKS = 10L
        const val MINIMUM_WATER_CELLS = 16
        private const val ACTIVE_FLICKER_THRESHOLD = 1.002f
        private const val CLEAR_REQUEST_THRESHOLD = 0.0005f
        private const val MASK_VERTICAL_SEARCH_RADIUS = 16
        private const val FORCED_FADE_TICKS = 40L
    }
}
