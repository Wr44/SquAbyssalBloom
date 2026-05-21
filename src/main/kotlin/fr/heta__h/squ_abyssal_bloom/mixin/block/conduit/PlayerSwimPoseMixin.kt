package fr.heta__h.squ_abyssal_bloom.mixin.block.conduit

import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Player::class)
abstract class PlayerSwimPoseMixin {

    @Inject(method = ["updatePlayerPose"], at = [At("TAIL")])
    private fun preventSwimmingInConduitDomain(ci: CallbackInfo) {
        val player = this as Player
        if (!player.hasEffect(MobEffects.CONDUIT_POWER)) return
        if (player.pose == Pose.SWIMMING) player.pose = Pose.STANDING
    }
}