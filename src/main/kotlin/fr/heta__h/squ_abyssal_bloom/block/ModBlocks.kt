package fr.heta__h.squ_abyssal_bloom.block

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.sprouting_sea_grass.SproutingSeagrassBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlocks {
    val REGISTRY: DeferredRegister.Blocks = DeferredRegister.createBlocks(SquAbyssalBloom.ID)

    val SPROUTING_SEAGRASS = REGISTRY.registerBlock("sprouting_seagrass") { props ->
        SproutingSeagrassBlock(
            props.mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .instabreak()
                .sound(SoundType.WET_GRASS)
                .pushReaction(PushReaction.DESTROY)
                .noOcclusion()
                .randomTicks()
        )
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}
