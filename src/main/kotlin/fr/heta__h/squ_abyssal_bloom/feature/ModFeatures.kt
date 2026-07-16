package fr.heta__h.squ_abyssal_bloom.feature

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.feature.vegetation.BloodSeagrassFeature
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModFeatures {

    val REGISTRY: DeferredRegister<Feature<*>> = DeferredRegister.create(Registries.FEATURE, SquAbyssalBloom.ID)

    val BLOOD_SEAGRASS_FEATURE: Supplier<BloodSeagrassFeature> = REGISTRY.register("blood_seagrass", Supplier {
        BloodSeagrassFeature(ProbabilityFeatureConfiguration.CODEC)
    })

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}