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

    @Inject(method = ["updateSwimming"], at = [At("HEAD")], cancellable = true)
    private fun preventSwimmingPhysics(ci: CallbackInfo) {
        val player = this as Player

        if (player.hasEffect(MobEffects.CONDUIT_POWER)) {
            player.isSwimming = false
            ci.cancel()
        }
    }

    @Inject(method = ["updatePlayerPose"], at = [At("TAIL")])
    private fun preventSwimmingPose(ci: CallbackInfo) {
        val player = this as Player

        if (player.hasEffect(MobEffects.CONDUIT_POWER)) {
            if (player.pose == Pose.SWIMMING) {
                player.pose = Pose.STANDING
            }
        }
    }
}