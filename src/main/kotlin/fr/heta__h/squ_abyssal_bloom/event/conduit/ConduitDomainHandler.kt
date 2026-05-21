@file:Suppress("DEPRECATION")

package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.mixin.enable.ConduitBlockEntityAccessor
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitHuntingTracker
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
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
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.level.ChunkEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object ConduitDomainHandler {

    private const val RADIUS_MIN = 16.0
    private const val RADIUS_MAX = 36.0

    const val PORTABLE_RADIUS = 12.0
    private const val PORTABLE_WARNING_DISTANCE = 3.5

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
        val isHunting: Boolean
    )

    private val conduitRegistry = ConcurrentHashMap<Level, MutableSet<BlockPos>>()
    private val playerAttachment = ConcurrentHashMap<UUID, ConduitTarget>()

    private fun getConduits(level: Level) = conduitRegistry.getOrPut(level) { ConcurrentHashMap.newKeySet() }

    private fun conduitRadius(size: Int): Double {
        val steps = size / 7
        val t = ((steps - STEP_MIN).toDouble() / (STEP_MAX - STEP_MIN)).coerceIn(0.0, 1.0)
        return RADIUS_MIN + t * (RADIUS_MAX - RADIUS_MIN)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        ConduitHuntingTracker.clearHits()
    }

    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? Level ?: return
        if (event.placedBlock.block == Blocks.CONDUIT) getConduits(level).add(event.pos)
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level as? Level ?: return
        if (event.state.block == Blocks.CONDUIT) {
            getConduits(level).remove(event.pos)
            playerAttachment.values.removeIf { it is ConduitTarget.Block && it.pos == event.pos }
        }
    }

    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load) {
        val level = event.level as? Level ?: return
        event.chunk.blockEntities.keys
            .filter { level.getBlockState(it).block == Blocks.CONDUIT }
            .forEach { getConduits(level).add(it) }
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        val level = event.level as? Level ?: return
        conduitRegistry.remove(level)
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) { detach(event.entity) }

    @SubscribeEvent
    fun onPlayerDeath(event: LivingDeathEvent) {
        val player = event.entity as? Player ?: return
        detach(player)
    }

    @SubscribeEvent
    fun onPlayerChangeDimension(event: PlayerEvent.PlayerChangedDimensionEvent) { detach(event.entity) }

    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) { detach(event.entity) }

    private fun detach(player: Player, previousAttachment: ConduitTarget? = null) {
        val wasInDomain = player.hasEffect(MobEffects.CONDUIT_POWER)
        val attachedTarget = previousAttachment ?: playerAttachment[player.uuid]

        playerAttachment.remove(player.uuid)
        revokeFlight(player)
        player.removeEffect(MobEffects.CONDUIT_POWER)

        if (wasInDomain && attachedTarget != null) {
            var shouldPlaySound = false
            when (attachedTarget) {
                is ConduitTarget.Block -> {
                    val be = player.level().getBlockEntity(attachedTarget.pos) as? ConduitBlockEntity
                    if (be?.isActive == true) shouldPlaySound = true
                }
                is ConduitTarget.Entity -> {
                    shouldPlaySound = true
                }
            }
            if (shouldPlaySound) {
                ModUtilities.playSoundLocal(
                    player,
                    SoundEvents.CONDUIT_DEACTIVATE,
                    SoundSource.PLAYERS,
                    0.5f, 1.5f
                )
            }
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return

        val previousAttachment = playerAttachment[player.uuid]
        val domain = findActiveDomain(player)

        if (!player.isUnderWater || domain == null) {
            detach(player, previousAttachment)
            return
        }

        player.airSupply = player.maxAirSupply
        if (player.isSwimming) player.isSwimming = false

        val currentEffect = player.getEffect(MobEffects.CONDUIT_POWER)
        val justEntered = currentEffect == null

        val targetAmplifier = if (domain.isHunting) 1 else 0
        if (currentEffect == null || currentEffect.amplifier != targetAmplifier || currentEffect.duration <= EFFECT_REFRESH_THRESHOLD) {
            player.addEffect(MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_DURATION, targetAmplifier, true, false))
        }

        if (justEntered) {
            var shouldPlaySound = false
            when (val target = domain.target) {
                is ConduitTarget.Block -> {
                    val be = player.level().getBlockEntity(target.pos) as? ConduitBlockEntity
                    if ((be?.tickCount ?: 0) > CONDUIT_ACTIVE_THRESHOLD) shouldPlaySound = true
                }
                is ConduitTarget.Entity -> {
                    shouldPlaySound = true
                }
            }
            if (shouldPlaySound) {
                ModUtilities.playSoundLocal(
                    player,
                    SoundEvents.CONDUIT_ACTIVATE,
                    SoundSource.PLAYERS,
                    0.5f, 1.5f
                )
            }
        }

        if (!player.abilities.flying && player.deltaMovement.y > JUMP_VELOCITY_MAX) {
            player.setDeltaMovement(player.deltaMovement.x, JUMP_VELOCITY_MAX, player.deltaMovement.z)
        }

        grantFlight(player, domain.isHunting)

        if (ModConfig.conduitBoundaryParticlesEnabled && player.tickCount % ModConfig.conduitBoundaryParticleInterval == 0) {
            val isPortable = domain.target is ConduitTarget.Entity
            spawnBoundaryWarning(player, domain.centerX, domain.centerY, domain.centerZ, domain.radius, isPortable)
        }
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
                    val nautilus = nautilusList.firstOrNull { it.uuid == attachedTarget.uuid && it.isAlive && it.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).item == NautilusLayer.CONDUIT }
                    if (nautilus != null && nautilus.distanceTo(player) <= PORTABLE_RADIUS) {
                        return DomainInfo(attachedTarget, nautilus.x, nautilus.y + nautilus.bbHeight / 2.0, nautilus.z, PORTABLE_RADIUS, false)
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
        val nautilus = nautilusList.firstOrNull { it.isAlive && it.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).item == NautilusLayer.CONDUIT }
        if (nautilus != null && nautilus.distanceTo(player) <= PORTABLE_RADIUS) {
            val target = ConduitTarget.Entity(nautilus.uuid)
            playerAttachment[player.uuid] = target
            return DomainInfo(target, nautilus.x, nautilus.y + nautilus.bbHeight / 2.0, nautilus.z, PORTABLE_RADIUS, false)
        }

        return null
    }

    private fun spawnBoundaryWarning(player: Player, cx: Double, cy: Double, cz: Double, radius: Double, isPortable: Boolean) {
        val eyePos = player.eyePosition
        val vx = eyePos.x - cx
        val vy = eyePos.y - cy
        val vz = eyePos.z - cz

        val dist = sqrt(vx * vx + vy * vy + vz * vz)

        val triggerDist = if (isPortable) PORTABLE_WARNING_DISTANCE else ModConfig.conduitBoundaryTriggerDistance
        if (dist <= 0.001 || radius - dist > triggerDist) return

        val nx = vx / dist; val ny = vy / dist; val nz = vz / dist
        val bx = cx + nx * radius; val by = cy + ny * radius; val bz = cz + nz * radius

        val serverLevel = player.level() as? ServerLevel ?: return

        val rx: Double; val ry: Double; val rz: Double
        if (abs(ny) > 0.99) {
            rx = 1.0; ry = 0.0; rz = 0.0
        } else {
            val rLen = sqrt(nx * nx + nz * nz).coerceAtLeast(1e-6)
            rx = nz / rLen; ry = 0.0; rz = -nx / rLen
        }
        val ux = ny * rz - nz * ry
        val uy = nz * rx - nx * rz
        val uz = nx * ry - ny * rx

        val ringPoints = ModConfig.conduitBoundaryRingPoints
        val ringRadius = if (isPortable) ModConfig.conduitBoundaryRingRadius / 2.0 else ModConfig.conduitBoundaryRingRadius

        repeat(ringPoints) { i ->
            val phi = 2.0 * Math.PI * i / ringPoints
            val cosPhi = cos(phi); val sinPhi = sin(phi)
            serverLevel.sendParticles(
                ParticleTypes.NAUTILUS,
                bx + (cosPhi * rx + sinPhi * ux) * ringRadius,
                by + (cosPhi * ry + sinPhi * uy) * ringRadius,
                bz + (cosPhi * rz + sinPhi * uz) * ringRadius,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }
    }

    private fun grantFlight(player: Player, isHunting: Boolean) {
        if (player.isCreative || player.isSpectator) return
        val abilities = player.abilities
        val targetSpeed = if (isHunting) FLYING_SPEED_HUNTING else FLYING_SPEED_NORMAL
        if (!abilities.mayfly) {
            abilities.apply {
                mayfly = true
                flying = true
            }
            abilities.flyingSpeed = targetSpeed
            player.onUpdateAbilities()
        } else if (abilities.flyingSpeed != targetSpeed) {
            abilities.flyingSpeed = targetSpeed
            player.onUpdateAbilities()
        }
    }

    private fun revokeFlight(player: Player) {
        if (player.isCreative || player.isSpectator) return
        val abilities = player.abilities
        if (!abilities.mayfly) return
        abilities.apply {
            mayfly = false
            flying = false
        }
        player.onUpdateAbilities()
    }
}