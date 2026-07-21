package fr.heta__h.squ_abyssal_bloom.entity

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
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
import fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer.BabyRedSlobbererModel
import fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer.RedSlobbererModel
import fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer.RedSlobbererRenderer
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.guardian_spike.GuardianSpikeModel
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.ghost_chimaera.GhostChimaeraEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.guardian_spike.GuardianSpikesLayer
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusBubbleSpitterModel
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusChestModel
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLampModel
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.findLocalWaterFloor
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModEntities {
    private const val BARNACLE_NATURAL_SPAWN_ROLL = 80
    private const val BARNACLE_LOCAL_DENSITY_RADIUS = 128.0
    private const val BARNACLE_LOCAL_DENSITY_CAP = 1

    // Drowned: weight 5 with a 1/40 roll; Brine: weight 1 with 1/56 gives a 7:1 target ratio.
    private const val BRINE_NATURAL_SPAWN_ROLL = 56
    private const val BRINE_MINIMUM_SPAWN_HEIGHT_ABOVE_FLOOR = 0
    private const val BRINE_MAXIMUM_SPAWN_HEIGHT_ABOVE_FLOOR = 2
    private const val BRINE_REQUIRED_WATER_BLOCKS = 2

    private const val RED_SLOBBERER_MINIMUM_SPAWN_HEIGHT_ABOVE_FLOOR = 3
    private const val RED_SLOBBERER_MAXIMUM_SPAWN_HEIGHT_ABOVE_FLOOR = 8
    private const val RED_SLOBBERER_REQUIRED_WATER_BLOCKS = 3

    val ENTITY_TYPES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SquAbyssalBloom.ID)

    val BARNACLE_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "barnacle"))


    //Entity

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
                .sized(0.7f, 1.4f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(BRINE_KEY)
        }

    val RED_SLOBBERER_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "red_slobberer"))

    val RED_SLOBBERER: DeferredHolder<EntityType<*>, EntityType<RedSlobbererEntity>> =
        ENTITY_TYPES.register("red_slobberer") { _: Identifier ->
            EntityType.Builder.of({ type, level -> RedSlobbererEntity(type, level) }, MobCategory.WATER_CREATURE)
                .sized(4.25f, 2.5f)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(RED_SLOBBERER_KEY)
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
        //Entity
        event.registerEntityRenderer(BARNACLE.get() as EntityType<out BarnacleEntity>, ::BarnacleRenderer)
        event.registerEntityRenderer(GHOST_CHIMAERA.get() as EntityType<out GhostChimaeraEntity>, ::GhostChimaeraRenderer)
        event.registerEntityRenderer(BRINE.get() as EntityType<out BrineEntity>, ::BrineRenderer)
        event.registerEntityRenderer(RED_SLOBBERER.get() as EntityType<out RedSlobbererEntity>, ::RedSlobbererRenderer)

        //Projectile
        event.registerEntityRenderer(BUBBLE.get() as EntityType<out BubbleProjectile>, ::BubbleRenderer)
    }

    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        //Entity
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
            RedSlobbererModel.LAYER_LOCATION,
            RedSlobbererModel::createBodyLayer
        )

        event.registerLayerDefinition(
            BabyRedSlobbererModel.LAYER_LOCATION,
            BabyRedSlobbererModel::createBodyLayer
        )


        //Render layer
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
        event.put(RED_SLOBBERER.get(), RedSlobbererEntity.createAttributes().build())
    }

    fun onAddLayers(event: EntityRenderersEvent.AddLayers) {
        val spikeModel = GuardianSpikeModel(event.entityModels.bakeLayer(GuardianSpikeModel.LAYER_LOCATION))
        val lampModel = NautilusLampModel(event.entityModels.bakeLayer(NautilusLampModel.LAYER_LOCATION))
        val bubbleModel = NautilusBubbleSpitterModel(event.entityModels.bakeLayer(NautilusBubbleSpitterModel.LAYER_LOCATION))
        val chestModel = NautilusChestModel(event.entityModels.bakeLayer(NautilusChestModel.LAYER_LOCATION))

        val modelSet = event.context.modelSet

        val conduitCage = modelSet.bakeLayer(ModelLayers.CONDUIT_CAGE)
        val conduitWind = modelSet.bakeLayer(ModelLayers.CONDUIT_WIND)
        val conduitEye = modelSet.bakeLayer(ModelLayers.CONDUIT_EYE)

        @Suppress("UNCHECKED_CAST")
        fun LivingEntityRenderer<*, *, *>.cast() = this as LivingEntityRenderer<LivingEntity, LivingEntityRenderState, EntityModel<LivingEntityRenderState>>
        fun LivingEntityRenderer<*, *, *>.addSpikes() = cast().addLayer(GuardianSpikesLayer(cast(), spikeModel))
        fun LivingEntityRenderer<*, *, *>.addNautilus() = cast().addLayer(
            NautilusLayer(cast(), lampModel, bubbleModel, chestModel, conduitCage, conduitWind, conduitEye)
        )
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
        event.register(
            BRINE.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.OCEAN_FLOOR,
            ::checkBrineSpawn,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        )
        event.register(
            RED_SLOBBERER.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.OCEAN_FLOOR,
            ::checkRedSlobbererSpawn,
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
        if (
            pos.y > ModServerConfig.BARNACLE_SPAWN_MAX_Y.get() ||
            !level.getBiome(pos).`is`(ModTags.Biomes.IS_ABYSSAL) ||
            !level.getFluidState(pos).`is`(FluidTags.WATER)
        ) {
            return false
        }

        if (reason != EntitySpawnReason.NATURAL) return true
        if (random.nextInt(BARNACLE_NATURAL_SPAWN_ROLL) != 0) return false

        return isBelowLocalSpawnCap(
            level,
            pos,
            BarnacleEntity::class.java,
            BARNACLE_LOCAL_DENSITY_RADIUS,
            BARNACLE_LOCAL_DENSITY_CAP
        )
    }

    private fun checkBrineSpawn(
        entityType: EntityType<BrineEntity>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (
            !ModServerConfig.BRINE_NATURAL_SPAWNING.get() ||
            !level.getBiome(pos).`is`(BiomeTags.IS_DEEP_OCEAN) ||
            !level.getFluidState(pos).`is`(FluidTags.WATER)
        ) {
            return false
        }

        if (reason != EntitySpawnReason.NATURAL) return true
        if (level.difficulty == Difficulty.PEACEFUL) return false
        if (!Monster.isDarkEnoughToSpawn(level, pos, random)) return false
        if (random.nextInt(BRINE_NATURAL_SPAWN_ROLL) != 0) return false

        return findBrineSpawnPosition(level, pos, entityType) != null
    }

    private fun checkRedSlobbererSpawn(
        entityType: EntityType<RedSlobbererEntity>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        return findRedSlobbererSpawnPosition(level, pos, entityType) != null
    }

    internal fun findRedSlobbererSpawnPosition(
        level: LevelReader,
        candidate: BlockPos,
        entityType: EntityType<*>
    ): BlockPos? = findWaterSpawnPositionAboveOceanFloor(
        level,
        candidate,
        entityType,
        RED_SLOBBERER_MINIMUM_SPAWN_HEIGHT_ABOVE_FLOOR..RED_SLOBBERER_MAXIMUM_SPAWN_HEIGHT_ABOVE_FLOOR,
        RED_SLOBBERER_REQUIRED_WATER_BLOCKS
    )

    internal fun findBrineSpawnPosition(
        level: LevelReader,
        candidate: BlockPos,
        entityType: EntityType<*>
    ): BlockPos? = findWaterSpawnPositionAboveOceanFloor(
        level,
        candidate,
        entityType,
        BRINE_MINIMUM_SPAWN_HEIGHT_ABOVE_FLOOR..BRINE_MAXIMUM_SPAWN_HEIGHT_ABOVE_FLOOR,
        BRINE_REQUIRED_WATER_BLOCKS
    )

    private fun findWaterSpawnPositionAboveOceanFloor(
        level: LevelReader,
        candidate: BlockPos,
        entityType: EntityType<*>,
        heightAboveFloor: IntRange,
        requiredWaterBlocks: Int
    ): BlockPos? {
        if (!level.worldBorder.isWithinBounds(candidate)) return null
        if (!level.getFluidState(candidate).`is`(FluidTags.WATER)) return null

        val floorPos = findLocalWaterFloor(level, candidate) ?: return null

        for (height in heightAboveFloor) {
            val spawnPos = floorPos.above(height)
            if (!level.worldBorder.isWithinBounds(spawnPos)) continue
            if (
                (0 until requiredWaterBlocks).any { verticalOffset ->
                    !level.getFluidState(spawnPos.above(verticalOffset)).`is`(FluidTags.WATER)
                }
            ) {
                continue
            }

            val spawnBox = entityType.getSpawnAABB(
                spawnPos.x + 0.5,
                spawnPos.y.toDouble(),
                spawnPos.z + 0.5
            )
            if (level.noCollision(spawnBox)) return spawnPos
        }

        return null
    }

    private fun <T : LivingEntity> isBelowLocalSpawnCap(
        level: ServerLevelAccessor,
        pos: BlockPos,
        entityClass: Class<T>,
        radius: Double,
        cap: Int
    ): Boolean {
        val searchBounds = AABB.ofSize(
            Vec3.atCenterOf(pos),
            radius * 2.0,
            radius * 2.0,
            radius * 2.0
        )
        return level.level.getEntitiesOfClass(entityClass, searchBounds) { it.isAlive }.size < cap
    }
}
