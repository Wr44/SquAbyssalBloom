package fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.control

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.ln

class CrystalJellyPulseController(private val crystalJelly: CrystalJellyEntity) {

    private companion object {
        const val PULSE_RELEASE_TICKS =
            CrystalJellyEntity.PULSE_ANIMATION_TICKS - CrystalJellyEntity.PULSE_CHARGE_TICKS
        const val PULSE_SPEED_DECAY = 2.0f
        const val TILT_TURN_DEGREES_PER_TICK = 4.0f
        const val MAX_SURFACE_RETREAT_CYCLES = 12
        const val MAX_DRIFT_CYCLES = 8
        const val PROPULSION_VOLUME_SCALE = 0.7
        const val FLOOR_CLEARANCE = 0.75
    }

    private var started = false
    private var stranded = false
    private var aimed = false
    private var reaimedInCurrentPulse = false
    private var remainingPulses = 0
    private var driftCycles = 0
    private var surfaceRetreatTargetY: Double? = null
    private var retreatedFromSurface = false
    private var targetTiltDegrees = 0.0f
    private var pulsePeakSpeed = 0.0
    private var pulseStrength = 0.0

    private val pulseProfileSum = (0 until PULSE_RELEASE_TICKS).sumOf { releaseTick ->
        pulseSpeedAt(releaseTick, 1.0)
    }

    fun tick() {
        if (!started) {
            started = true
            beginDrift()
            return
        }

        if (!crystalJelly.isInWater) {
            if (!stranded) {
                stranded = true
                beginDrift()
            }
            crystalJelly.xRot = Mth.approachDegrees(crystalJelly.xRot, 0.0f, TILT_TURN_DEGREES_PER_TICK)
            holdYaw()
            return
        }

        if (stranded) {
            stranded = false
            beginDrift()
            return
        }

        if (!crystalJelly.isPulsing) {
            if (!isDriftComplete()) return
            beginBurst()
        }

        tickPulse()
    }

    private fun tickPulse() {
        val cycleTick = crystalJelly.pulseCycleTick
        if (cycleTick == 0 && !beginPulse()) return

        crystalJelly.xRot = Mth.approachDegrees(
            crystalJelly.xRot,
            targetTiltDegrees,
            TILT_TURN_DEGREES_PER_TICK
        )

        if (cycleTick < CrystalJellyEntity.PULSE_CHARGE_TICKS) {
            crystalJelly.deltaMovement = Vec3.ZERO
            return
        }

        if (!reaimedInCurrentPulse && isObstructed()) {
            reaimedInCurrentPulse = true
            aim()
        }

        val releaseTick = cycleTick - CrystalJellyEntity.PULSE_CHARGE_TICKS
        if (releaseTick == 0) playPropulsionSound()
        crystalJelly.deltaMovement = bodyAxis().scale(pulseSpeedAt(releaseTick, pulsePeakSpeed))
    }

    private fun beginPulse(): Boolean {
        if (isNearSurface()) {
            beginSurfaceRetreat()
            return false
        }
        if (remainingPulses <= 0) {
            beginDrift()
            return false
        }

        remainingPulses--
        reaimedInCurrentPulse = false
        pulseStrength = crystalJelly.random.nextDouble()
        pulsePeakSpeed = pulseDistanceAt(pulseStrength) / pulseProfileSum
        return true
    }

    private fun holdYaw() {
        crystalJelly.yRot = crystalJelly.yBodyRot
        crystalJelly.yHeadRot = crystalJelly.yBodyRot
    }

    private fun aim() {
        val random = crystalJelly.random
        crystalJelly.yRot = random.nextFloat() * 360.0f
        targetTiltDegrees = random.nextFloat() * ModServerConfig.CRYSTAL_JELLY_MAX_TILT_DEGREES.get().toFloat()
        aimed = true
    }

    private fun pulseSpeedAt(releaseTick: Int, peakSpeed: Double): Double =
        ModUtilities.decayingImpulseSpeed(
            releaseTick.toFloat(),
            PULSE_RELEASE_TICKS.toFloat(),
            peakSpeed.toFloat(),
            PULSE_SPEED_DECAY
        ).toDouble()

    private fun playPropulsionSound() {
        ModUtilities.playWaterPropulsion(
            crystalJelly,
            ModSounds.CRYSTAL_JELLY_PROPULSION.get(),
            pulseStrength,
            SoundSource.NEUTRAL,
            PROPULSION_VOLUME_SCALE
        )
    }

