package fr.heta__h.squ_abyssal_bloom.config

import net.neoforged.fml.loading.FMLEnvironment
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist

object ServerConfigCache {
    var strictBarnacleSpawning: Boolean = true
    var shallowDeepBoundary: Float = -0.477f
    var deepAbyssalBoundary: Float = -0.763f

    fun update(data: ServerConfigData) {
        strictBarnacleSpawning = data.strictBarnacleSpawning
        shallowDeepBoundary = data.shallowDeepBoundary
        deepAbyssalBoundary = data.deepAbyssalBoundary
    }

    fun isSingleplayer(): Boolean {
        return if (FMLEnvironment.getDist() == Dist.CLIENT) {
            Minecraft.getInstance().isLocalServer
        } else {
            false
        }
    }

    val effectiveStrictBarnacleSpawning: Boolean
        get() = if (isSingleplayer()) ModServerConfig.STRICT_BARNACLE_SPAWNING.get() else strictBarnacleSpawning

    val effectiveShallowDeep: Float
        get() = if (isSingleplayer()) ModServerConfig.SHALLOW_DEEP_BOUNDARY.get().toFloat() else shallowDeepBoundary

    val effectiveDeepAbyssal: Float
        get() = if (isSingleplayer()) ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get().toFloat() else deepAbyssalBoundary
}