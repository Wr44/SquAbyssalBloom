package fr.heta__h.squ_abyssal_bloom.event.nautilus.bubble

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile.Companion.stageFor
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object NautilusBubbleHud {

    private val SPRITE_BG = Identifier.withDefaultNamespace("hud/jump_bar_background")
    private val SPRITE_PROGRESS = Identifier.withDefaultNamespace("hud/jump_bar_progress")

    private val STAGE_TINTS = intArrayOf(
        0xFF44CCFF.toInt(),
        0xFF0088FF.toInt(),
        0xFF0033CC.toInt(),
    )
    private const val OVERCHARGE_TINT  = 0xFFFF4444.toInt()
    private const val COOLDOWN_TINT = 0xFF888888.toInt()
    private const val BAR_WIDTH = 182
    private const val BAR_HEIGHT = 5
    private const val COOLDOWN_TICKS = 40

    private var cooldownEndGameTime = 0L
    private var prevDashing = false

    private var cachedHeldBubble: BubbleProjectile? = null
    private var lastCacheTick = -1L

    @SubscribeEvent
    fun onRenderBar(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.CONTEXTUAL_INFO_BAR) return

        val mc = Minecraft.getInstance()
        if (mc.options.hideGui) return

        val player = mc.player ?: return
        val nautilus = player.vehicle as? AbstractNautilus ?: return
        if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item != NautilusLayer.BUBBLE) return

        event.isCanceled = true

        val dashing = nautilus.isDashing
        val gameTime = mc.level?.gameTime ?: 0L
        if (dashing && !prevDashing) {
            cooldownEndGameTime = gameTime + COOLDOWN_TICKS
        }
        prevDashing = dashing

        val gui = event.guiGraphics
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        val barX = (screenW - BAR_WIDTH) / 2

        val barY = screenH - 29

        if (gameTime != lastCacheTick) {
            cachedHeldBubble = mc.level?.getEntitiesOfClass(
                BubbleProjectile::class.java, nautilus.boundingBox.inflate(3.0)
            ) { it.isHeld && !it.isRemoved }?.firstOrNull()
            lastCacheTick = gameTime
        }

        val heldBubble = cachedHeldBubble
        val cooldownProgress = if (gameTime < cooldownEndGameTime)
            ((cooldownEndGameTime - gameTime).toFloat() / COOLDOWN_TICKS).coerceIn(0f, 1f)
        else 0f

        gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_BG, barX, barY, BAR_WIDTH, BAR_HEIGHT)

        when {
            heldBubble != null -> {
                val ticks = heldBubble.holdTicks
                val ratio = (ticks.toFloat() / BubbleProjectile.TICKS_TO_OVERCHARGE).coerceIn(0f, 1f)
                val fillWidth = (ratio * BAR_WIDTH).toInt().coerceAtLeast(1)
                val isOvercharge = ticks >= BubbleProjectile.TICKS_TO_OVERCHARGE
                val tint = if (isOvercharge) OVERCHARGE_TINT else STAGE_TINTS[stageFor(ticks)]

                gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_PROGRESS,
                    BAR_WIDTH, BAR_HEIGHT, 0, 0, barX, barY, fillWidth, BAR_HEIGHT, tint)

                for (threshold in intArrayOf(
                    BubbleProjectile.TICKS_TO_STAGE_1,
                    BubbleProjectile.TICKS_TO_STAGE_2,
                )) {
                    val sepX = barX + ((threshold.toFloat() / BubbleProjectile.TICKS_TO_OVERCHARGE) * BAR_WIDTH).toInt()
                    gui.fill(RenderPipelines.GUI, sepX, barY, sepX + 1, barY + BAR_HEIGHT, 0xCC000000.toInt())
                }
            }

            cooldownProgress > 0f -> {
                val fillWidth = (cooldownProgress * BAR_WIDTH).toInt().coerceAtLeast(1)
                gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_PROGRESS,
                    BAR_WIDTH, BAR_HEIGHT, 0, 0, barX, barY, fillWidth, BAR_HEIGHT, COOLDOWN_TINT)
            }
        }
    }

    @SubscribeEvent
    fun onRenderBarBackground(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND) return
        val mc = Minecraft.getInstance()
        val nautilus = mc.player?.vehicle as? AbstractNautilus ?: return
        if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item != NautilusLayer.BUBBLE) return
        event.isCanceled = true
    }


}