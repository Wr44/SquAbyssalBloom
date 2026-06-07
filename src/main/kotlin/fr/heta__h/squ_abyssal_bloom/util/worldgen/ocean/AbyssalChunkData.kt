package fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean

data class AbyssalChunkData(val mask: BooleanArray, val floorGrid: IntArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbyssalChunkData

        if (!mask.contentEquals(other.mask)) return false
        if (!floorGrid.contentEquals(other.floorGrid)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mask.contentHashCode()
        result = 31 * result + floorGrid.contentHashCode()
        return result
    }
}