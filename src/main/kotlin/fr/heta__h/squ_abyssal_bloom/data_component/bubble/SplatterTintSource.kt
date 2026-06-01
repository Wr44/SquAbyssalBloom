package fr.heta__h.squ_abyssal_bloom.data_component.bubble

import com.mojang.serialization.MapCodec
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

class SplatterTintSource : ItemTintSource {
    override fun calculate(stack: ItemStack, level: ClientLevel?, entity: LivingEntity?): Int {
        val data = stack.get(ModDataComponents.SPLATTER_DATA.get())
        return data?.color ?: -1
    }

    override fun type(): MapCodec<out ItemTintSource> = CODEC

    companion object {
        val ID = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "splatter_tint")
        val CODEC: MapCodec<SplatterTintSource> = MapCodec.unit(SplatterTintSource())
    }
}