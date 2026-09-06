package fr.heta__h.squ_abyssal_bloom.data_component

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.data_component.bubble.SplatterData
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModDataComponents {
    val REGISTRY = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SquAbyssalBloom.ID)

    val IS_SPROUTING: Supplier<DataComponentType<Boolean>> = REGISTRY.register("is_sprouting", Supplier {
        DataComponentType.builder<Boolean>()
            .persistent(Codec.BOOL)
            .build()
    })

    val SPLATTER_DATA: Supplier<DataComponentType<SplatterData>> = REGISTRY.register("splatter_data", Supplier {
        DataComponentType.builder<SplatterData>()
            .persistent(SplatterData.CODEC)
            .networkSynchronized(SplatterData.STREAM_CODEC)
            .build()
    })

    val PLANKTON_LUMINESCENCE: Supplier<DataComponentType<Boolean>> = REGISTRY.register("plankton_luminescence", Supplier {
        DataComponentType.builder<Boolean>()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build()
    })

    val PLANKTON_FILL: Supplier<DataComponentType<Int>> = REGISTRY.register("plankton_fill", Supplier {
        DataComponentType.builder<Int>()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    })

    fun register(modBus: IEventBus) {
        REGISTRY.register(modBus)
    }
}