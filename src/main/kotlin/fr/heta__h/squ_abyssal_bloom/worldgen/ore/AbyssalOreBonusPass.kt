package fr.heta__h.squ_abyssal_bloom.worldgen.ore

object AbyssalOreBonusPass {
    private val active: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    val isActive: Boolean get() = active.get()

    fun enter() = active.set(true)

    fun exit() = active.remove()
}
