package fr.heta__h.squ_abyssal_bloom.event.sound

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.abyssal_depth.AbyssDepthCache
import fr.heta__h.squ_abyssal_bloom.render.abyssal_depth.AbyssDepthProfile
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.sound.ambient.AbyssalLoopSound
import fr.heta__h.squ_abyssal_bloom.util.sound.PositionedAmbientSound
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.event.level.LevelEvent
import kotlin.math.roundToInt

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object AbyssalAmbientSoundManager {
    private const val SAMPLE_EXTENT_HORIZONTAL = 12
    private const val SAMPLE_EXTENT_VERTICAL = 8
    private const val MOOD_DRAIN_TICKS = 6000.0f
    private const val DEEP_SHIFT_MIN_TICKS = 1400
    private const val DEEP_SHIFT_MAX_TICKS = 2000
    private const val ABYSSAL_SHIFT_MIN_TICKS = 600
    private const val ABYSSAL_SHIFT_MAX_TICKS = 900
    private const val DEEP_BUBBLE_MIN_TICKS = 2400
    private const val DEEP_BUBBLE_MAX_TICKS = 3600
    private const val ABYSSAL_BUBBLE_MIN_TICKS = 1200
    private const val ABYSSAL_BUBBLE_MAX_TICKS = 1800
    private const val MOOD_MIN_TICK_DELAY = 1800
    private const val MOOD_MAX_TICK_DELAY = 3000

    private val random = RandomSource.create()
    private var currentLevel: ClientLevel? = null
    private var loop: AbyssalLoopSound? = null
    private var moodiness = 0.0f
    private var moodTickDelay = nextDelay(MOOD_MIN_TICK_DELAY, MOOD_MAX_TICK_DELAY).toFloat()
    private var shiftTicksRemaining = -1
    private var bubbleTicksRemaining = -1

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level
        val player = minecraft.player

        if (level == null || player == null) {
            reset(null)
            return
        }
        if (currentLevel !== level) reset(level)

        val eyePos = BlockPos.containing(player.eyePosition)
        val underwater = level.getFluidState(eyePos).`is`(Fluids.WATER)
        if (underwater) AbyssDepthCache.refreshIfNeeded(level, eyePos)
        val entry = if (underwater) AbyssDepthProfile.entry.toFloat() else 0.0f
        val presence = if (underwater) AbyssDepthProfile.presence.toFloat() else 0.0f
        val oppression = if (underwater) AbyssDepthProfile.oppression.toFloat() else 0.0f

        updateLoop(minecraft, underwater, entry)

        if (!ModConfig.enableAbyssalAmbientSound || !underwater) {
            moodiness = (moodiness - 1.0f / MOOD_DRAIN_TICKS).coerceAtLeast(0.0f)
            return
        }

        updateAdditions(minecraft, player.eyePosition, entry > 0.0f, presence)

        updateMood(minecraft, level, player.eyePosition, oppression)
    }

    private fun updateLoop(minecraft: Minecraft, underwater: Boolean, depthFactor: Float) {
        val active = ModConfig.enableAbyssalAmbientSound && underwater && depthFactor > 0.0f
        val sound = loop
        if (active && (sound == null || sound.isStopped)) {
            AbyssalLoopSound(minecraft).also {
                loop = it
                minecraft.soundManager.play(it)
            }
        } else if (sound?.isStopped == true) {
            loop = null
        }
    }

    private fun updateMood(
        minecraft: Minecraft,
        level: ClientLevel,
        eyes: Vec3,
        oppression: Float
    ) {
        if (oppression <= 0.0f) {
            moodiness = (moodiness - 1.0f / MOOD_DRAIN_TICKS).coerceAtLeast(0.0f)
            return
        }

        val sample = BlockPos.containing(
            eyes.x + random.nextInt(SAMPLE_EXTENT_HORIZONTAL * 2 + 1) - SAMPLE_EXTENT_HORIZONTAL,
            eyes.y + random.nextInt(SAMPLE_EXTENT_VERTICAL * 2 + 1) - SAMPLE_EXTENT_VERTICAL,
            eyes.z + random.nextInt(SAMPLE_EXTENT_HORIZONTAL * 2 + 1) - SAMPLE_EXTENT_HORIZONTAL
        )
        if (!level.hasChunkAt(sample)) return

        val blockLight = level.getBrightness(LightLayer.BLOCK, sample)
        val darkness = 1.0f - blockLight / 15.0f
        val protection = AbyssDepthCache.displayedAmbientFogRepellerInfluence.toFloat().coerceIn(0.0f, 1.0f)
        val pressure = oppression * darkness * (1.0f - protection * 0.5f)

        moodiness += pressure / moodTickDelay
        moodiness -= (1.0f - darkness) / MOOD_DRAIN_TICKS
        moodiness = moodiness.coerceIn(0.0f, 1.0f)

        if (moodiness >= 1.0f) {
            playMood(minecraft, eyes, sample)
            moodiness = random.nextFloat() * 0.08f
            moodTickDelay = nextDelay(MOOD_MIN_TICK_DELAY, MOOD_MAX_TICK_DELAY).toFloat()
        }
    }

    private fun updateAdditions(minecraft: Minecraft, eyes: Vec3, inAbyssalZone: Boolean, intensity: Float) {
        if (!inAbyssalZone) {
            shiftTicksRemaining = -1
            bubbleTicksRemaining = -1
            return
        }

        val factor = intensity.coerceIn(0.0f, 1.0f)
        val shiftMin = interpolateDelay(DEEP_SHIFT_MIN_TICKS, ABYSSAL_SHIFT_MIN_TICKS, factor)
        val shiftMax = interpolateDelay(DEEP_SHIFT_MAX_TICKS, ABYSSAL_SHIFT_MAX_TICKS, factor)
        val bubbleMin = interpolateDelay(DEEP_BUBBLE_MIN_TICKS, ABYSSAL_BUBBLE_MIN_TICKS, factor)
        val bubbleMax = interpolateDelay(DEEP_BUBBLE_MAX_TICKS, ABYSSAL_BUBBLE_MAX_TICKS, factor)

        if (shiftTicksRemaining < 0) shiftTicksRemaining = nextDelay(shiftMin, shiftMax)
        if (bubbleTicksRemaining < 0) bubbleTicksRemaining = nextDelay(bubbleMin, bubbleMax)
        shiftTicksRemaining = shiftTicksRemaining.coerceAtMost(shiftMax)
        bubbleTicksRemaining = bubbleTicksRemaining.coerceAtMost(bubbleMax)

        shiftTicksRemaining--
        bubbleTicksRemaining--

        if (shiftTicksRemaining <= 0) {
            playAddition(minecraft, eyes, ModSounds.ABYSSAL_WATER_SHIFT.get())
            shiftTicksRemaining = nextDelay(shiftMin, shiftMax)
        }
        if (bubbleTicksRemaining <= 0) {
            playAddition(minecraft, eyes, ModSounds.ABYSSAL_DISTANT_BUBBLES.get())
            bubbleTicksRemaining = nextDelay(bubbleMin, bubbleMax)
        }
    }

    private fun playAddition(minecraft: Minecraft, eyes: Vec3, event: net.minecraft.sounds.SoundEvent) {
        minecraft.soundManager.play(
            PositionedAmbientSound(event, randomPosition(eyes, 10.0, 20.0), 1.0f, randomPitch(0.94f, 1.06f))
        )
    }

    private fun playMood(minecraft: Minecraft, eyes: Vec3, sample: BlockPos) {
        val sampleCenter = Vec3.atCenterOf(sample)
        val direction = sampleCenter.subtract(eyes).normalize()
        val offset = 4.0 + random.nextDouble() * 4.0
        val position = if (direction.lengthSqr() > 0.0) sampleCenter.add(direction.scale(offset)) else sampleCenter
        minecraft.soundManager.play(
            PositionedAmbientSound(ModSounds.ABYSSAL_MOOD.get(), position, 1.0f, randomPitch(0.96f, 1.04f))
        )
    }

    private fun randomPosition(origin: Vec3, minDistance: Double, maxDistance: Double): Vec3 {
        val angle = random.nextDouble() * Math.PI * 2.0
        val distance = minDistance + random.nextDouble() * (maxDistance - minDistance)
        val yOffset = random.nextDouble() * 12.0 - 8.0
        return origin.add(Math.cos(angle) * distance, yOffset, Math.sin(angle) * distance)
    }

    private fun randomPitch(min: Float, max: Float): Float = min + random.nextFloat() * (max - min)

    private fun nextDelay(min: Int, max: Int): Int = min + random.nextInt(max - min + 1)

    private fun interpolateDelay(deep: Int, abyssal: Int, factor: Float): Int =
        (deep + (abyssal - deep) * factor).roundToInt()

    private fun reset(level: ClientLevel?) {
        loop?.stopImmediately()
        loop = null
        moodiness = 0.0f
        moodTickDelay = nextDelay(MOOD_MIN_TICK_DELAY, MOOD_MAX_TICK_DELAY).toFloat()
        shiftTicksRemaining = -1
        bubbleTicksRemaining = -1
        currentLevel = level
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level is ClientLevel) reset(null)
    }
}
