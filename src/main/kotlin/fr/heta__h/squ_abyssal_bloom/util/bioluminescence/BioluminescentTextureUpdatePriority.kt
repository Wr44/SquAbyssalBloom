package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

enum class BioluminescentTextureUpdatePriority(val rank: Int) {
    STABLE(1),
    FLICKER(2),
    DISAPPEARING(3),
    APPEARING(4),
    CREATED(5)
}
