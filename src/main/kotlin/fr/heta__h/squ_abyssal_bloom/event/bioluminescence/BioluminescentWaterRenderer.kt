package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomState
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent
import net.neoforged.neoforge.event.level.LevelEvent

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentWaterRenderer {
    private const val GRID_SIZE = 8
    private const val SURFACE_OFFSET = 0.002
    private const val SEARCH_HORIZONTAL_RADIUS = 16
    private const val SEARCH_VERTICAL_RADIUS = 16
    private const val SEARCH_INTERVAL_TICKS = 20L
    private const val MASK_REFRESH_INTERVAL_TICKS = 20L
    private const val MASK_VERTICAL_SEARCH_RADIUS = 16
    private const val TEXTURE_UPDATE_INTERVAL_TICKS = 4L
    private const val BLOOM_LIFETIME_TICKS = 800L
    private const val BLOOM_RESTART_DELAY_TICKS = 40L
    private const val NEXT_SEED_SALT = 0x6A09E667F3BCC909L
    private const val FULL_BRIGHT_LIGHTMAP = 15728880

    data class WaterCell(
        val cellX: Int,
        val cellZ: Int,
        val waterPos: BlockPos,
        val surfaceY: Double
    )

    private val cachedCells = arrayOfNulls<WaterCell>(GRID_SIZE * GRID_SIZE)

    private var anchor: BlockPos? = null
    private var anchorLevel: ClientLevel? = null
    private var lastSearchTick: Long? = null
    private var cachedMaskAnchor: BlockPos? = null
    private var lastMaskRefreshTick: Long? = null
    private var cachedValidCellCount = 0
    private var bloomTexture: BioluminescentTexture? = null
    private var bloomRenderType: RenderType? = null
    private var bloomState: BioluminescentBloomState? = null
    private var pendingBloomSeed: Long? = null
    private var nextBloomStartTick: Long? = null
    private var lastTextureUpdateTick: Long? = null

    private fun reset() {
        anchor = null
        anchorLevel = null
        lastSearchTick = null
        clearWaterMask()
        closeBloomTexture()
    }

    private fun clearWaterMask() {
        cachedCells.fill(null)
        cachedMaskAnchor = null
        lastMaskRefreshTick = null
        cachedValidCellCount = 0
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level is ClientLevel) reset()
    }

    @SubscribeEvent
    fun onClientStopping(event: ClientStoppingEvent) {
        reset()
    }

    @SubscribeEvent
    fun onRenderLevel(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return

        if (anchorLevel !== level) {
            reset()
            anchorLevel = level
        }

        var waterSurface = anchor

        if (waterSurface == null) {
            val player = minecraft.player ?: return
            val gameTime = level.gameTime
            if (!canSearch(gameTime)) return

            lastSearchTick = gameTime
            waterSurface = ModUtilities.findNearbyWaterSurface(
                level = level,
                center = player.blockPosition(),
                horizontalRadius = SEARCH_HORIZONTAL_RADIUS,
                verticalRadius = SEARCH_VERTICAL_RADIUS
            ) ?: return

            anchor = waterSurface
            clearWaterMask()
            resetBloomForAnchor()
            SquAbyssalBloom.LOGGER.debug("[Bioluminescence] Water surface anchored at {}", waterSurface)
        }

        if (!ModUtilities.isWaterSurface(level, waterSurface)) {
            anchor = null
            lastSearchTick = null
            clearWaterMask()
            resetBloomForAnchor()
            return
        }

        updateWaterMaskIfNeeded(level, waterSurface, level.gameTime)
        if (cachedValidCellCount == 0) return

        updateBloom(level, waterSurface, level.gameTime)
        if (bloomTexture?.isVisible != true) return

        val renderType = bloomRenderType ?: return
        renderWaterMask(event, renderType)
    }

    private fun canSearch(gameTime: Long): Boolean {
        val previousSearchTick = lastSearchTick ?: return true
        return gameTime < previousSearchTick || gameTime - previousSearchTick >= SEARCH_INTERVAL_TICKS
    }

    private fun updateWaterMaskIfNeeded(level: ClientLevel, waterSurface: BlockPos, gameTime: Long) {
        val previousRefreshTick = lastMaskRefreshTick
        val refreshDue = cachedMaskAnchor != waterSurface ||
            previousRefreshTick == null ||
            gameTime < previousRefreshTick ||
            gameTime - previousRefreshTick >= MASK_REFRESH_INTERVAL_TICKS

        if (!refreshDue) return
        rebuildWaterMask(level, waterSurface, gameTime)
    }

    private fun rebuildWaterMask(level: ClientLevel, waterSurface: BlockPos, gameTime: Long) {
        val previousMaskAnchor = cachedMaskAnchor
        val previousValidCellCount = cachedValidCellCount

        cachedCells.fill(null)

        val originX = waterSurface.x - GRID_SIZE / 2
        val originZ = waterSurface.z - GRID_SIZE / 2
        val startY = minOf(level.maxY - 1, waterSurface.y + MASK_VERTICAL_SEARCH_RADIUS)
        val minimumY = maxOf(level.minY, waterSurface.y - MASK_VERTICAL_SEARCH_RADIUS)

        var validCellCount = 0

        for (cellX in 0 until GRID_SIZE) {
            for (cellZ in 0 until GRID_SIZE) {
                val worldX = originX + cellX
                val worldZ = originZ + cellZ
                val waterBlock = ModUtilities.findWaterBlockBelow(
                    level = level,
                    x = worldX,
                    z = worldZ,
                    startY = startY,
                    minimumY = minimumY
                ) ?: continue

                val topWaterBlock = ModUtilities.findTopWaterBlock(level, waterBlock) ?: continue
                if (!ModUtilities.isRenderableWaterSurface(level, topWaterBlock)) continue

                cachedCells[cellZ * GRID_SIZE + cellX] = WaterCell(
                    cellX = cellX,
                    cellZ = cellZ,
                    waterPos = topWaterBlock,
                    surfaceY = ModUtilities.getFluidSurfaceHeight(level, topWaterBlock)
                )
                validCellCount++
            }
        }

        cachedMaskAnchor = waterSurface
        lastMaskRefreshTick = gameTime
        cachedValidCellCount = validCellCount

        if (previousMaskAnchor != waterSurface || previousValidCellCount != validCellCount) {
            SquAbyssalBloom.LOGGER.debug(
                "[Bioluminescence] Water mask updated at {}: {}/{} valid cells",
                waterSurface,
                validCellCount,
                GRID_SIZE * GRID_SIZE
            )
        }
    }

    private fun updateBloom(level: ClientLevel, waterSurface: BlockPos, gameTime: Long) {
        val currentState = bloomState

        if (currentState == null) {
            val scheduledStart = nextBloomStartTick
            if (scheduledStart != null && gameTime < scheduledStart) return

            val seed = pendingBloomSeed ?: createInitialBloomSeed(level, waterSurface, gameTime)
            val state = BioluminescentBloomState.create(
                seed = seed,
                createdAt = gameTime,
                lifetime = BLOOM_LIFETIME_TICKS
            )
            val texture = getOrCreateBloomTexture()
            texture.prepare(state)

            bloomState = state
            pendingBloomSeed = null
            nextBloomStartTick = null
            lastTextureUpdateTick = gameTime
            return
        }

        if (currentState.isCompleteAt(gameTime)) {
            bloomTexture?.clear()
            bloomState = null
            pendingBloomSeed = RandomSupport.mixStafford13(currentState.seed xor NEXT_SEED_SALT)
            nextBloomStartTick = addWithoutOverflow(gameTime, BLOOM_RESTART_DELAY_TICKS)
            lastTextureUpdateTick = null
            return
        }

        val previousUpdateTick = lastTextureUpdateTick
        val updateDue = previousUpdateTick == null ||
            gameTime < previousUpdateTick ||
            gameTime - previousUpdateTick >= TEXTURE_UPDATE_INTERVAL_TICKS

        if (!updateDue) return

        getOrCreateBloomTexture().update(currentState, gameTime)
        lastTextureUpdateTick = gameTime
    }

    private fun getOrCreateBloomTexture(): BioluminescentTexture {
        val existing = bloomTexture
        if (existing != null) return existing

        val created = BioluminescentTexture(Minecraft.getInstance().textureManager)
        bloomTexture = created
        bloomRenderType = RenderTypes.eyes(created.identifier)
        return created
    }

    private fun createInitialBloomSeed(
        level: ClientLevel,
        waterSurface: BlockPos,
        gameTime: Long
    ): Long {
        val dimensionBits = level.dimension().identifier().toString().hashCode().toLong() shl 32
        return RandomSupport.mixStafford13(waterSurface.asLong() xor dimensionBits xor gameTime)
    }

    private fun addWithoutOverflow(value: Long, increment: Long): Long {
        return if (value > Long.MAX_VALUE - increment) Long.MAX_VALUE else value + increment
    }

    private fun resetBloomForAnchor() {
        bloomTexture?.clear()
        bloomState = null
        pendingBloomSeed = null
        nextBloomStartTick = null
        lastTextureUpdateTick = null
    }

    private fun closeBloomTexture() {
        bloomTexture?.close()
        bloomTexture = null
        bloomRenderType = null
        bloomState = null
        pendingBloomSeed = null
        nextBloomStartTick = null
        lastTextureUpdateTick = null
    }

    private fun renderWaterMask(
        event: RenderLevelStageEvent.AfterTranslucentBlocks,
        renderType: RenderType
    ) {
        val minecraft = Minecraft.getInstance()
        val cameraPos = minecraft.gameRenderer.mainCamera.position()
        val poseStack = event.poseStack
        val bufferSource = minecraft.renderBuffers().bufferSource()

        poseStack.pushPose()

        val pose = poseStack.last()
        val consumer = bufferSource.getBuffer(renderType)

        for (cell in cachedCells) {
            if (cell == null) continue
            renderCell(consumer, pose, cell, cameraPos)
        }

        bufferSource.endBatch(renderType)
        poseStack.popPose()
    }

    private fun renderCell(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        cell: WaterCell,
        cameraPos: Vec3
    ) {
        val minX = (cell.waterPos.x - cameraPos.x).toFloat()
        val maxX = (cell.waterPos.x + 1.0 - cameraPos.x).toFloat()
        val minZ = (cell.waterPos.z - cameraPos.z).toFloat()
        val maxZ = (cell.waterPos.z + 1.0 - cameraPos.z).toFloat()
        val y = (cell.surfaceY + SURFACE_OFFSET - cameraPos.y).toFloat()

        val minU = cell.cellX.toFloat() / GRID_SIZE
        val maxU = (cell.cellX + 1).toFloat() / GRID_SIZE
        val minV = cell.cellZ.toFloat() / GRID_SIZE
        val maxV = (cell.cellZ + 1).toFloat() / GRID_SIZE

        vertex(consumer, pose, minX, y, minZ, minU, minV, 1.0f)
        vertex(consumer, pose, minX, y, maxZ, minU, maxV, 1.0f)
        vertex(consumer, pose, maxX, y, maxZ, maxU, maxV, 1.0f)
        vertex(consumer, pose, maxX, y, minZ, maxU, minV, 1.0f)

        vertex(consumer, pose, minX, y, minZ, minU, minV, -1.0f)
        vertex(consumer, pose, maxX, y, minZ, maxU, minV, -1.0f)
        vertex(consumer, pose, maxX, y, maxZ, maxU, maxV, -1.0f)
        vertex(consumer, pose, minX, y, maxZ, minU, maxV, -1.0f)
    }

    private fun vertex(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        normalY: Float
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, normalY, 0.0f)
    }
}
