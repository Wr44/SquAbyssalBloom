package fr.heta__h.squ_abyssal_bloom.compat.iris

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.BioluminescentIrisDebugState
import net.irisshaders.iris.api.v0.IrisApi

object IrisRenderState {

    private var lastLoggedShaderPackActive: Boolean? = null

    var shaderPackInUse: Boolean = false
        private set

    var renderingShadowPass: Boolean = false
        private set

    var vanillaQuadsLastFrame: Int = 0
        private set

    var shaderUnderwaterQuadsLastFrame: Int = 0
        private set

    var shaderCompensationQuadsLastFrame: Int = 0
        private set

    var shadowPassSkipCount: Int = 0
        private set

    var vanillaPassCount: Int = 0
        private set

    var shaderPrimaryPassCount: Int = 0
        private set

    var shaderCompensationPassCount: Int = 0
        private set

    var compensationDepthFactorAvg: Double = 0.0
        private set

    var compensationDepthFactorMin: Double = 0.0
        private set

    var compensationDepthFactorMax: Double = 0.0
        private set

    var compensationAngleFactorAvg: Double = 0.0
        private set

    var compensationFinalMultiplierAvg: Double = 0.0
        private set

    var compensationCameraUnderwater: Boolean = false
        private set

    fun refresh() {
        if (!ModCompat.hasIris) {
            shaderPackInUse = false
            renderingShadowPass = false
            return
        }
        try {
            val api = IrisApi.getInstance()
            shaderPackInUse = api.isShaderPackInUse
            renderingShadowPass = shaderPackInUse && api.isRenderingShadowPass
        } catch (t: Throwable) {
            shaderPackInUse = false
            renderingShadowPass = false
        }
        if (lastLoggedShaderPackActive != shaderPackInUse) {
            lastLoggedShaderPackActive = shaderPackInUse
            SquAbyssalBloom.LOGGER.info(
                "[Bio Debug] Iris shader pack ${if (shaderPackInUse) "activated" else "deactivated"}"
            )
        }
    }

    fun beginFrame() {
        vanillaPassCount = 0
        shaderPrimaryPassCount = 0
        shaderCompensationPassCount = 0
    }

    fun recordVanillaPass(quadCount: Int) {
        vanillaPassCount = 1
        vanillaQuadsLastFrame = quadCount
    }

    fun recordShaderPrimaryPass(quadCount: Int) {
        shaderPrimaryPassCount = 1
        shaderUnderwaterQuadsLastFrame = quadCount
    }

    fun recordShaderCompensationPass(
        quadCount: Int,
        depthFactorAvg: Double,
        depthFactorMin: Double,
        depthFactorMax: Double,
        angleFactorAvg: Double,
        finalMultiplierAvg: Double,
        cameraUnderwater: Boolean
    ) {
        shaderCompensationPassCount = 1
        shaderCompensationQuadsLastFrame = quadCount
        compensationDepthFactorAvg = depthFactorAvg
        compensationDepthFactorMin = depthFactorMin
        compensationDepthFactorMax = depthFactorMax
        compensationAngleFactorAvg = angleFactorAvg
        compensationFinalMultiplierAvg = finalMultiplierAvg
        compensationCameraUnderwater = cameraUnderwater
    }

    fun recordShadowPassSkip() {
        shadowPassSkipCount++
    }

    fun debugLines(): List<String> {
        if (!ModCompat.hasIris) return listOf("[Bio Debug] Iris: not installed")
        val compensationEnabled = ModConfig.shaderBioluminescenceVisibilityCompensation > 0.0
        return listOf(
            "[Bio Debug] Iris: present shaderPackInUse=$shaderPackInUse shadowPass=$renderingShadowPass " +
                "debugMode=${BioluminescentIrisDebugState.mode}",
            "[Bio Debug] Iris ordre attendu: opaque -> AfterOpaqueBlocks(primary) -> entites -> " +
                "eau reelle -> AfterTranslucentBlocks(compensation)",
            "[Bio Debug] Iris passe principale: activee=$shaderPackInUse stage=AfterOpaqueBlocks " +
                "pipeline=bioluminescent_shader_underwater_surface irisProgram=ENTITIES_TRANSLUCENT " +
                "alphaMultiplier=${ModConfig.shaderBioluminescencePrimaryAlphaMultiplier}",
            "[Bio Debug] Iris passe compensation: activee=${shaderPackInUse && compensationEnabled} stage=AfterTranslucentBlocks " +
                "pipeline=bioluminescent_shader_visibility_compensation irisProgram=ENTITIES_TRANSLUCENT " +
                "force=${ModConfig.shaderBioluminescenceVisibilityCompensation}",
            "[Bio Debug] Iris compensation: depthFactor avg=$compensationDepthFactorAvg " +
                "min=$compensationDepthFactorMin max=$compensationDepthFactorMax " +
                "angleFactorAvg=$compensationAngleFactorAvg multiplierAvg=$compensationFinalMultiplierAvg " +
                "cameraImmergee=$compensationCameraUnderwater",
            "[Bio Debug] Iris passes derniere frame: vanilla=$vanillaPassCount principale=$shaderPrimaryPassCount " +
                "compensation=$shaderCompensationPassCount shadowPassIgnorees=$shadowPassSkipCount",
            "[Bio Debug] Iris quads: vanilla=$vanillaQuadsLastFrame principale=$shaderUnderwaterQuadsLastFrame " +
                "compensation=$shaderCompensationQuadsLastFrame"
        )
    }
}
