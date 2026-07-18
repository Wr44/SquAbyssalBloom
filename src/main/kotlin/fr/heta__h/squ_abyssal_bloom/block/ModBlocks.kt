package fr.heta__h.squ_abyssal_bloom.block

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.BloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.TallBloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.block.sprouting_seagrass.SproutingSeagrassBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SeagrassBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlocks {
    @JvmField
    val REGISTRY: DeferredRegister.Blocks = DeferredRegister.createBlocks(SquAbyssalBloom.ID)


    // Plants
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

    val BLOOD_SEAGRASS = REGISTRY.registerBlock("blood_seagrass") { props ->
        BloodSeagrassBlock(
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

    val TALL_BLOOD_SEAGRASS = REGISTRY.registerBlock("tall_blood_seagrass") { props ->
        TallBloodSeagrassBlock(
            props.mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .instabreak()
                .sound(SoundType.WET_GRASS)
                .pushReaction(PushReaction.DESTROY)
                .noOcclusion()
        )
    }

    // Blocks
    @JvmField
    val ASTRAL_PRISMARINE = REGISTRY.registerBlock("astral_prismarine") { props ->
        AstralPrismarineBlock(
            props.mapColor(MapColor.COLOR_CYAN)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5f, 6.0f)
                .sound(SoundType.STONE)
                .lightLevel { state -> if (state.getValue(AstralPrismarineBlock.ACTIVE)) 14 else 4 }
        )
    }

    val RHODOPHYTA = REGISTRY.registerBlock("rhodophyta") { props ->
        Block(
            props.mapColor(MapColor.COLOR_RED)
                .strength(0.6f, 3.0f)
                .sound(SoundType.CORAL_BLOCK)

        )
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}