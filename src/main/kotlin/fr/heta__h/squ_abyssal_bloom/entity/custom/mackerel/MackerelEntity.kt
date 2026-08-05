package fr.heta__h.squ_abyssal_bloom.entity.custom.mackerel

import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class MackerelEntity(type: EntityType<out MackerelEntity>, level: Level) : AbstractSchoolingFish(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = AbstractFish.createAttributes()
    }

    override fun getBucketItemStack(): ItemStack = ItemStack(ModItems.MACKEREL_BUCKET.get())

    override fun getAmbientSound(): SoundEvent = ModSounds.MACKEREL_AMBIENT.get()

    override fun getDeathSound(): SoundEvent = ModSounds.MACKEREL_DEATH.get()

    override fun getHurtSound(source: DamageSource): SoundEvent = ModSounds.MACKEREL_HURT.get()

    override fun getFlopSound(): SoundEvent = ModSounds.MACKEREL_FLOP.get()

    override fun canRide(vehicle: Entity): Boolean = false
}
