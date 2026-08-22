package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.world.entity.ExperienceOrb
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ExperienceOrb::class)
interface ExperienceOrbAccessor {

    @Accessor("count")
    fun getCount(): Int

    @Accessor("count")
    fun setCount(value: Int)
}
