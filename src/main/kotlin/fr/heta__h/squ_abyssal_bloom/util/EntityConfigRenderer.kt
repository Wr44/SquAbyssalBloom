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

    private val height = 120

    override fun render(
        graphics: GuiGraphics?,
        x: Int,
        y: Int,
        renderWidth: Int,
        tickDelta: Float
    ): Int {
        if (graphics == null) return 0

        val entity = entityProvider() ?: return 0
        val mc = Minecraft.getInstance()
        if (mc.level == null) return 0

        val maxDimension = max(entity.bbWidth, entity.bbHeight)
        val dynamicScale = (38f / maxDimension.coerceAtLeast(0.1f))

        val x0 = x
        val y0 = y
        val x1 = x + renderWidth
        val y1 = y + height

        val bestProfileAngle = 150f

        entity.yBodyRot = bestProfileAngle
        entity.yBodyRotO = bestProfileAngle
        entity.yHeadRot = bestProfileAngle
        entity.yHeadRotO = bestProfileAngle
        entity.yRot = bestProfileAngle
        entity.yRotO = bestProfileAngle
        entity.xRot = 0f
        entity.xRotO = 0f

        val quaternion = Quaternionf()
            .rotateZ(Math.PI.toFloat())
            .rotateX(Math.toRadians(-10.0).toFloat())

        val dispatcher = mc.entityRenderDispatcher
        val renderState = try {
            dispatcher.extractEntity(entity, 0f)
        } catch (e: Exception) {
            return 0
        }

        graphics.submitEntityRenderState(
            renderState,
            dynamicScale,
            Vector3f(0f, 0f, 0f),
            quaternion,
            null,
            x0, y0, x1, y1
        )

        return height
    }

    override fun close() {
    }
}