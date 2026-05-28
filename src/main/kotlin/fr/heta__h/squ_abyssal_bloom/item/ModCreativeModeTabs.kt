package fr.heta__h.squ_abyssal_bloom.item

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModCreativeModeTabs {
    val CREATIVE_MODE_TAB: DeferredRegister<CreativeModeTab> = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        SquAbyssalBloom.ID
    )

    val SPAWN_EGG_TAB: Supplier<CreativeModeTab> = CREATIVE_MODE_TAB.register(
        "spawn_egg_tab"
    ) { ->
        CreativeModeTab.builder()
            .icon { ItemStack(ModItems.BARNACLE_SPAWN_EGG.get()) }
            .title(Component.translatable("creativetab.squ_abyssal_bloom.spawn_eggs"))
            .displayItems { itemDisplayParameters, output ->
                output.accept(ModItems.BARNACLE_TOOTH.get())
                output.accept(ModItems.GUARDIAN_EYE.get())
                output.accept(ModItems.PRISMARINE_BULB.get())
                output.accept(ModItems.PRISMARINE_SPIKE.get())
                output.accept(ModItems.BRINE_BUBBLES.get())
                output.accept(ModItems.LIFELINE_BUBBLE.get())
                output.accept(ModItems.RESPIRATION_BUBBLE.get())
                output.accept(ModItems.NAUTILUS_LAMP.get())
                output.accept(ModItems.BARBED_NAUTILUS_SCALE.get())
                output.accept(ModItems.ABYSSAL_GUARDIAN_FOCALIST.get())
                output.accept(ModItems.BUBBLE_SPITTER.get())
                output.accept(ModItems.MOBILE_CONDUIT.get())

                output.accept(ModItems.ASTRAL_PRISMARINE.get())

                output.accept(ModItems.BARNACLE_SPAWN_EGG.get())
                output.accept(ModItems.BRINE_SPAWN_EGG.get())
            }
            .build()
    }

    fun register(eventBus: IEventBus) {
        CREATIVE_MODE_TAB.register(eventBus)
    }
}