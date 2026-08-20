package fr.heta__h.squ_abyssal_bloom.network.conduit

import fr.heta__h.squ_abyssal_bloom.event.conduit.ConduitBoundaryClientRenderer

object ConduitClientPayloadHandler {
    fun handleDomain(payload: S2CConduitDomainPayload) {
        ConduitBoundaryClientRenderer.applyDomain(payload)
    }
}
