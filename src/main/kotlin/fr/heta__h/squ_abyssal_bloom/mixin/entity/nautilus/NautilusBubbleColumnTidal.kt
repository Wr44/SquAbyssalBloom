package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Entity::class)
abstract class NautilusBubbleColumnTidal {

    @Inject(method = ["onInsideBubbleColumn"], at = [At("HEAD")], cancellable = true)
    fun onInsideBubbleColumn(dragDown: Boolean, ci: CallbackInfo) {
        val self = this as Entity
        if (shouldCancelBubbleColumn(self)) ci.cancel()
    }

    @Inject(method = ["onAboveBubbleColumn"], at = [At("HEAD")], cancellable = true)
    fun onAboveBubbleColumn(dragDown: Boolean, pos: BlockPos, ci: CallbackInfo) {
        val self = this as Entity
        if (shouldCancelBubbleColumn(self)) ci.cancel()
    }

    private fun shouldCancelBubbleColumn(self: Entity): Boolean {

        val nautilusDirect = self as? AbstractNautilus
        if (nautilusDirect != null) {
            val bodyStack = nautilusDirect.getItemBySlot(EquipmentSlot.BODY)
            val level = getEnchantLevel(bodyStack, nautilusDirect.level(), "tidal_immunity")
            return level > 0
        }

        val vehicle = self.vehicle
        val nautilusVehicle = vehicle as? AbstractNautilus
        if (nautilusVehicle != null) {
            val bodyStack = nautilusVehicle.getItemBySlot(EquipmentSlot.BODY)
            val level = getEnchantLevel(bodyStack, nautilusVehicle.level(), "tidal_immunity")
            return level > 0
        }

        return false
    }

}