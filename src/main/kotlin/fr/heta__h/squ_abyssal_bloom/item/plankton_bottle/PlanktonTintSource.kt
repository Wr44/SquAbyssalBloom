package fr.heta__h.squ_abyssal_bloom.item.plankton_bottle

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.palette.BioluminescentPalettes
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

class PlanktonTintSource(private val phase: Double, private val whiten: Double) : ItemTintSource {

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "plankton_tint")

        val CODEC: MapCodec<PlanktonTintSource> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.DOUBLE.optionalFieldOf("phase", 0.0).forGetter(PlanktonTintSource::phase),
                Codec.DOUBLE.optionalFieldOf("whiten", 0.0).forGetter(PlanktonTintSource::whiten)
            ).apply(instance, ::PlanktonTintSource)
        }

        private const val OPAQUE_ALPHA = 0xFF shl 24
        private const val COLOR_STEP_TICKS = 70.0
        private const val PULSE_PERIOD_TICKS = 46.0
        private const val FLICKER_PERIOD_TICKS = 11.0
        private const val FLICKER_STRENGTH = 0.25
        private const val MIN_LUMINOSITY = 0.08
        private const val MAX_LUMINOSITY = 1.0
        private const val GLOW_WHITEN_STRENGTH = 0.62
    }

    override fun calculate(stack: ItemStack, level: ClientLevel?, entity: LivingEntity?): Int {
        val renderGameTime = renderGameTime(level)
        val color = cycledColorAt(renderGameTime)
        val glow = luminosityAt(renderGameTime) * GLOW_WHITEN_STRENGTH
        val brightened = ModUtilities.lerpColor(color, ModUtilities.WHITE_RGB, glow)
        return OPAQUE_ALPHA or ModUtilities.lerpColor(brightened, ModUtilities.WHITE_RGB, whiten.coerceIn(0.0, 1.0))
    }

    override fun type(): MapCodec<out ItemTintSource> = CODEC

    private fun renderGameTime(level: ClientLevel?): Double {
        if (level == null) return 0.0

        val elapsed = level.gameTime + Minecraft.getInstance().deltaTracker.gameTimeDeltaTicks.toDouble()
        return elapsed + phase * COLOR_STEP_TICKS
    }

    private fun cycledColorAt(renderGameTime: Double): Int {
        val cycle = BioluminescentPalettes.waveColorCycle
        val position = renderGameTime / COLOR_STEP_TICKS
        val step = floor(position)
        val index = Math.floorMod(step.toLong(), cycle.size.toLong()).toInt()
        val next = (index + 1) % cycle.size
        val progress = ModUtilities.smooth(0.0, 1.0, position - step)

        return ModUtilities.lerpColor(cycle[index], cycle[next], progress)
    }

    private fun luminosityAt(renderGameTime: Double): Double {
        val slow = 0.5 + 0.5 * sin(renderGameTime * PI * 2.0 / PULSE_PERIOD_TICKS)
        val fast = 0.5 + 0.5 * sin(renderGameTime * PI * 2.0 / FLICKER_PERIOD_TICKS)
        val combined = ModUtilities.smooth(0.0, 1.0, slow) * (1.0 - FLICKER_STRENGTH) + fast * FLICKER_STRENGTH
        return MIN_LUMINOSITY + (MAX_LUMINOSITY - MIN_LUMINOSITY) * combined
    }
}
