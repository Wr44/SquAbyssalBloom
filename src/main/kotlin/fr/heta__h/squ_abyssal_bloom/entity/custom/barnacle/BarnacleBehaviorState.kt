package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

enum class BarnacleBehaviorState(val syncedId: Int) {
    FLEE(0),
    SWALLOW(1),
    GRAB(2),
    PURSUE(3),
    IDLE(4);

    companion object {
        fun fromSyncedId(id: Int): BarnacleBehaviorState = entries.firstOrNull { it.syncedId == id } ?: IDLE
    }
}
