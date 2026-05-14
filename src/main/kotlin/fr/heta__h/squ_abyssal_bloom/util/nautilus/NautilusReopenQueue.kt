package fr.heta__h.squ_abyssal_bloom.util.nautilus

object NautilusReopenQueue {
    private val pending = mutableListOf<() -> Unit>()

    fun schedule(action: () -> Unit) {
        pending.add(action)
    }

    fun flush() {
        if (pending.isEmpty()) return
        val snapshot = pending.toList()
        pending.clear()
        snapshot.forEach { it() }
    }
}