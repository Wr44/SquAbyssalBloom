package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(AbstractNautilus::class)
abstract class NautilusChestMixin {
    private val self get() = this as AbstractNautilus
    private fun hasChest() = self.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).`is`(NautilusLayerItems.CHEST)

    @Inject(method = ["getInventoryColumns"], at = [At("HEAD")], cancellable = true)
    private fun chestColumns(cir: CallbackInfoReturnable<Int>) {
        if (hasChest()) {
            cir.returnValue = 5
            cir.cancel()
        }
    }
}