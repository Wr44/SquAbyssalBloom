package fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.palette

import net.minecraft.world.level.levelgen.RandomSupport

object BioluminescentPalettes {
    private val cyanGreen = BioluminescentPalette(
        0x082B68,
        0x18DDE3,
        0x78F5AE,
        0x9A55F5,
        0xEFFFF8
    )
    private val cyanPurple = BioluminescentPalette(
        0x101D72,
        0x20D9EC,
        0x8061F5,
        0xEB42CE,
        0xF5EDFF
    )
    private val violetPink = BioluminescentPalette(
        0x15185F,
        0x7959EE,
        0xE947D2,
        0xFF6FA9,
        0xFFF0FB
    )
    private val turquoiseMagenta = BioluminescentPalette(
        0x08296A,
        0x20DFC1,
        0xBE3FDF,
        0xFF53AE,
        0xFFF0FC
    )
    private val blueMagentaPink = BioluminescentPalette(
        0x0B1C68,
        0x3B72EA,
        0xC541E5,
        0xFF5A9F,
        0xFFF0FC
    )

    private val all = listOf(cyanGreen, cyanPurple, violetPink, turquoiseMagenta, blueMagentaPink)

    val waveColorCycle: List<Int> = all.flatMap { palette ->
        listOf(palette.firstColor, palette.secondColor, palette.accentColor)
    }

    fun select(
        seed: Long,
        family: BioluminescentPaletteFamily = BioluminescentPaletteFamily.RANDOM
    ): BioluminescentPalette {
        val choices = when (family) {
            BioluminescentPaletteFamily.RANDOM -> all
            BioluminescentPaletteFamily.CYAN -> listOf(cyanGreen, cyanPurple)
            BioluminescentPaletteFamily.GREEN -> listOf(cyanGreen)
            BioluminescentPaletteFamily.PURPLE -> listOf(cyanPurple, violetPink, blueMagentaPink)
            BioluminescentPaletteFamily.PINK -> listOf(violetPink, turquoiseMagenta, blueMagentaPink)
        }
        val index = Math.floorMod(
            RandomSupport.mixStafford13(seed),
            choices.size.toLong()
        ).toInt()
        return choices[index]
    }
}
