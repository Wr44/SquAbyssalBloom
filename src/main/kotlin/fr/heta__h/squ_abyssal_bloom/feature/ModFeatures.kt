package fr.heta__h.squ_abyssal_bloom.feature

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.feature.vegetation.BloodSeagrassFeature
import fr.heta__h.squ_abyssal_bloom.feature.vegetation.DeadRhodophytaFeature
import fr.heta__h.squ_abyssal_bloom.feature.vegetation.RedCoralReefFeature
import fr.heta__h.squ_abyssal_bloom.feature.vegetation.RhodophytaTowerFeature
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModFeatures {

    val REGISTRY: DeferredRegister<Feature<*>> = DeferredRegister.create(Registries.FEATURE, SquAbyssalBloom.ID)

    val BLOOD_SEAGRASS_FEATURE = REGISTRY.register("blood_seagrass", Supplier {
        BloodSeagrassFeature(ProbabilityFeatureConfiguration.CODEC)
    })

    val RED_CORAL_REEF_FEATURE = REGISTRY.register("red_coral_reef", Supplier {
        RedCoralReefFeature(NoneFeatureConfiguration.CODEC)
    })

    val RHODOPHYTA_TOWER_FEATURE = REGISTRY.register("rhodophyta_tower", Supplier {
        RhodophytaTowerFeature(NoneFeatureConfiguration.CODEC)
    })

    val DEAD_RHODOPHYTA = REGISTRY.register("dead_rhodophyta", Supplier {
        DeadRhodophytaFeature(NoneFeatureConfiguration.CODEC)
    })

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}