package fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave

enum class BioluminescentZoneActivity(val commandName: String, val maximumOpacity: Float) {
    ACTIVE("active", 0.60f),
    INACTIVE("inactive", 0.45f)
}
