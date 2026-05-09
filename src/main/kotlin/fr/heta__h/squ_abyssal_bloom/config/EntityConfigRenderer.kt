package fr.heta__h.squ_abyssal_bloom.config

import dev.isxander.yacl3.gui.image.ImageRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.max

class EntityConfigRenderer(
    private val entityType: EntityType<out LivingEntity>,
    private val prepareEntity: ((LivingEntity, Int) -> Unit)? = null
) : ImageRenderer {

    companion object {
        private const val HEIGHT = 120
        private const val ROTATION_SPEED = 35.0
        private val BASE_ROTATION = Quaternionf()
            .rotateZ(Math.PI.toFloat())
            .rotateX(Math.toRadians(-10.0).toFloat())

        private val EPOCH_MS = System.currentTimeMillis()
    }

    private var cachedEntity: LivingEntity? = null

    
    private val virtualTick: Int
        get() = ((System.currentTimeMillis() - EPOCH_MS) / 50L).toInt()

    private fun getOrCreateEntity(): LivingEntity? {
        cachedEntity?.let { return it }

        val level = Minecraft.getInstance().level ?: return null

        @Suppress("UNCHECKED_CAST")
        val entity = (entityType as EntityType<LivingEntity>).create(level, EntitySpawnReason.LOAD) ?: return null

        cachedEntity = entity
        return entity
    }

    override fun render(
        graphics: GuiGraphics?,
        x: Int,
        y: Int,
        renderWidth: Int,
        tickDelta: Float
    ): Int {
        val entity = getOrCreateEntity() ?: return 0
        val mc = Minecraft.getInstance()
        if (graphics == null) return 0

        val tick = virtualTick
        entity.tickCount = tick

        val angle = ((System.currentTimeMillis() / 1000.0 * ROTATION_SPEED) % 360).toFloat()

        prepareEntity?.invoke(entity, tick)

        val maxDimension = max(entity.bbWidth, entity.bbHeight).coerceAtLeast(0.1f)
        val scale = 50f / maxDimension

        val prevYBody = entity.yBodyRot
        val prevYHead = entity.yHeadRot
        val prevYRot  = entity.yRot
        val prevXRot  = entity.xRot

        try {
            entity.yBodyRot  = angle
            entity.yBodyRotO = angle
            entity.yHeadRot  = angle
            entity.yHeadRotO = angle
            entity.yRot      = angle
            entity.yRotO     = angle
            entity.xRot      = 0f
            entity.xRotO     = 0f

            val dispatcher  = mc.entityRenderDispatcher
            val renderState = dispatcher.extractEntity(entity, tickDelta)

            graphics.submitEntityRenderState(
                renderState,
                scale,
                Vector3f(0f, 0f, 0f),
                BASE_ROTATION,
                null,
                x, y,
                x + renderWidth, y + HEIGHT
            )

        } catch (_: Exception) {
            return 0
        } finally {
            entity.yBodyRot = prevYBody
            entity.yHeadRot = prevYHead
            entity.yRot     = prevYRot
            entity.xRot     = prevXRot
        }

        return HEIGHT
    }

    override fun close() {
        cachedEntity = null
    }
}