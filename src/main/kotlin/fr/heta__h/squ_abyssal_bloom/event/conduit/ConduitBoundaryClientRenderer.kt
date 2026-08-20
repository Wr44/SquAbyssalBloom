package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.network.conduit.ConduitDomainKind
import fr.heta__h.squ_abyssal_bloom.network.conduit.S2CConduitDomainPayload
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object ConduitBoundaryClientRenderer {

    private const val PORTABLE_WARNING_DISTANCE = 3.5

    private var domain: S2CConduitDomainPayload? = null

    fun applyDomain(payload: S2CConduitDomainPayload) {
        domain = if (payload.kind == ConduitDomainKind.NONE) null else payload
    }

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        domain = null
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.isPaused) return
        val level = minecraft.level ?: return
        val player = minecraft.player ?: return
        val currentDomain = domain ?: return
        if (!ModConfig.enableConduitBoundaryParticles) return
        val interval = ModConfig.conduitBoundaryParticleInterval.coerceAtLeast(1)
        if (player.tickCount % interval != 0) return

        val center = centerOf(level, currentDomain) ?: return
        spawnBoundaryWarning(
            level,
            player,
            center,
            currentDomain.radius,
            currentDomain.kind == ConduitDomainKind.PORTABLE
        )
    }

    private fun centerOf(level: ClientLevel, currentDomain: S2CConduitDomainPayload): Vec3? {
        return when (currentDomain.kind) {
            ConduitDomainKind.BLOCK -> Vec3.atCenterOf(currentDomain.blockPos)
            ConduitDomainKind.PORTABLE -> {
                val entity = level.getEntity(currentDomain.entityId) ?: return null
                Vec3(entity.x, entity.y + entity.bbHeight / 2.0, entity.z)
            }
            ConduitDomainKind.NONE -> null
        }
    }

    private fun spawnBoundaryWarning(
        level: ClientLevel,
        player: Player,
        center: Vec3,
        radius: Double,
        isPortable: Boolean
    ) {
        val eyePos = player.eyePosition
        val vx = eyePos.x - center.x
        val vy = eyePos.y - center.y
        val vz = eyePos.z - center.z

        val dist = sqrt(vx * vx + vy * vy + vz * vz)

        val triggerDist = if (isPortable) PORTABLE_WARNING_DISTANCE else ModConfig.conduitBoundaryTriggerDistance
        if (dist <= 0.001 || radius - dist > triggerDist) return

        val nx = vx / dist
        val ny = vy / dist
        val nz = vz / dist
        val bx = center.x + nx * radius
        val by = center.y + ny * radius
        val bz = center.z + nz * radius

        val rx: Double
        val ry: Double
        val rz: Double
        if (abs(ny) > 0.99) {
            val rLen = sqrt(nx * nx + ny * ny)
            rx = ny / rLen
            ry = -nx / rLen
            rz = 0.0
        } else {
            val rLen = sqrt(nx * nx + nz * nz).coerceAtLeast(1e-6)
            rx = nz / rLen
            ry = 0.0
            rz = -nx / rLen
        }
        val ux = ny * rz - nz * ry
        val uy = nz * rx - nx * rz
        val uz = nx * ry - ny * rx

        val ringPoints = ModConfig.conduitBoundaryRingPoints
        val ringRadius = if (isPortable) ModConfig.conduitBoundaryRingRadius / 2.0 else ModConfig.conduitBoundaryRingRadius

        repeat(ringPoints) { i ->
            val phi = 2.0 * Math.PI * i / ringPoints
            val cosPhi = cos(phi)
            val sinPhi = sin(phi)

            val px = bx + (cosPhi * rx + sinPhi * ux) * ringRadius
            val py = by + (cosPhi * ry + sinPhi * uy) * ringRadius
            val pz = bz + (cosPhi * rz + sinPhi * uz) * ringRadius

            if (level.getFluidState(BlockPos.containing(px, py, pz)).`is`(FluidTags.WATER)) {
                level.addParticle(ParticleTypes.NAUTILUS, px, py, pz, 0.0, 0.0, 0.0)
            }
        }
    }
}
