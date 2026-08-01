package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

data class BioluminescentWaterBounds(
    val minimumX: Int,
    val minimumY: Int,
    val minimumZ: Int,
    val maximumX: Int,
    val maximumY: Int,
    val maximumZ: Int
) {
    val width: Int
        get() = maximumX - minimumX + 1

    val length: Int
        get() = maximumZ - minimumZ + 1

    val debugText: String
        get() = "$minimumX,$minimumY,$minimumZ -> $maximumX,$maximumY,$maximumZ"
}
