package fr.heta__h.squ_abyssal_bloom.util.accessor

interface IAbyssalNoiseChunk {
    fun getFloorGrid(): IntArray?
    fun setFloorGrid(grid: IntArray?)

    fun getChunkMinX(): Int
    fun setChunkMinX(x: Int)

    fun getChunkMinZ(): Int
    fun setChunkMinZ(z: Int)
}