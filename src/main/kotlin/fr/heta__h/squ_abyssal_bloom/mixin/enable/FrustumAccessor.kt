package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.client.renderer.culling.Frustum
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(Frustum::class)
interface FrustumAccessor {
    @Invoker("cubeInFrustum")
    fun invokeCubeInFrustum(
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double
    ): Int
}
