package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractContainerScreenAccessor
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractMountInventoryScreen::class)
abstract class NautilusScreenMixin {

    @Inject(method = ["renderBg"], at = [At("RETURN")])
    private fun drawExtraSlotBackground(
        guiGraphics: GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int,
        ci: CallbackInfo
    ) {
        val nautilusScreen = (this as Any) as? NautilusInventoryScreen ?: return
        val accessor = nautilusScreen as AbstractContainerScreenAccessor

        val renderX = accessor.getLeftPos() + 7
        val renderY = accessor.getTopPos() + 53

        val notreSlot = nautilusScreen.menu.slots.find { it.x == 8 && it.y == 54 }
        val hasItem = notreSlot?.hasItem() ?: false
        val isHovering = mouseX >= renderX && mouseX < renderX + 18 && mouseY >= renderY && mouseY < renderY + 18

        val vanillaSlotSprite = Identifier.withDefaultNamespace("container/slot")

        if (hasItem) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, vanillaSlotSprite, renderX, renderY, 18, 18)
        } else if (isHovering) {
            val customHoverTexture = Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "textures/gui/nautilus_extra_slot_empty_hover.png")
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, customHoverTexture, renderX, renderY, 0f, 0f, 18, 18, 18, 18)
        } else {
            val customEmptyTexture = Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "textures/gui/nautilus_extra_slot_empty.png")
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, customEmptyTexture, renderX, renderY, 0f, 0f, 18, 18, 18, 18)
        }

        val hasChest = nautilusScreen.menu.slots.any { it.x == 80 && it.y == 18 }

        if (hasChest) {
            val chestGridSprite = Identifier.withDefaultNamespace("container/horse/chest_slots")

            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                chestGridSprite,
                accessor.getLeftPos() + 79,
                accessor.getTopPos() + 17,
                90, 54
            )
        }
    }
}