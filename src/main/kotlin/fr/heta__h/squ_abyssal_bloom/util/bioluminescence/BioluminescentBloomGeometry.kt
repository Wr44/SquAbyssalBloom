package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

data class BioluminescentBloomGeometry(
    val widthInBlocks: Int,
    val lengthInBlocks: Int,
    val shape: BioluminescentBloomShape,
    val rotationRadians: Double? = null
)
