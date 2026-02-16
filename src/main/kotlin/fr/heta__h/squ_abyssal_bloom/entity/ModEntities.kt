package fr.heta__h.squ_abyssal_bloom.entity

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleModel
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleRenderer
import fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera.GhostChimaeraModel
import fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera.GhostChimaeraRenderer
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.ghost_chimaera.GhostChimaeraEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModEntities {
    val ENTITY_TYPES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Squ_abyssal_bloom.ID)

    val BARNACLE_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "barnacle"))

    val BARNACLE: DeferredHolder<EntityType<*>, EntityType<BarnacleEntity>> =
        ENTITY_TYPES.register("barnacle") { _: Identifier ->
            EntityType.Builder.of({ type, level -> BarnacleEntity(type, level) }, MobCategory.MONSTER)
                .sized(3f, 1.75f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(BARNACLE_KEY)
        }

    val GHOAST_CHIMAERA_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "ghost_chimera"))

    val GHOST_CHIMAERA: DeferredHolder<EntityType<*>, EntityType<GhostChimaeraEntity>> =
        ENTITY_TYPES.register("ghost_chimera") { _: Identifier ->
            EntityType.Builder.of({ type, level -> GhostChimaeraEntity(type, level) }, MobCategory.UNDERGROUND_WATER_CREATURE)
                .sized(7.5f, 3f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(GHOAST_CHIMAERA_KEY)
        }

    fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(BARNACLE.get() as EntityType<out BarnacleEntity>, ::BarnacleRenderer)
        event.registerEntityRenderer(GHOST_CHIMAERA.get() as EntityType<out GhostChimaeraEntity>, ::GhostChimaeraRenderer)
    }

    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(
            BarnacleModel.LAYER_LOCATION,
            BarnacleModel::createBodyLayer
        )

        event.registerLayerDefinition(
            GhostChimaeraModel.LAYER_LOCATION,
            GhostChimaeraModel::createBodyLayer
        )
    }


    fun onRegisterAttributes(event: EntityAttributeCreationEvent) {
        event.put(BARNACLE.get(), BarnacleEntity.createAttributes().build())
        event.put(GHOST_CHIMAERA.get(), GhostChimaeraEntity.createAttributes().build())
    }

    fun register(eventBus: IEventBus) {
        ENTITY_TYPES.register(eventBus)
    }

    fun registerSpawnPlacements(event: RegisterSpawnPlacementsEvent) {
        event.register(
            BARNACLE.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkBarnacleSpawn,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        )
    }

    private fun checkBarnacleSpawn(
        entityType: EntityType<BarnacleEntity>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        return pos.y <= 24 && level.getFluidState(pos).`is`(FluidTags.WATER)
    }
}