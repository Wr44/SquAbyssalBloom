package fr.heta__h.squ_abyssal_bloom.entity.client

import fr.heta__h.squ_abyssal_bloom.entity.custom.BarnacleEntity
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider

class BarnacleRenderer(context: EntityRendererProvider.Context) : EntityRenderer<BarnacleEntity, BarnacleRenderState>(
    context
) {
    override fun createRenderState(): BarnacleRenderState = BarnacleRenderState()
}