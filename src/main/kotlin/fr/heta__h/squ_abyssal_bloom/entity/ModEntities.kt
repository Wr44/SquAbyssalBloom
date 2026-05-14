package fr.heta__h.squ_abyssal_bloom.entity

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleModel
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleRenderer
import fr.heta__h.squ_abyssal_bloom.entity.client.brine.BrineModel
import fr.heta__h.squ_abyssal_bloom.entity.client.brine.BrineRenderer
import fr.heta__h.squ_abyssal_bloom.entity.client.bubble.BubbleRenderer
import fr.heta__h.squ_abyssal_bloom.entity.client.bubble.BubbleStage1Model
import fr.heta__h.squ_abyssal_bloom.entity.client.bubble.BubbleStage2Model
import fr.heta__h.squ_abyssal_bloom.entity.client.bubble.BubbleStage3Model
import fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera.GhostChimaeraModel
import fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera.GhostChimaeraRenderer
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.guardian_spike.GuardianSpikeModel
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.ghost_chimaera.GhostChimaeraEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.guardian_spike.GuardianSpikesLayer
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusBubbleSpitterModel
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusChestModel
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLampModel
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
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
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SquAbyssalBloom.ID)

    val BARNACLE_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "barnacle"))


    

    val BARNACLE: DeferredHolder<EntityType<*>, EntityType<BarnacleEntity>> =
        ENTITY_TYPES.register("barnacle") { _: Identifier ->
            EntityType.Builder.of({ type, level -> BarnacleEntity(type, level) }, MobCategory.MONSTER)
                .sized(3f, 1.75f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(BARNACLE_KEY)
        }

    val GHOAST_CHIMAERA_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "ghost_chimera"))

    val GHOST_CHIMAERA: DeferredHolder<EntityType<*>, EntityType<GhostChimaeraEntity>> =
        ENTITY_TYPES.register("ghost_chimera") { _: Identifier ->
            EntityType.Builder.of({ type, level -> GhostChimaeraEntity(type, level) }, MobCategory.UNDERGROUND_WATER_CREATURE)
                .sized(7.5f, 3f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(GHOAST_CHIMAERA_KEY)
        }

    val BRINE_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "brine"))

    val BRINE: DeferredHolder<EntityType<*>, EntityType<BrineEntity>> =
        ENTITY_TYPES.register("brine") { _: Identifier ->
            EntityType.Builder.of({ type, level -> BrineEntity(type, level) }, MobCategory.MONSTER)
                .sized(1f, 1.8f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(BRINE_KEY)
        }


    val BUBBLE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bubble_projectile"))

    val BUBBLE = ENTITY_TYPES.register("bubble_projectile") { name: Identifier ->
        EntityType.Builder.of({ type, level -> BubbleProjectile(type, level) }, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(4)
            .updateInterval(1)
            .build(BUBBLE_KEY)
    }


    fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        
        event.registerEntityRenderer(BARNACLE.get() as EntityType<out BarnacleEntity>, ::BarnacleRenderer)
        event.registerEntityRenderer(GHOST_CHIMAERA.get() as EntityType<out GhostChimaeraEntity>, ::GhostChimaeraRenderer)
        event.registerEntityRenderer(BRINE.get() as EntityType<out BrineEntity>, ::BrineRenderer)

        
        event.registerEntityRenderer(BUBBLE.get() as EntityType<out BubbleProjectile>, ::BubbleRenderer)
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

        event.registerLayerDefinition(
            BrineModel.LAYER_LOCATION,
            BrineModel::createBodyLayer
        )

        event.registerLayerDefinition(
            BubbleStage1Model.LAYER_LOCATION,
            BubbleStage1Model::createBodyLayer
        )

        event.registerLayerDefinition(
            BubbleStage2Model.LAYER_LOCATION,
            BubbleStage2Model::createBodyLayer
        )

        event.registerLayerDefinition(
            BubbleStage3Model.LAYER_LOCATION,
            BubbleStage3Model::createBodyLayer
        )

        
        event.registerLayerDefinition(
            GuardianSpikeModel.LAYER_LOCATION,
            GuardianSpikeModel::createBodyLayer
        )

        event.registerLayerDefinition(
            NautilusLampModel.LAYER_LOCATION,
            NautilusLampModel::createBodyLayer
        )

        event.registerLayerDefinition(
            NautilusBubbleSpitterModel.LAYER_LOCATION,
            NautilusBubbleSpitterModel::createBodyLayer
        )

        event.registerLayerDefinition(
            NautilusChestModel.LAYER_LOCATION,
            NautilusChestModel::createBodyLayer
        )
    }


    fun onRegisterAttributes(event: EntityAttributeCreationEvent) {
        event.put(BARNACLE.get(), BarnacleEntity.createAttributes().build())
        event.put(GHOST_CHIMAERA.get(), GhostChimaeraEntity.createAttributes().build())
        event.put(BRINE.get(), BrineEntity.createAttributes().build())
    }

    fun onAddLayers(event: EntityRenderersEvent.AddLayers) {
        val spikeModel = GuardianSpikeModel(event.entityModels.bakeLayer(GuardianSpikeModel.LAYER_LOCATION))
        val lampModel = NautilusLampModel(event.entityModels.bakeLayer(NautilusLampModel.LAYER_LOCATION))
        val bubbleModel = NautilusBubbleSpitterModel(event.entityModels.bakeLayer(NautilusBubbleSpitterModel.LAYER_LOCATION))
        val chestModel = NautilusChestModel(event.entityModels.bakeLayer(NautilusChestModel.LAYER_LOCATION))

        @Suppress("UNCHECKED_CAST")
        fun LivingEntityRenderer<*, *, *>.cast() = this as LivingEntityRenderer<LivingEntity, LivingEntityRenderState, EntityModel<LivingEntityRenderState>>
        fun LivingEntityRenderer<*, *, *>.addSpikes() = cast().addLayer(GuardianSpikesLayer(cast(), spikeModel))
        fun LivingEntityRenderer<*, *, *>.addNautilus() = cast().addLayer(NautilusLayer(cast(), lampModel, bubbleModel, chestModel))

        BuiltInRegistries.ENTITY_TYPE.forEach { entityType ->
            (event.getRenderer(entityType) as? LivingEntityRenderer<*, *, *>)?.addSpikes()
        }
        event.skins.forEach { skin ->
            (event.getPlayerRenderer(skin) as? LivingEntityRenderer<*, *, *>)?.addSpikes()
        }

        listOf(EntityType.NAUTILUS, EntityType.ZOMBIE_NAUTILUS).forEach { entityType ->
            (event.getRenderer(entityType) as? LivingEntityRenderer<*, *, *>)?.let {
                it.addNautilus()

            }
        }
    }

    fun register(eventBus: IEventBus) {
        ENTITY_TYPES.register(eventBus)
    }

    fun registerSpawnPlacements(event: RegisterSpawnPlacementsEvent) {
        event.register(
            BARNACLE.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.OCEAN_FLOOR,
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