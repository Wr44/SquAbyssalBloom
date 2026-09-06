package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton

data class BioluminescentSkeletonPath(
    val sourceCore: Int,
    val destinationCore: Int,
    val cellIndices: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BioluminescentSkeletonPath

        if (sourceCore != other.sourceCore) return false
        if (destinationCore != other.destinationCore) return false
        if (!cellIndices.contentEquals(other.cellIndices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceCore
        result = 31 * result + destinationCore
        result = 31 * result + cellIndices.contentHashCode()
        return result
    }
}
