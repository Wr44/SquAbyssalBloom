package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

enum class BioluminescentIrisDebugMode {
    NORMAL,
    PRIMARY_ONLY,
    COMPENSATION_ONLY,
    COMPENSATION_IGNORE_MODULATION,
    COMPENSATION_FIXED_ALPHA,
    COMPENSATION_FACTOR_VISUALIZATION
}

object BioluminescentIrisDebugState {

    var mode: BioluminescentIrisDebugMode = BioluminescentIrisDebugMode.NORMAL
    var compensationOffsetOverride: Double? = null

    fun reset() {
        mode = BioluminescentIrisDebugMode.NORMAL
        compensationOffsetOverride = null
    }
}
