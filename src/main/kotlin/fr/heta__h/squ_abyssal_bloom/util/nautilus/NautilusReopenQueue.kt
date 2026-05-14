package fr.heta__h.squ_abyssal_bloom.util.nautilus

object NautilusReopenQueue {
    private val pending = mutableListOf<() -> Unit>()

    fun schedule(action: () -> Unit) {
        pending.add(action)
    }

    fun flush() {
        pending.forEach { it() }
        pending.clear()
    }
}