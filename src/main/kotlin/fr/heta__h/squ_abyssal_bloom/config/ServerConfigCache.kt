package fr.heta__h.squ_abyssal_bloom.config

import net.neoforged.fml.loading.FMLEnvironment
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist

object ServerConfigCache {
    var strictBarnacleSpawning: Boolean = true
    var shallowDeepBoundary: Double = -0.477
    var deepAbyssalBoundary: Double = -0.763

    fun update(data: ServerConfigData) {
        strictBarnacleSpawning = data.strictBarnacleSpawning
        shallowDeepBoundary = data.shallowDeepBoundary
        deepAbyssalBoundary = data.deepAbyssalBoundary
    }

    fun syncFromSpec() {
        update(ServerConfigData.fromSpec())
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

    // On convertit en Float uniquement au moment de la lecture pour le Mixin
    val effectiveShallowDeep: Float
        get() = (if (isSingleplayer()) ModServerConfig.SHALLOW_DEEP_BOUNDARY.get() else shallowDeepBoundary).toFloat()

    val effectiveDeepAbyssal: Float
        get() = (if (isSingleplayer()) ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get() else deepAbyssalBoundary).toFloat()
}