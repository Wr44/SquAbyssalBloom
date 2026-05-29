package fr.heta__h.squ_abyssal_bloom.particle

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.particle.marine_snow.MarineSnowParticleProvider
import fr.heta__h.squ_abyssal_bloom.particle.nautilus.NautilusTrackingParticleProvider
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModParticles {
    val REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, SquAbyssalBloom.ID)

    val MARINE_SNOW = REGISTRY.register("marine_snow", Supplier { SimpleParticleType(false) })

    val NAUTILUS_TRACKING_PARTICLE = REGISTRY.register("nautilus_tracking_particle", Supplier { SimpleParticleType(false) })

    fun register(modBus: IEventBus) = REGISTRY.register(modBus)

    fun registerParticleProviders(event: net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent) {
        event.registerSpriteSet(MARINE_SNOW.get(), ::MarineSnowParticleProvider)
        event.registerSpriteSet(NAUTILUS_TRACKING_PARTICLE.get(), ::NautilusTrackingParticleProvider)
    }
}
