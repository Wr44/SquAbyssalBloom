package fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist

object ClientBeamHandler {
    fun handleBeamSync(payload: FocalistBeamSyncPayload) {
        if (payload.isShooting) {
            ClientBeamData.activeBeams[payload.shooterId] = payload.targetId
        } else {
            ClientBeamData.activeBeams.remove(payload.shooterId)
        }
    }
}