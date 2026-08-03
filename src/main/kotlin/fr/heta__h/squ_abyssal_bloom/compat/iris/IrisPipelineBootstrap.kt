package fr.heta__h.squ_abyssal_bloom.compat.iris

import com.mojang.blaze3d.pipeline.RenderPipeline
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram

object IrisPipelineBootstrap {

    private var attempted = false

    fun registerBioluminescentSurfaces(vararg pipelines: RenderPipeline) {
        if (attempted) return
        attempted = true

        if (!ModCompat.hasIris) return
        if (!ModConfig.enableIrisCompatibility) {
            SquAbyssalBloom.LOGGER.info(
                "[Bio Debug] Iris detected but compatibility disabled in config; skipping pipeline registration"
            )
            return
        }

        for (pipeline in pipelines) {
            try {
                IrisApi.getInstance().assignPipeline(pipeline, IrisProgram.ENTITIES_TRANSLUCENT)
                SquAbyssalBloom.LOGGER.info(
                    "[Bio Debug] Iris compatibility registered: ${pipeline.location} -> IrisProgram.ENTITIES_TRANSLUCENT"
                )
            } catch (t: Throwable) {
                SquAbyssalBloom.LOGGER.warn(
                    "[Bio Debug] Iris compatibility registration failed for ${pipeline.location}; " +
                        "this pass will render unhooked under shader packs",
                    t
                )
            }
        }
    }
}
