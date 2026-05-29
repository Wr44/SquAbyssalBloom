package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.ConduitBlockEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ConduitBlockEntity::class)
interface ConduitBlockEntityAccessor {
    @Accessor("effectBlocks")
    fun getEffectBlocks(): List<BlockPos>
}