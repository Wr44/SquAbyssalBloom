package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import net.minecraft.world.level.levelgen.RandomSupport

object BioluminescentPalettes {
    private val cyanGreen = BioluminescentPalette(0x18DDE3, 0x78F5AE, 0xEFFFF8)
    private val cyanPurple = BioluminescentPalette(0x20D9EC, 0x966CFF, 0xF5EDFF)
    private val violetPink = BioluminescentPalette(0x895FF4, 0xFF62B8, 0xFFF0FB)
    private val turquoiseMagenta = BioluminescentPalette(0x20DFC1, 0xE957D6, 0xFFF0FC)

    private val all = listOf(cyanGreen, cyanPurple, violetPink, turquoiseMagenta)

    fun select(
        seed: Long,
        family: BioluminescentPaletteFamily = BioluminescentPaletteFamily.RANDOM
    ): BioluminescentPalette {
        val choices = when (family) {
            BioluminescentPaletteFamily.RANDOM -> all
            BioluminescentPaletteFamily.CYAN -> listOf(cyanGreen, cyanPurple)
            BioluminescentPaletteFamily.GREEN -> listOf(cyanGreen)
            BioluminescentPaletteFamily.PURPLE -> listOf(cyanPurple, violetPink)
            BioluminescentPaletteFamily.PINK -> listOf(violetPink, turquoiseMagenta)
        }
        val index = Math.floorMod(
            RandomSupport.mixStafford13(seed),
            choices.size.toLong()
        ).toInt()
        return choices[index]
    }
}
