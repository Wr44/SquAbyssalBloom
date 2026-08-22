@file:Suppress("DEPRECATION")

package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.mixin.enable.ConduitBlockEntityAccessor
import fr.heta__h.squ_abyssal_bloom.network.conduit.S2CConduitDomainPayload
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.conduit.AstralPrismarineTracker
import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitHuntingTracker
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ConduitBlockEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.BlockDropsEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.level.block.BreakBlockEvent
import net.neoforged.neoforge.event.level.ChunkEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object ConduitDomainHandler {

    private const val RADIUS_MIN = 16.0
    private const val RADIUS_MAX = 36.0

    const val PORTABLE_RADIUS = 12.0

    private const val STEP_MIN = 2
    private const val STEP_MAX = 6

    private const val EFFECT_DURATION = 400
    private const val EFFECT_REFRESH_THRESHOLD = 300

    private const val FLYING_SPEED_NORMAL = 0.035f
    private const val FLYING_SPEED_HUNTING = 0.05f

    private const val JUMP_VELOCITY_MAX = 0.42

    private const val CONDUIT_ACTIVE_THRESHOLD = 40

    private sealed class ConduitTarget {
        data class Block(val pos: BlockPos) : ConduitTarget()
        data class Entity(val uuid: UUID) : ConduitTarget()
    }

    private data class DomainInfo(
        val target: ConduitTarget,
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val radius: Double,
        val isHunting: Boolean,
        val entityId: Int = -1
    )

    data class FlightLease(
        var restoreMayfly: Boolean,
        var restoreFlying: Boolean,
        var restoreFlyingSpeed: Float,
        var appliedMayfly: Boolean? = null,
        var appliedFlying: Boolean? = null,
        var appliedFlyingSpeed: Float? = null,
    )

    private val conduitRegistry = ConcurrentHashMap<Level, MutableSet<BlockPos>>()
    private val playerAttachment = ConcurrentHashMap<UUID, ConduitTarget>()
    private val playerNextAmbientSound = ConcurrentHashMap<UUID, Long>()
    private val pendingConduitEquipmentChange = ConcurrentHashMap.newKeySet<UUID>()
    private val lastSentDomain = ConcurrentHashMap<UUID, S2CConduitDomainPayload>()
    private val flightLeases = ConcurrentHashMap<UUID, FlightLease>()

    private fun getConduits(level: Level) = conduitRegistry.getOrPut(level) { ConcurrentHashMap.newKeySet() }

    private fun conduitRadius(size: Int): Double {
        val steps = size / 7
        val t = ((steps - STEP_MIN).toDouble() / (STEP_MAX - STEP_MIN)).coerceIn(0.0, 1.0)
        return RADIUS_MIN + t * (RADIUS_MAX - RADIUS_MIN)
    }

    fun markConduitEquipmentChange(playerUUID: UUID) {
        pendingConduitEquipmentChange.add(playerUUID)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        ConduitHuntingTracker.clearHits()

        if (event.server.tickCount % 200 == 0) {
            for (level in event.server.allLevels) {
                AstralPrismarineTracker.checkOrphans(level as? ServerLevel ?: continue)
            }

            val onlinePlayers = event.server.playerList.players.map { it.uuid }.toSet()
            playerAttachment.keys.removeIf { it !in onlinePlayers }
            lastSentDomain.keys.removeIf { it !in onlinePlayers }
            playerNextAmbientSound.keys.removeIf { it !in onlinePlayers }
            pendingConduitEquipmentChange.removeIf { it !in onlinePlayers }
            flightLeases.keys.removeIf { it !in onlinePlayers }
        }
    }

    @SubscribeEvent
    fun onServerStopped(event: ServerStoppedEvent) {
        conduitRegistry.clear()
        playerAttachment.clear()
        playerNextAmbientSound.clear()
        pendingConduitEquipmentChange.clear()
        lastSentDomain.clear()
        flightLeases.clear()
    }

    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? Level ?: return
        if (event.placedBlock.block == Blocks.CONDUIT) getConduits(level).add(event.pos)
    }

    @SubscribeEvent
    fun onConduitBreak(event: BreakBlockEvent) {
        if (event.state.block != Blocks.CONDUIT) return
        val level = event.level as? ServerLevel ?: return

        val wasActive = (level.getBlockEntity(event.pos) as? ConduitBlockEntity)?.isActive == true
        if (wasActive) {
            level.playSound(event.player, event.pos, SoundEvents.CONDUIT_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f)
        }

        deactivateAstralBlocks(level, event.pos)
    }

    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load) {
        val level = event.level as? Level ?: return
        event.chunk.blockEntities.keys
            .filter { level.getBlockState(it).block == Blocks.CONDUIT }
            .forEach { getConduits(level).add(it) }

        AstralPrismarineTracker.onChunkLoad(event.chunk)
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        val level = event.level as? Level ?: return
        conduitRegistry.remove(level)
        if (level is ServerLevel) {
            AstralPrismarineTracker.releaseLevel(level)
        }
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        detach(event.entity)
        lastSentDomain.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onPlayerDeath(event: LivingDeathEvent) {
        val player = event.entity as? Player ?: return
        detach(player)
    }

    @SubscribeEvent
    fun onPlayerChangeDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        detach(event.entity)
    }

    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        detach(event.entity)
    }

    private fun detach(player: Player, previousAttachment: ConduitTarget? = null) {
        val wasInDomain = player.hasEffect(MobEffects.CONDUIT_POWER)
        val attachedTarget = previousAttachment ?: playerAttachment[player.uuid]

        playerAttachment.remove(player.uuid)
        playerNextAmbientSound.remove(player.uuid)
        pendingConduitEquipmentChange.remove(player.uuid)
        revokeFlight(player)

        if (!player.level().isClientSide) {
            syncDomain(player, S2CConduitDomainPayload.NONE)
            player.removeEffect(MobEffects.CONDUIT_POWER)

            if (wasInDomain && attachedTarget != null) {
                val soundToPlay = when (attachedTarget) {
                    is ConduitTarget.Block -> {
                        val be = player.level().getBlockEntity(attachedTarget.pos) as? ConduitBlockEntity
                        if (be?.isActive == true) ModSounds.CONDUIT_LEAVING.get() else null
                    }
                    is ConduitTarget.Entity -> ModSounds.CONDUIT_LEAVING.get()
                }

                soundToPlay?.let {
                    ModUtilities.playSoundLocal(player, it, SoundSource.PLAYERS, 1.5f, 1f)
                }
            }
        }
    }

    @SubscribeEvent
    fun onBreakSpeed(event: PlayerEvent.BreakSpeed) {
        val player = event.entity
        if (!player.isUnderWater) return
        val effect = player.getEffect(MobEffects.CONDUIT_POWER) ?: return
        val multiplier = if (effect.amplifier == 0) 2f else 3f
        event.newSpeed = event.originalSpeed * multiplier
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity

        if (player.level().isClientSide) return

        val previousAttachment = playerAttachment[player.uuid]
        val domain = findActiveDomain(player)

        if (!player.isUnderWater || domain == null) {
            if (previousAttachment is ConduitTarget.Entity && pendingConduitEquipmentChange.remove(player.uuid)) {
                playerAttachment.remove(player.uuid)
                playerNextAmbientSound.remove(player.uuid)
                revokeFlight(player)
                if (!player.level().isClientSide) {
                    syncDomain(player, S2CConduitDomainPayload.NONE)
                    player.removeEffect(MobEffects.CONDUIT_POWER)
                }
                return
            }
            detach(player, previousAttachment)
            return
        }

        val currentEffect = player.getEffect(MobEffects.CONDUIT_POWER)
        val justEntered = currentEffect == null

        grantFlight(player, domain.isHunting)

        player.airSupply = player.maxAirSupply

        val targetAmplifier = if (domain.isHunting) 1 else 0
        if (currentEffect == null || currentEffect.amplifier != targetAmplifier || currentEffect.duration <= EFFECT_REFRESH_THRESHOLD) {
            player.addEffect(MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_DURATION, targetAmplifier, true, false))
        }

        if (justEntered) {
            val isEquipmentChange = domain.target is ConduitTarget.Entity && pendingConduitEquipmentChange.remove(player.uuid)

            if (!isEquipmentChange) {
                val soundToPlay = when (val target = domain.target) {
                    is ConduitTarget.Block -> {
                        val be = player.level().getBlockEntity(target.pos) as? ConduitBlockEntity
                        if ((be?.tickCount ?: 0) > CONDUIT_ACTIVE_THRESHOLD) ModSounds.CONDUIT_ENTERING.get() else null
                    }
                    is ConduitTarget.Entity -> ModSounds.CONDUIT_ENTERING.get()
                }

                soundToPlay?.let {
                    ModUtilities.playSoundLocal(player, it, SoundSource.PLAYERS, 1.0f, 1.0f)
                }
            }
        }

        if (domain.target is ConduitTarget.Entity) {
            val gameTime = player.level().gameTime

            if (gameTime % 80L == 0L) {
                ModUtilities.playSoundLocal(
                    player,
                    SoundEvents.CONDUIT_AMBIENT,
                    SoundSource.PLAYERS,
                    1.0f, 1.0f
                )
            }

            val nextSound = playerNextAmbientSound[player.uuid] ?: 0L
            if (gameTime > nextSound) {
                playerNextAmbientSound[player.uuid] = gameTime + 60L + player.level().random.nextInt(40).toLong()
                ModUtilities.playSoundLocal(
                    player,
                    SoundEvents.CONDUIT_AMBIENT_SHORT,
                    SoundSource.PLAYERS,
                    1.0f, 1.0f
                )
            }
        }

        if (!player.abilities.flying && player.deltaMovement.y > JUMP_VELOCITY_MAX) {
            player.setDeltaMovement(player.deltaMovement.x, JUMP_VELOCITY_MAX, player.deltaMovement.z)
        }

        syncDomain(player, domainPayloadOf(domain))
    }

    private fun findActiveDomain(player: Player): DomainInfo? {
        val level = player.level()
        val attachedTarget = playerAttachment[player.uuid]

        if (attachedTarget != null) {
            when (attachedTarget) {
                is ConduitTarget.Block -> {
                    val be = level.getBlockEntity(attachedTarget.pos) as? ConduitBlockEntity
                    if (be != null && be.isActive) {
                        val size = (be as ConduitBlockEntityAccessor).getEffectBlocks().size
                        val radius = conduitRadius(size)
                        if (attachedTarget.pos.closerThan(player.blockPosition(), radius)) {
                            return DomainInfo(attachedTarget, attachedTarget.pos.x + 0.5, attachedTarget.pos.y + 0.5, attachedTarget.pos.z + 0.5, radius, be.isHunting)
                        }
                    }
                    playerAttachment.remove(player.uuid)
                }
                is ConduitTarget.Entity -> {
                    val nautilusList = level.getEntitiesOfClass(AbstractNautilus::class.java, player.boundingBox.inflate(PORTABLE_RADIUS))
                    val nautilus = nautilusList.firstOrNull { it.uuid == attachedTarget.uuid && it.isAlive && it.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).item == NautilusLayerItems.CONDUIT }
                    if (nautilus != null && nautilus.distanceTo(player) <= PORTABLE_RADIUS) {
                        return DomainInfo(attachedTarget, nautilus.x, nautilus.y + nautilus.bbHeight / 2.0, nautilus.z, PORTABLE_RADIUS, false, nautilus.id)
                    }
                    playerAttachment.remove(player.uuid)
                }
            }
        }

        for (pos in getConduits(level)) {
            val be = level.getBlockEntity(pos) as? ConduitBlockEntity ?: run {
                getConduits(level).remove(pos)
                continue
            }
            if (!be.isActive) continue
            val size = (be as ConduitBlockEntityAccessor).getEffectBlocks().size
            val radius = conduitRadius(size)
            if (pos.closerThan(player.blockPosition(), radius)) {
                val target = ConduitTarget.Block(pos)
                playerAttachment[player.uuid] = target
                return DomainInfo(target, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, radius, be.isHunting)
            }
        }

        val nautilusList = level.getEntitiesOfClass(AbstractNautilus::class.java, player.boundingBox.inflate(PORTABLE_RADIUS))
        val nautilus = nautilusList.firstOrNull { it.isAlive && it.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).item == NautilusLayerItems.CONDUIT }
        if (nautilus != null && nautilus.distanceTo(player) <= PORTABLE_RADIUS) {
            val target = ConduitTarget.Entity(nautilus.uuid)
            playerAttachment[player.uuid] = target
            return DomainInfo(target, nautilus.x, nautilus.y + nautilus.bbHeight / 2.0, nautilus.z, PORTABLE_RADIUS, false, nautilus.id)
        }

        return null
    }

    private fun domainPayloadOf(domain: DomainInfo): S2CConduitDomainPayload {
        return when (val target = domain.target) {
            is ConduitTarget.Block -> S2CConduitDomainPayload.ofBlock(target.pos, domain.radius)
            is ConduitTarget.Entity -> S2CConduitDomainPayload.ofPortable(domain.entityId, domain.radius)
        }
    }

    private fun syncDomain(player: Player, payload: S2CConduitDomainPayload) {
        val serverPlayer = player as? ServerPlayer ?: return
        if (lastSentDomain.put(player.uuid, payload) == payload) return
        PacketDistributor.sendToPlayer(serverPlayer, payload)
    }

    private fun grantFlight(player: Player, isHunting: Boolean) {
        val abilities = player.abilities
        val targetSpeed = if (isHunting) FLYING_SPEED_HUNTING else FLYING_SPEED_NORMAL
        var enteredDomain = false
        val lease = flightLeases[player.uuid] ?: FlightLease(
            restoreMayfly = abilities.mayfly,
            restoreFlying = abilities.flying,
            restoreFlyingSpeed = abilities.flyingSpeed,
        ).also {
            flightLeases[player.uuid] = it
            enteredDomain = true
        }

        var updateNeeded = false

        lease.appliedMayfly?.let { appliedValue ->
            if (abilities.mayfly != appliedValue) {
                lease.restoreMayfly = abilities.mayfly
                lease.appliedMayfly = null
            }
        }
        lease.appliedFlying?.let { appliedValue ->
            if (abilities.flying != appliedValue) {
                lease.restoreFlying = abilities.flying
                lease.appliedFlying = null
            }
        }
        lease.appliedFlyingSpeed?.let { appliedValue ->
            if (abilities.flyingSpeed != appliedValue) {
                lease.restoreFlyingSpeed = abilities.flyingSpeed
                lease.appliedFlyingSpeed = null
            }
        }

        if (!abilities.mayfly && !player.isCreative && !player.isSpectator) {
            if (lease.appliedMayfly == null) lease.restoreMayfly = false
            abilities.mayfly = true
            lease.appliedMayfly = true
            updateNeeded = true
        }

        if (enteredDomain && !abilities.flying && !player.onGround() && !player.isCreative && !player.isSpectator) {
            abilities.flying = true
            lease.appliedFlying = true
            updateNeeded = true
        }

        if (abilities.flyingSpeed != targetSpeed) {
            if (lease.appliedFlyingSpeed == null) lease.restoreFlyingSpeed = abilities.flyingSpeed
            abilities.flyingSpeed = targetSpeed
            lease.appliedFlyingSpeed = targetSpeed
            updateNeeded = true
        }

        if (updateNeeded) {
            player.onUpdateAbilities()
        }
    }

    private fun revokeFlight(player: Player) {
        val lease = flightLeases.remove(player.uuid) ?: return
        val abilities = player.abilities
        val gameModeOwnsFlight = player.isCreative || player.isSpectator
        var updateNeeded = false

        if (!gameModeOwnsFlight) {
            lease.appliedMayfly?.let { appliedValue ->
                if (abilities.mayfly == appliedValue && abilities.mayfly != lease.restoreMayfly) {
                    abilities.mayfly = lease.restoreMayfly
                    updateNeeded = true
                }
            }
            lease.appliedFlying?.let { appliedValue ->
                if (abilities.flying == appliedValue && abilities.flying != lease.restoreFlying) {
                    abilities.flying = lease.restoreFlying
                    updateNeeded = true
                }
            }
        }

        lease.appliedFlyingSpeed?.let { appliedValue ->
            if (abilities.flyingSpeed == appliedValue && abilities.flyingSpeed != lease.restoreFlyingSpeed) {
                abilities.flyingSpeed = lease.restoreFlyingSpeed
                updateNeeded = true
            }
        }

        if (updateNeeded) player.onUpdateAbilities()
    }

    private fun deactivateAstralBlocks(level: ServerLevel, pos: BlockPos) {
        val allPositions = AstralPrismarineTracker.nearbyOffsetPositions(pos)

        allPositions.forEach { checkPos ->

            if (!level.isLoaded(checkPos)) return@forEach

            val chunk = level.getChunk(checkPos)
            val state = chunk.getBlockState(checkPos)
            if (state.`is`(ModBlocks.ASTRAL_PRISMARINE) &&
                state.getValue(AstralPrismarineBlock.ACTIVE)) {
                level.setBlock(checkPos, state.setValue(AstralPrismarineBlock.ACTIVE, false), 3)
                AstralPrismarineTracker.markInactive(level, checkPos)
            }
        }
    }
}
