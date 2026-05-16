@file:Suppress("DEPRECATION")

package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.mixin.enable.ConduitBlockEntityAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object ConduitDomainHandler {

    private const val RADIUS_MIN = 16.0
    private const val RADIUS_MAX = 32.0

    private const val STEP_MIN = 2
    private const val STEP_MAX = 6

    private const val EFFECT_DURATION = 300
    private const val EFFECT_REFRESH_THRESHOLD = 240

    private const val FLYING_SPEED_NORMAL = 0.025f
    private const val FLYING_SPEED_HUNTING = 0.05f
    private const val JUMP_VELOCITY_MAX = 0.42

    private const val BOUNDARY_TRIGGER_DISTANCE = 5.0
    private const val BOUNDARY_RING_RADIUS = 2.5
    private const val BOUNDARY_RING_POINTS = 24
    private const val BOUNDARY_PARTICLE_INTERVAL = 3

    private data class DomainInfo(val conduitPos: BlockPos, val radius: Double, val isHunting: Boolean)

    private val conduitRegistry = ConcurrentHashMap<Level, MutableSet<BlockPos>>()
    private val playerAttachment = ConcurrentHashMap<UUID, BlockPos>()

    private fun getConduits(level: Level) = conduitRegistry.getOrPut(level) { ConcurrentHashMap.newKeySet() }

    private fun conduitRadius(size: Int): Double {
        val steps = size / 7
        val t = ((steps - STEP_MIN).toDouble() / (STEP_MAX - STEP_MIN)).coerceIn(0.0, 1.0)
        return RADIUS_MIN + t * (RADIUS_MAX - RADIUS_MIN)
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
            playerAttachment.values.removeIf { it == event.pos }
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

    private fun detach(player: Player) {
        playerAttachment.remove(player.uuid)
        revokeFlight(player)
        player.removeEffect(MobEffects.CONDUIT_POWER)
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return

        val domain = findActiveDomain(player)

        if (!player.isUnderWater || domain == null) {
            detach(player)
            return
        }

        player.airSupply = player.maxAirSupply

        val targetAmplifier = if (domain.isHunting) 1 else 0
        val currentEffect = player.getEffect(MobEffects.CONDUIT_POWER)
        if (currentEffect == null || currentEffect.amplifier != targetAmplifier || currentEffect.duration <= EFFECT_REFRESH_THRESHOLD) {
            player.addEffect(MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_DURATION, targetAmplifier, true, false))
        }

        if (!player.abilities.flying && player.deltaMovement.y > JUMP_VELOCITY_MAX) {
            player.setDeltaMovement(player.deltaMovement.x, JUMP_VELOCITY_MAX, player.deltaMovement.z)
        }

        grantFlight(player, domain.isHunting)

        if (player.tickCount % BOUNDARY_PARTICLE_INTERVAL == 0) {
            spawnBoundaryWarning(player, domain.conduitPos, domain.radius)
        }
    }

    private fun findActiveDomain(player: Player): DomainInfo? {
        val level = player.level()

        val attachedPos = playerAttachment[player.uuid]
        if (attachedPos != null) {
            val be = level.getBlockEntity(attachedPos) as? ConduitBlockEntity
            if (be != null && be.isActive) {
                val size = (be as ConduitBlockEntityAccessor).getEffectBlocks().size
                val radius = conduitRadius(size)
                if (attachedPos.closerThan(player.blockPosition(), radius))
                    return DomainInfo(attachedPos, radius, be.isHunting)
            }
            playerAttachment.remove(player.uuid)
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
                playerAttachment[player.uuid] = pos
                return DomainInfo(pos, radius, be.isHunting)
            }
        }
        return null
    }

    private fun spawnBoundaryWarning(player: Player, conduitPos: BlockPos, radius: Double) {
        val cx = conduitPos.x + 0.5
        val cy = conduitPos.y + 0.5
        val cz = conduitPos.z + 0.5

        val eyePos = player.eyePosition
        val vx = eyePos.x - cx
        val vy = eyePos.y - cy
        val vz = eyePos.z - cz

        val dist = sqrt(vx * vx + vy * vy + vz * vz)
        if (dist <= 0.001 || radius - dist > BOUNDARY_TRIGGER_DISTANCE) return

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

        repeat(BOUNDARY_RING_POINTS) { i ->
            val phi = 2.0 * Math.PI * i / BOUNDARY_RING_POINTS
            val cosPhi = cos(phi); val sinPhi = sin(phi)
            serverLevel.sendParticles(
                ParticleTypes.NAUTILUS,
                bx + (cosPhi * rx + sinPhi * ux) * BOUNDARY_RING_RADIUS,
                by + (cosPhi * ry + sinPhi * uy) * BOUNDARY_RING_RADIUS,
                bz + (cosPhi * rz + sinPhi * uz) * BOUNDARY_RING_RADIUS,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }
    }

    private fun grantFlight(player: Player, isHunting: Boolean) {
        if (player.isCreative || player.isSpectator) return
        val abilities = player.abilities
        val targetSpeed = if (isHunting) FLYING_SPEED_HUNTING else FLYING_SPEED_NORMAL
        if (!abilities.mayfly) {
            abilities.mayfly = true
            abilities.flying = true
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
        abilities.mayfly = false
        abilities.flying = false
        player.onUpdateAbilities()
    }
}