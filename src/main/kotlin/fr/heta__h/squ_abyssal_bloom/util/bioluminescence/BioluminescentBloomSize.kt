package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

enum class BioluminescentBloomSize(
    val commandName: String,
    val minimumBlocks: Int,
    val maximumBlocks: Int
) {
    SMALL("small", 6, 12),
    MEDIUM("medium", 12, 24),
    LARGE("large", 24, 48)
}
