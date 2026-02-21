package fr.heta__h.squ_abyssal_bloom.item

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems.ITEMS
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
        Squ_abyssal_bloom.ID
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
                output.accept(ModItems.ABYSSAL_GUARDIAN_FOCALIST.get())
                output.accept(ModItems.BARNACLE_SPAWN_EGG.get())
            }
            .build()
    }

    fun register(eventBus: IEventBus) {
        CREATIVE_MODE_TAB.register(eventBus)
    }
}