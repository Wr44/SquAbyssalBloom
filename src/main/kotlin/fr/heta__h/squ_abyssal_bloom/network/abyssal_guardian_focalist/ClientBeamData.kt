package fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist

import java.util.concurrent.ConcurrentHashMap

object ClientBeamData {
    val activeBeams: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()

    fun clear() {
        activeBeams.clear()
    }
}
