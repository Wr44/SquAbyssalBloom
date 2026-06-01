package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractNautilus::class)
abstract class NautilusRegenMixin {

    val REGEN_INTERVAL = 600
    val REGEN_HEAL = 1f

    @Unique
    private var regenTimer = 0

    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun onTick(ci: CallbackInfo) {
        val nautilus = this as AbstractNautilus
        if (nautilus.level().isClientSide) return
        if (!nautilus.isAlive || !nautilus.isTame) return
        if (nautilus.health >= nautilus.maxHealth) return

        regenTimer++
        if (regenTimer >= REGEN_INTERVAL) {
            regenTimer = 0
            nautilus.heal(REGEN_HEAL)
        }
    }
}