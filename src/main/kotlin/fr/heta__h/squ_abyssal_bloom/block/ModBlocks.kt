package fr.heta__h.squ_abyssal_bloom.block

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.BloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.TallBloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.block.brine_bubble_column.BrineBubbleColumnBlock
import fr.heta__h.squ_abyssal_bloom.block.calcareous_deposit.CalcareousDepositBlock
import fr.heta__h.squ_abyssal_bloom.block.sprouting_seagrass.SproutingSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.block.underwater_torch.UnderwaterTorchBlock
import fr.heta__h.squ_abyssal_bloom.block.underwater_torch.UnderwaterWallTorchBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CoralBlock
import net.minecraft.world.level.block.LanternBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.WallBlock
import net.minecraft.world.level.block.state.BlockBehaviour
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
    val BRINE_BUBBLE_COLUMN = REGISTRY.registerBlock("brine_bubble_column") { props ->
        BrineBubbleColumnBlock(
            props.mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY)
                .liquid()
                .sound(SoundType.EMPTY)
        )
    }

    val CALCAREOUS_DEPOSIT = REGISTRY.registerBlock("calcareous_deposit") { props ->
        CalcareousDepositBlock(
            props.mapColor(MapColor.COLOR_LIGHT_GRAY)
                .noCollision()
                .noOcclusion()
                .strength(0.45f)
                .sound(SoundType.CALCITE)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    @JvmField
    val ASTRAL_PRISMARINE = REGISTRY.registerBlock("astral_prismarine") { props ->
        AstralPrismarineBlock(
            props.mapColor(MapColor.COLOR_CYAN)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(30.0f, 750.0f)
                .sound(SoundType.STONE)
                .lightLevel { state -> if (state.getValue(AstralPrismarineBlock.ACTIVE)) 14 else 4 }
        )
    }

    val DEAD_RHODOPHYTA = REGISTRY.registerBlock("dead_rhodophyta") { props ->
        Block(
            props.mapColor(MapColor.COLOR_GRAY)
                .strength(0.6f, 3.0f)
                .sound(SoundType.CORAL_BLOCK)
        )
    }

    val RHODOPHYTA = REGISTRY.registerBlock("rhodophyta") { props ->
        CoralBlock(
            DEAD_RHODOPHYTA.get(),
            props.mapColor(MapColor.COLOR_RED)
                .strength(0.6f, 3.0f)
                .sound(SoundType.CORAL_BLOCK)
                .lightLevel { _ -> 4 }
        )
    }

    val MARINE_BRICKS = REGISTRY.registerBlock("marine_bricks") { props -> Block(props.marineBricks()) }

    val ALGEA_INFESTED_MARINE_BRICKS = REGISTRY.registerBlock("algea_infested_marine_bricks") { props -> Block(props.marineBricks()) }

    val RHODOPHYTA_INFESTED_MARINE_BRICKS = REGISTRY.registerBlock("rhodophyta_infested_marine_bricks") { props -> Block(props.marineBricks()) }

    val CHISELED_MARINE_BRICKS = REGISTRY.registerBlock("chiseled_marine_bricks") { props -> Block(props.marineBricks()) }

    val MARINE_BRICKS_STAIRS = REGISTRY.registerBlock("marine_bricks_stairs") { props ->
        StairBlock(MARINE_BRICKS.get().defaultBlockState(), props.marineBricks())
    }

    val MARINE_BRICKS_SLAB = REGISTRY.registerBlock("marine_bricks_slab") { props ->
        SlabBlock(props.marineBricks())
    }

    val ALGEA_INFESTED_MARINE_BRICKS_STAIRS = REGISTRY.registerBlock("algea_infested_marine_bricks_stairs") { props ->
        StairBlock(ALGEA_INFESTED_MARINE_BRICKS.get().defaultBlockState(), props.marineBricks())
    }

    val ALGEA_INFESTED_MARINE_BRICKS_SLAB = REGISTRY.registerBlock("algea_infested_marine_bricks_slab") { props ->
        SlabBlock(props.marineBricks())
    }

    val RHODOPHYTA_INFESTED_MARINE_BRICKS_STAIRS = REGISTRY.registerBlock("rhodophyta_infested_marine_bricks_stairs") { props ->
        StairBlock(RHODOPHYTA_INFESTED_MARINE_BRICKS.get().defaultBlockState(), props.marineBricks())
    }

    val RHODOPHYTA_INFESTED_MARINE_BRICKS_SLAB = REGISTRY.registerBlock("rhodophyta_infested_marine_bricks_slab") { props ->
        SlabBlock(props.marineBricks())
    }

    val MARINE_BRICKS_WALL = REGISTRY.registerBlock("marine_bricks_wall") { props ->
        WallBlock(props.marineBricks().forceSolidOn())
    }

    val ALGEA_INFESTED_MARINE_BRICKS_WALL = REGISTRY.registerBlock("algea_infested_marine_bricks_wall") { props ->
        WallBlock(props.marineBricks().forceSolidOn())
    }

    val RHODOPHYTA_INFESTED_MARINE_BRICKS_WALL = REGISTRY.registerBlock("rhodophyta_infested_marine_bricks_wall") { props ->
        WallBlock(props.marineBricks().forceSolidOn())
    }

    val BIOLUMINESCENT_TORCH = REGISTRY.registerBlock("bioluminescent_torch") { props ->
        UnderwaterTorchBlock(
            props.noCollision()
                .instabreak()
                .lightLevel { _ -> 14 }
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.DESTROY)
        )
    }

    val BIOLUMINESCENT_WALL_TORCH = REGISTRY.registerBlock("bioluminescent_wall_torch") { props ->
        UnderwaterWallTorchBlock(
            props.noCollision()
                .instabreak()
                .lightLevel { _ -> 14 }
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.DESTROY)
                .overrideDescription("block.${SquAbyssalBloom.ID}.bioluminescent_torch")
        )
    }

    val BIOLUMINESCENT_LANTERN = REGISTRY.registerBlock("bioluminescent_lantern") { props ->
        LanternBlock(
            props.mapColor(MapColor.METAL)
                .forceSolidOn()
                .strength(3.5f)
                .sound(SoundType.LANTERN)
                .lightLevel { _ -> 15 }
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
        )
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }

    // Utilities for Block

    private fun BlockBehaviour.Properties.marineBricks(): BlockBehaviour.Properties =
        this.mapColor(MapColor.COLOR_GRAY)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
}
