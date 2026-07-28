package fr.heta__h.squ_abyssal_bloom.mixin.entity.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishThreatClassifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.Pufferfish
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Pufferfish::class, remap = false)
abstract class PufferfishScaryMobMixin {

    private companion object {
        @JvmStatic
        @Inject(method = ["lambda\$static\$0"], at = [At("HEAD")], cancellable = true)
        private fun notScaryIfFishSchoolFriendly(
            target: LivingEntity,
            level: ServerLevel,
            cir: CallbackInfoReturnable<Boolean>
        ) {
            if (FishThreatClassifier.isFriendly(target)) {
                cir.returnValue = false
            }
        }
    }
}
