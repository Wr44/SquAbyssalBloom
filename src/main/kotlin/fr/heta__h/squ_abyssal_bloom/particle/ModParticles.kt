package fr.heta__h.squ_abyssal_bloom.particle

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water.BioluminescentWaterParticleProvider
import fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water.BioluminescentWaterParticleType
import fr.heta__h.squ_abyssal_bloom.particle.marine_snow.MarineSnowParticleProvider
import fr.heta__h.squ_abyssal_bloom.particle.nautilus.NautilusTrackingParticleProvider
import fr.heta__h.squ_abyssal_bloom.particle.underwater_torch.UnderwaterTorchBubbleParticleProvider
import net.minecraft.client.particle.FlameParticle
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModParticles {
    val REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, SquAbyssalBloom.ID)

    val MARINE_SNOW = REGISTRY.register("marine_snow", Supplier { SimpleParticleType(false) })

    val NAUTILUS_TRACKING_PARTICLE = REGISTRY.register("nautilus_tracking_particle", Supplier { SimpleParticleType(false) })

    val UNDERWATER_CRYSTAL_GAZ = REGISTRY.register("underwater_crystal_gaz", Supplier { SimpleParticleType(false) })

    val UNDERWATER_TORCH_BUBBLE = REGISTRY.register("underwater_torch_bubble", Supplier { SimpleParticleType(false) })

    val BIOLUMINESCENT_WATER_PARTICLE = REGISTRY.register("bioluminescent_water_particle", Supplier { BioluminescentWaterParticleType(false) })

    fun register(modBus: IEventBus) = REGISTRY.register(modBus)

    fun registerParticleProviders(event: net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent) {
        event.registerSpriteSet(MARINE_SNOW.get(), ::MarineSnowParticleProvider)
        event.registerSpriteSet(NAUTILUS_TRACKING_PARTICLE.get(), ::NautilusTrackingParticleProvider)
        event.registerSpriteSet(UNDERWATER_CRYSTAL_GAZ.get()) { sprites -> FlameParticle.Provider(sprites) }
        event.registerSpriteSet(UNDERWATER_TORCH_BUBBLE.get(), ::UnderwaterTorchBubbleParticleProvider)
        event.registerSpriteSet(BIOLUMINESCENT_WATER_PARTICLE.get(), ::BioluminescentWaterParticleProvider)
    }
}
