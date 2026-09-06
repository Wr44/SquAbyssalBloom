package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

data class BioluminescentWaterBounds(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int
) {
    val width: Int
        get() = maxX - minX + 1

    val length: Int
        get() = maxZ - minZ + 1

}
