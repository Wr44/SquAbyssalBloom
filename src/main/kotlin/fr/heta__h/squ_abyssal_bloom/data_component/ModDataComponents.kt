package fr.heta__h.squ_abyssal_bloom.data_component

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModDataComponents {
    val REGISTRY = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Squ_abyssal_bloom.ID)

    val IS_SPROUTING: Supplier<DataComponentType<Boolean>> = REGISTRY.register("is_sprouting", Supplier {
        DataComponentType.builder<Boolean>()
            .persistent(com.mojang.serialization.Codec.BOOL)
            .build()
    })

    fun register(modBus: IEventBus) {
        REGISTRY.register(modBus)
    }
}