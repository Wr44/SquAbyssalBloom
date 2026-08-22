package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.pipeline

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object BioluminescentRenderPipelines {

    val VANILLA_SURFACE: RenderPipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "pipeline/bioluminescent_surface"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withShaderDefine("EMISSIVE")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler0")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    val SHADER_UNDERWATER_SURFACE: RenderPipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "pipeline/bioluminescent_shader_underwater_surface"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler0")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    val SHADER_VISIBILITY_COMPENSATION: RenderPipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "pipeline/bioluminescent_shader_visibility_compensation"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler0")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    val VANILLA_FOOTPRINT: RenderPipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "pipeline/bioluminescent_footprint"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withShaderDefine("EMISSIVE")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler0")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    val SHADER_FOOTPRINT: RenderPipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "pipeline/bioluminescent_shader_footprint"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler0")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    @SubscribeEvent
    fun registerPipelines(event: RegisterRenderPipelinesEvent) {
        event.registerPipeline(VANILLA_SURFACE)
        event.registerPipeline(SHADER_UNDERWATER_SURFACE)
        event.registerPipeline(SHADER_VISIBILITY_COMPENSATION)
        event.registerPipeline(VANILLA_FOOTPRINT)
        event.registerPipeline(SHADER_FOOTPRINT)
    }
}
