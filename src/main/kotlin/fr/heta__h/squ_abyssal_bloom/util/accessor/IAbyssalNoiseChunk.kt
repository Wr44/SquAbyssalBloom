package fr.heta__h.squ_abyssal_bloom.util.accessor

interface IAbyssalNoiseChunk {
    fun abyssalBloom_getFloorGrid(): IntArray?
    fun abyssalBloom_setFloorGrid(grid: IntArray?)
    fun abyssalBloom_getChunkMinX(): Int
    fun abyssalBloom_setChunkMinX(x: Int)
    fun abyssalBloom_getChunkMinZ(): Int
    fun abyssalBloom_setChunkMinZ(z: Int)
}