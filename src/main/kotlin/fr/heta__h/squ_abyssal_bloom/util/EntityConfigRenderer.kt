package fr.heta__h.squ_abyssal_bloom.util

import dev.isxander.yacl3.gui.image.ImageRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.max

class EntityConfigRenderer(
    private val entityProvider: () -> LivingEntity?
) : ImageRenderer {

    companion object {
        private const val HEIGHT = 120
        private const val PROFILE_ANGLE = 150f
        private val BASE_ROTATION = Quaternionf()
            .rotateZ(Math.PI.toFloat())
            .rotateX(Math.toRadians(-10.0).toFloat())
    }

    override fun render(
        graphics: GuiGraphics?,
        x: Int,
        y: Int,
        renderWidth: Int,
        tickDelta: Float
    ): Int {

        val entity = entityProvider() ?: return 0
        val mc = Minecraft.getInstance()
        if (graphics == null) return 0

        val maxDimension = max(entity.bbWidth, entity.bbHeight).coerceAtLeast(0.1f)
        val scale = 50f / maxDimension

        val x1 = x + renderWidth
        val y1 = y + HEIGHT

        val prevYBody = entity.yBodyRot
        val prevYHead = entity.yHeadRot
        val prevYRot = entity.yRot
        val prevXRot = entity.xRot

        try {
            entity.yBodyRot = PROFILE_ANGLE
            entity.yBodyRotO = PROFILE_ANGLE
            entity.yHeadRot = PROFILE_ANGLE
            entity.yHeadRotO = PROFILE_ANGLE
            entity.yRot = PROFILE_ANGLE
            entity.yRotO = PROFILE_ANGLE
            entity.xRot = 0f
            entity.xRotO = 0f

            val dispatcher = mc.entityRenderDispatcher
            val renderState = dispatcher.extractEntity(entity, tickDelta)

            graphics.submitEntityRenderState(
                renderState,
                scale,
                Vector3f(0f, 0f, 0f),
                BASE_ROTATION,
                null,
                x, y, x1, y1
            )

        } catch (_: Exception) {
            return 0
        } finally {
            entity.yBodyRot = prevYBody
            entity.yHeadRot = prevYHead
            entity.yRot = prevYRot
            entity.xRot = prevXRot
        }

        return HEIGHT
    }

    override fun close() {}
}
