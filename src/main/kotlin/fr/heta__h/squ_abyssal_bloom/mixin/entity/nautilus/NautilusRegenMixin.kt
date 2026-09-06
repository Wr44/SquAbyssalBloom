package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractNautilus::class)
abstract class NautilusRegenMixin {

    @Unique
    private var regenTimer = 0

    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun onTick(ci: CallbackInfo) {
        val nautilus = this as AbstractNautilus
        if (nautilus.level().isClientSide) return
        if (!nautilus.isAlive || !nautilus.isTame || nautilus.health >= nautilus.maxHealth) {
            regenTimer = 0
            return
        }

        regenTimer++
        if (regenTimer >= 600) {
            regenTimer = 0
            nautilus.heal(1f)
        }
    }
}
