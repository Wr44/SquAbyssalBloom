package fr.heta__h.squ_abyssal_bloom.network.bioluminescence

import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.BioluminescentZoneManager
import net.minecraft.client.Minecraft

object BioluminescenceClientPayloadHandler {
    fun handleWave(payload: S2CBioluminescenceWavePayload) {
        val level = Minecraft.getInstance().level ?: return
        if (!payload.isStructurallyValid()) return
        if (level.dimension().identifier() != payload.dimension) return
        BioluminescentZoneManager.synchronizeWave(level, payload)
    }

    fun handleWaveEnd(payload: S2CBioluminescenceWaveEndPayload) {
        val level = Minecraft.getInstance().level ?: return
        if (level.dimension().identifier() != payload.dimension) return
        BioluminescentZoneManager.removeWave(payload.eventId, level.gameTime)
    }

    fun handleArmed(payload: S2CBioluminescenceArmedPayload) {
        BioluminescentZoneManager.setOpportunityArmed(payload.armed)
    }

    fun handleBloomStateUpdate(payload: S2CPlanktonBloomStateUpdatePayload) {
        BioluminescentZoneManager.applyBloomStateUpdate(payload)
    }
}
