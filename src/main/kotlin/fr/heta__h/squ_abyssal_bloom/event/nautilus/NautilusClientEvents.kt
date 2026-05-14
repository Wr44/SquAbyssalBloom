package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractContainerScreenAccessor
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusMouseHelper
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ScreenEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object NautilusClientEvents {

    @SubscribeEvent
    fun onScreenInit(event: ScreenEvent.Init.Post) {
        val screen = event.screen
        if (screen is NautilusInventoryScreen) {
            val topPos = (screen as AbstractContainerScreenAccessor).getTopPos()
            NautilusMouseHelper.restore(topPos)
        }
    }
}