    private fun pulseDistanceAt(strength: Double): Double {
        val configured = ModServerConfig.CRYSTAL_JELLY_MIN_PULSE_DISTANCE.get() to
            ModServerConfig.CRYSTAL_JELLY_MAX_PULSE_DISTANCE.get()
        val minimum = minOf(configured.first, configured.second)
        val maximum = maxOf(configured.first, configured.second)
        return minimum + strength * (maximum - minimum)
    }

    private fun isObstructed(): Boolean =
        crystalJelly.horizontalCollision || (crystalJelly.verticalCollision && !crystalJelly.onGround())

    private fun bodyAxis(): Vec3 {
        val yawRadians = (crystalJelly.yBodyRot * Mth.DEG_TO_RAD).toDouble()
        val tiltRadians = (crystalJelly.xRot * Mth.DEG_TO_RAD).toDouble()
        val sinTilt = Mth.sin(tiltRadians).toDouble()
        return Vec3(
            -sinTilt * Mth.sin(yawRadians),
            Mth.cos(tiltRadians).toDouble(),
            sinTilt * Mth.cos(yawRadians)
        )
    }

    fun interruptPulse() {
        remainingPulses = 0
        reaimedInCurrentPulse = false
        beginDrift()
    }

    private fun beginBurst() {
        surfaceRetreatTargetY = null
        val configuredPulses = ModServerConfig.CRYSTAL_JELLY_MIN_PULSES.get() to ModServerConfig.CRYSTAL_JELLY_MAX_PULSES.get()
        remainingPulses = crystalJelly.random.nextIntBetweenInclusive(
            minOf(configuredPulses.first, configuredPulses.second),
            maxOf(configuredPulses.first, configuredPulses.second)
        )
        if (!aimed || retreatedFromSurface || crystalJelly.random.nextBoolean()) aim()
        retreatedFromSurface = false
        crystalJelly.startPhase(true)
    }

    private fun beginDrift() {
        surfaceRetreatTargetY = null
        driftCycles = drawDriftCycles()
        crystalJelly.startPhase(false)
    }

    private fun beginSurfaceRetreat() {
        val random = crystalJelly.random
        val configuredRetreat = ModServerConfig.CRYSTAL_JELLY_MIN_SURFACE_RETREAT.get() to
            ModServerConfig.CRYSTAL_JELLY_MAX_SURFACE_RETREAT.get()
        val minimumRetreat = minOf(configuredRetreat.first, configuredRetreat.second)
        val maximumRetreat = maxOf(configuredRetreat.first, configuredRetreat.second)
        val retreat = minimumRetreat + random.nextDouble() * (maximumRetreat - minimumRetreat)
        crystalJelly.startPhase(false)
        surfaceRetreatTargetY = crystalJelly.y - retreat
        retreatedFromSurface = true
    }

    private fun drawDriftCycles(): Int {
        val sampledTicks = -ln(1.0 - crystalJelly.random.nextDouble()) *
            ModServerConfig.CRYSTAL_JELLY_DRIFT_MEAN_TICKS.get().toDouble()
        return ceil(sampledTicks / CrystalJellyEntity.IDLE_ANIMATION_TICKS)
            .toInt()
            .coerceIn(1, MAX_DRIFT_CYCLES)
    }

    private fun isDriftComplete(): Boolean {
        val elapsedTicks = crystalJelly.phaseElapsedTicks
        if (elapsedTicks <= 0 || elapsedTicks % CrystalJellyEntity.IDLE_ANIMATION_TICKS != 0) return false
        if (isNearFloor()) return true

        val elapsedCycles = elapsedTicks / CrystalJellyEntity.IDLE_ANIMATION_TICKS
        val retreatTargetY = surfaceRetreatTargetY
            ?: return elapsedCycles >= driftCycles

        return crystalJelly.y <= retreatTargetY || elapsedCycles >= MAX_SURFACE_RETREAT_CYCLES
    }

    private fun isNearFloor(): Boolean =
        !crystalJelly.level().noCollision(
            crystalJelly,
            crystalJelly.boundingBox.expandTowards(0.0, -FLOOR_CLEARANCE, 0.0)
        )

    private fun isNearSurface(): Boolean {
        if (!crystalJelly.isInWater) return true
        return ModUtilities.findWaterSurface(
            crystalJelly.level(),
            crystalJelly.blockPosition()
        ) < ModServerConfig.CRYSTAL_JELLY_SURFACE_PROXIMITY.get()
    }
}
