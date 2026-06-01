package fr.heta__h.squ_abyssal_bloom.config

import net.neoforged.fml.loading.FMLEnvironment
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist

object ServerConfigCache {
    var strictBarnacleSpawning: Boolean = true

    fun update(data: ServerConfigData) {
        strictBarnacleSpawning = data.strictBarnacleSpawning
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
}