package fr.heta__h.squ_abyssal_bloom.event.barnacle

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.config.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import net.minecraft.world.entity.LivingEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.DEDICATED_SERVER])
object SpawnPreventionEvent {
    private val LOGGER: Logger = LogManager.getLogger(SquAbyssalBloom.ID)

    @SubscribeEvent
    fun onPositionCheck(event: MobSpawnEvent.PositionCheck) {
        if (!ServerConfigCache.effectiveStrictBarnacleSpawning) return

        val entity = event.entity
        val entityName = entity.type.description.string
        val registryName = entity.type.toString()

        if (entityName.contains("Barnacle", ignoreCase = true) ||
            registryName.contains("barnacle", ignoreCase = true)) {

            if (entity.type != ModEntities.BARNACLE.get()) {
                LOGGER.warn(
                    "Prevention of unauthorized Barnacle entity spawn detected: {}", entityName
                )

                event.result = MobSpawnEvent.PositionCheck.Result.FAIL
            }
        }
    }


    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Pre) {
        if (!ServerConfigCache.effectiveStrictBarnacleSpawning) return

        val entity = event.entity

        if (entity !is LivingEntity) return

        if (entity.level().isClientSide) return

        val entityName = entity.type.description.string
        val registryName = entity.type.toString()

        if ((entityName.contains("Barnacle", ignoreCase = true) ||
                    registryName.contains("barnacle", ignoreCase = true)) &&
            entity.type != ModEntities.BARNACLE.get()) {

            LOGGER.warn("Removal of unauthorized Barnacle entity detected: {}", entityName)
            entity.discard()
        }
    }
}