package fr.heta__h.squ_abyssal_bloom.network.bubble

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.neoforge.network.handling.IPayloadContext
import kotlin.math.cos
import kotlin.math.sin

object C2SBubbleChargeStartPacket : CustomPacketPayload {

    val ID = CustomPacketPayload.Type<C2SBubbleChargeStartPacket>(
        Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "bubble_charge_start")
    )

    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2SBubbleChargeStartPacket> =
        StreamCodec.unit(this)

    override fun type() = ID

    fun handle(payload: C2SBubbleChargeStartPacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player() as? ServerPlayer ?: return@enqueueWork
            val nautilus = player.vehicle as? AbstractNautilus ?: return@enqueueWork

            if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item != NautilusLayer.BUBBLE) return@enqueueWork
            if (nautilus.getJumpCooldown() > 0) return@enqueueWork

            val level = nautilus.level() as? ServerLevel ?: return@enqueueWork

            val alreadyHeld = level.getEntitiesOfClass(
                BubbleProjectile::class.java,
                nautilus.boundingBox.inflate(3.0)
            ) { it.isHeld && it.owner == nautilus }.isNotEmpty()

            if (alreadyHeld) return@enqueueWork

            val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), level)
            bubble.owner = nautilus
            bubble.isHeld = true

            val controller = nautilus.controllingPassenger ?: nautilus
            val yawRad = Math.toRadians(controller.yRot.toDouble())
            val dirX = -sin(yawRad)
            val dirZ = cos(yawRad)

            bubble.setPos(
                nautilus.x + dirX * 1.3,
                nautilus.y + nautilus.bbHeight / 3.0,
                nautilus.z + dirZ * 1.3
            )

            level.addFreshEntity(bubble)

            level.playSound(
                null,
                nautilus.x,
                nautilus.y,
                nautilus.z,
                ModSounds.BUBBLE_PROJECTILE_STAGE_UP.get(),
                SoundSource.HOSTILE,
                0.8f,
                1.2f
            )
        }
    }
}