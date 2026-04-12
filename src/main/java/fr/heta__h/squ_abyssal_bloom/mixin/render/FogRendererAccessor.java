package fr.heta__h.squ_abyssal_bloom.mixin.render;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(FogRenderer.class)
public interface FogRendererAccessor {
    @Accessor("FOG_ENVIRONMENTS")
    static List<FogEnvironment> getFogEnvironments() {
        throw new AssertionError();
    }
}