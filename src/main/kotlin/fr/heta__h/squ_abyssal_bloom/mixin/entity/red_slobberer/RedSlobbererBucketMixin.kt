package fr.heta__h.squ_abyssal_bloom.mixin.entity.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.animal.Bucketable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(RedSlobbererEntity::class)
abstract class RedSlobbererBucketMixin : Bucketable {

    private val self get() = this as RedSlobbererEntity

    override fun fromBucket(): Boolean = self.isFromBucket()

    override fun setFromBucket(fromBucket: Boolean) {
        self.setFromBucketFlag(fromBucket)
    }

    override fun saveToBucketTag(bucket: ItemStack) {
        Bucketable.saveDefaultDataToBucketTag(self, bucket)
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket) { tag ->
            tag.putInt("Age", self.getAge())
        }
    }

    override fun loadFromBucketTag(tag: CompoundTag) {
        Bucketable.loadDefaultDataFromBucketTag(self, tag)
        self.setAge(tag.getIntOr("Age", self.babyStartAge()))
    }

    override fun getBucketItemStack(): ItemStack = ItemStack(ModItems.BABY_RED_SLOBBERER_BUCKET.get())

    override fun getPickupSound(): SoundEvent = ModSounds.BABY_RED_SLOBBERER_CAPTURED.get()

    @Inject(method = ["mobInteract"], at = [At("HEAD")], cancellable = true)
    private fun onBucketPickup(player: Player, hand: InteractionHand, cir: CallbackInfoReturnable<InteractionResult>) {
        if (!self.isBaby) return
        val itemStack = player.getItemInHand(hand)
        if (itemStack.item != Items.WATER_BUCKET || !self.isAlive) return

        self.playSound(getPickupSound(), 1.0f, 1.0f)
        val bucket = getBucketItemStack()
        saveToBucketTag(bucket)
        val result = ItemUtils.createFilledResult(itemStack, player, bucket, false)
        player.setItemInHand(hand, result)

        if (!self.level().isClientSide && player is ServerPlayer) {
            CriteriaTriggers.FILLED_BUCKET.trigger(player, bucket)
        }

        self.discard()
        cir.returnValue = InteractionResult.SUCCESS
        cir.cancel()
    }

    @Inject(method = ["requiresCustomPersistence"], at = [At("HEAD")], cancellable = true)
    private fun onRequiresCustomPersistence(cir: CallbackInfoReturnable<Boolean>) {
        if (fromBucket()) {
            cir.returnValue = true
            cir.cancel()
        }
    }
}
