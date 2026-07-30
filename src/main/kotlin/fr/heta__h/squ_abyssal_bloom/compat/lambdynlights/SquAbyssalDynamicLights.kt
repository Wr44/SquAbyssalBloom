package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity.NautilusLampLuminance
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.neoforged.neoforge.common.Tags

class SquAbyssalDynamicLights : DynamicLightsInitializer {

    override fun onInitializeDynamicLights(context: DynamicLightsContext) {
        context.entityLightSourceManager().onRegisterEvent().register { ctx ->

            ctx.register(EntityType.NAUTILUS, NautilusLampLuminance.INSTANCE)
            ctx.register(EntityType.ZOMBIE_NAUTILUS, NautilusLampLuminance.INSTANCE)
        }

        context.itemLightSourceManager().onRegisterEvent().register { ctx ->
            ctx.register(ModItems.BIOLUMINESCENT_CRYSTAL.get(), 10)
            ctx.register(ModItems.NAUTILUS_LAMP.get(), 15)
        }
    }
}