package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractNautilus::class)
abstract class NautilusDashBubble {

    @Inject(method = ["onPlayerJump"], at = [At("HEAD")], cancellable = true)
    fun onJumpHeld(jumpPower: Int, ci: CallbackInfo) {
        val self = this as AbstractNautilus
        if (!isBubbleMode(self)) return
        ci.cancel()
    }

    @Inject(method = ["handleStartJump"], at = [At("HEAD")], cancellable = true)
    fun onJumpFired(jumpPower: Int, ci: CallbackInfo) {
        val self = this as AbstractNautilus
        if (!isBubbleMode(self)) return
        ci.cancel()

        if (self.level().isClientSide) return
        val level = self.level() as? ServerLevel ?: return

        val bubble = level.getEntitiesOfClass(
            BubbleProjectile::class.java, self.boundingBox.inflate(3.0)
        ) { it.isHeld && it.owner == self }.firstOrNull() ?: return

        val ratio = (bubble.holdTicks.toDouble() / BubbleProjectile.TICKS_TO_OVERCHARGE).coerceIn(0.0, 1.0)
        val controller = self.controllingPassenger ?: self
        val look = controller.lookAngle

        bubble.release(look.scale(0.15 + ratio * 1.35))

        val recoilStrength = 0.6 + (2.2 * ratio)

        val recoilVec = Vec3(
            -look.x * recoilStrength,
            0.3,
            -look.z * recoilStrength
        )

        self.deltaMovement = self.deltaMovement.add(recoilVec)
        self.hurtMarked = true

        val rider = self.controllingPassenger
        if (rider is ServerPlayer) {
            rider.deltaMovement = rider.deltaMovement.add(recoilVec)
            rider.hurtMarked = true

            rider.connection.send(ClientboundSetEntityMotionPacket(self))
            rider.connection.send(ClientboundSetEntityMotionPacket(rider))
        }

        self.isDashing = true
    }

    @Inject(method = ["executeRidersJump"], at = [At("HEAD")], cancellable = true)
    fun cancelDash(scale: Float, player: Player, ci: CallbackInfo) {
        val self = this as AbstractNautilus
        if (!isBubbleMode(self)) return
        ci.cancel()
    }

    private fun isBubbleMode(entity: AbstractNautilus) = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item == NautilusLayerItems.BUBBLE
}
