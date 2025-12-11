package fr.heta__h.squ_abyssal_bloom.datagen

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.ModelProvider
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.data.PackOutput

class ModModelProvider(output: PackOutput) : ModelProvider(output, Squ_abyssal_bloom.ID) {

    protected override fun registerModels(blockModels: BlockModelGenerators, itemModels: ItemModelGenerators) {
        itemModels.generateFlatItem(ModItems.BARNACLE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM)
    }
}