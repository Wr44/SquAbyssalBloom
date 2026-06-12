package fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain

data class AbyssalChunkData(
    val abyssalMask: BooleanArray,
    val carverMask: BooleanArray,
    val floorGrid: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AbyssalChunkData
        if (!abyssalMask.contentEquals(other.abyssalMask)) return false
        if (!carverMask.contentEquals(other.carverMask)) return false
        if (!floorGrid.contentEquals(other.floorGrid)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = abyssalMask.contentHashCode()
        result = 31 * result + carverMask.contentHashCode()
        result = 31 * result + floorGrid.contentHashCode()
        return result
    }
}