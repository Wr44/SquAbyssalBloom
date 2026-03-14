package fr.heta__h.squ_abyssal_bloom.particle

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModParticles {
    val REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, Squ_abyssal_bloom.ID)

    val MARINE_SNOW = REGISTRY.register("marine_snow", Supplier { SimpleParticleType(false) })

    fun register(modBus: IEventBus) = REGISTRY.register(modBus)
}
