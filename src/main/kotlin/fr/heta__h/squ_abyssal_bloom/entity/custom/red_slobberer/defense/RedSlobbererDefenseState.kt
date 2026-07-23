package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.defense

enum class RedSlobbererDefenseState(val networkId: Int) {
    NORMAL(0),
    HIDING(1),
    HIDDEN(2),
    SHOWING(3);

    companion object {
        private val BY_NETWORK_ID = entries.associateBy(RedSlobbererDefenseState::networkId)

        fun fromNetworkId(networkId: Int): RedSlobbererDefenseState = BY_NETWORK_ID[networkId] ?: NORMAL
    }
}
