package fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly

import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.control.CrystalJellyBodyRotationControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.control.CrystalJellyLookControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.control.CrystalJellyPulseController
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.interaction.CrystalJellyBottleHarvest
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.BodyRotationControl
import net.minecraft.world.entity.animal.fish.WaterAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.Optional
import java.util.UUID

class CrystalJellyEntity(type: EntityType<out CrystalJellyEntity>, level: Level) : WaterAnimal(type, level) {

    companion object {
        const val PULSE_ANIMATION_TICKS = 16
        const val PULSE_CHARGE_TICKS = 10
        const val IDLE_ANIMATION_TICKS = 30
        const val STRANDING_TRANSITION_TICKS = 12
        const val RECOVERY_TRANSITION_TICKS = 8

        private const val NOT_FADING = Long.MIN_VALUE

        const val TAG_BOTTLE_READY = "BottleReadyGameTime"

        private const val DEPLETED_GLOW_LOSS = 0.4f

        fun depletionAt(level: Level, bottleReadyGameTime: Long): Float {
            val remaining = bottleReadyGameTime - level.gameTime
            if (remaining <= 0L) return 0.0f
            val cooldown = ModServerConfig.CRYSTAL_JELLY_BOTTLE_HARVEST_COOLDOWN.get()
            if (cooldown <= 0) return 0.0f
            return (remaining.toFloat() / cooldown).coerceIn(0.0f, 1.0f)
        }

        fun glowStrengthAt(level: Level, bottleReadyGameTime: Long): Float =
            1.0f - DEPLETED_GLOW_LOSS * depletionAt(level, bottleReadyGameTime)

        private const val HARVEST_RECOIL_HORIZONTAL = 0.18
        private const val HARVEST_RECOIL_VERTICAL = -0.05

        const val BODY_TURN_DEGREES_PER_TICK = 18.0f

        private const val AMBIENT_PARTICLE_INTERVAL = 10
        private const val DEPLETED_PARTICLE_INTERVAL_FACTOR = 3.0f
        private const val AMBIENT_PARTICLE_SPREAD = 0.2
        private const val AMBIENT_PARTICLE_MINIMUM_HEIGHT = -0.7
        private const val AMBIENT_PARTICLE_MAXIMUM_HEIGHT = 0.05


        private val PULSING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            CrystalJellyEntity::class.java,
            EntityDataSerializers.BOOLEAN
        )
        private val PHASE_START_GAME_TIME: EntityDataAccessor<Long> = SynchedEntityData.defineId(
            CrystalJellyEntity::class.java,
            EntityDataSerializers.LONG
        )
        private val FADE_START_GAME_TIME: EntityDataAccessor<Long> = SynchedEntityData.defineId(
            CrystalJellyEntity::class.java,
            EntityDataSerializers.LONG
        )
        private val BOTTLE_READY_GAME_TIME: EntityDataAccessor<Long> = SynchedEntityData.defineId(
            CrystalJellyEntity::class.java,
            EntityDataSerializers.LONG
        )
        private val FROM_BUCKET: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            CrystalJellyEntity::class.java,
            EntityDataSerializers.BOOLEAN
        )

        fun createAttributes(): AttributeSupplier.Builder = Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.4)
    }

    val idleAnimationState = AnimationState()
    val swimAnimationState = AnimationState()
    val outOfWaterAnimationState = AnimationState()
    val strandingAnimationState = AnimationState()
    val recoveryAnimationState = AnimationState()
    val tentacleAnimationState = AnimationState()

    private val pulseController = CrystalJellyPulseController(this)

    var bottleReadyGameTime: Long
        get() = entityData.get(BOTTLE_READY_GAME_TIME)
        set(value) {
            entityData.set(BOTTLE_READY_GAME_TIME, value)
        }

    val depletion: Float
        get() = depletionAt(level(), bottleReadyGameTime)

    val glowStrength: Float
        get() = glowStrengthAt(level(), bottleReadyGameTime)

    private var submergedLastTick: Boolean? = null
    private var transitionEndTick = 0

    init {
        lookControl = CrystalJellyLookControl(this)
    }

    var waveEventId: UUID?
        get() = getData(ModAttachments.CRYSTAL_JELLY_WAVE_ID).orElse(null)
        set(value) {
            setData(ModAttachments.CRYSTAL_JELLY_WAVE_ID, Optional.ofNullable(value))
        }

    val isPulsing: Boolean
        get() = entityData.get(PULSING)

    val phaseElapsedTicks: Int
        get() = (level().gameTime - entityData.get(PHASE_START_GAME_TIME))
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()

    val pulseCycleTick: Int
        get() = phaseElapsedTicks % PULSE_ANIMATION_TICKS

    val isFadingOut: Boolean
        get() = entityData.get(FADE_START_GAME_TIME) != NOT_FADING

    val fadeOutAlpha: Float
        get() {
            val fadeStart = entityData.get(FADE_START_GAME_TIME)
            if (fadeStart == NOT_FADING) return 1.0f
            val fadeTicks = ModConfig.bioluminescenceTileFadeInTicks.coerceAtLeast(1)
            return (1.0f - (level().gameTime - fadeStart).toFloat() / fadeTicks).coerceIn(0.0f, 1.0f)
        }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(PULSING, false)
        builder.define(PHASE_START_GAME_TIME, 0L)
        builder.define(FADE_START_GAME_TIME, NOT_FADING)
        builder.define(BOTTLE_READY_GAME_TIME, 0L)
        builder.define(FROM_BUCKET, false)
    }

    override fun createBodyControl(): BodyRotationControl = CrystalJellyBodyRotationControl(this)

    fun beginFadeOut() {
        if (isFadingOut) return
        entityData.set(FADE_START_GAME_TIME, level().gameTime)
    }

    fun startPhase(pulsing: Boolean) {
        entityData.set(PULSING, pulsing)
        entityData.set(PHASE_START_GAME_TIME, level().gameTime)
    }

    override fun tick() {
        super.tick()

        if (level().isClientSide) {
            setupAnimationStates()
        } else {
            pulseController.tick()
            if (isFadingOut && fadeOutAlpha <= 0.0f) {
                discard()
                return
            }
        }

        val particleInterval = (AMBIENT_PARTICLE_INTERVAL * (1.0f + DEPLETED_PARTICLE_INTERVAL_FACTOR * depletion)).toInt()
        if (isInWater && !isFadingOut && random.nextInt(particleInterval) == 0) {
            level().addParticle(
                ModParticles.CRYSTAL_JELLY_GLOW.get(),
                getRandomX(AMBIENT_PARTICLE_SPREAD),
                y + AMBIENT_PARTICLE_MINIMUM_HEIGHT +
                    random.nextDouble() * (AMBIENT_PARTICLE_MAXIMUM_HEIGHT - AMBIENT_PARTICLE_MINIMUM_HEIGHT),
                getRandomZ(AMBIENT_PARTICLE_SPREAD),
                0.0,
                0.0,
                0.0
            )
        }
    }


    private fun setupAnimationStates() {
        if (isInWater) {
            tentacleAnimationState.startIfStopped(tickCount)
        } else {
            tentacleAnimationState.stop()
        }

        if (updateTransition()) return

        val phaseStartTick = tickCount - phaseElapsedTicks
        when {
            !isInWater -> {
                swimAnimationState.stop()
                idleAnimationState.stop()
                outOfWaterAnimationState.startIfStopped(transitionEndTick)
            }
            isPulsing -> {
                outOfWaterAnimationState.stop()
                idleAnimationState.stop()
                swimAnimationState.startIfStopped(phaseStartTick)
            }
            else -> {
                outOfWaterAnimationState.stop()
                swimAnimationState.stop()
                idleAnimationState.startIfStopped(phaseStartTick)
            }
        }
    }

    private fun updateTransition(): Boolean {
        val submerged = isInWater
        if (submergedLastTick != submerged) {
            val firstObservation = submergedLastTick == null
            submergedLastTick = submerged
            if (!firstObservation) beginTransition(submerged)
        }

        if (tickCount >= transitionEndTick) {
            strandingAnimationState.stop()
            recoveryAnimationState.stop()
            return false
        }

        idleAnimationState.stop()
        swimAnimationState.stop()
        outOfWaterAnimationState.stop()
        return true
    }

    private fun beginTransition(submerged: Boolean) {
        if (submerged) {
            strandingAnimationState.stop()
            recoveryAnimationState.start(tickCount)
            transitionEndTick = tickCount + RECOVERY_TRANSITION_TICKS
        } else {
            recoveryAnimationState.stop()
            strandingAnimationState.start(tickCount)
            transitionEndTick = tickCount + STRANDING_TRANSITION_TICKS
        }
    }

    fun isFromBucket(): Boolean = entityData.get(FROM_BUCKET)

    fun setFromBucketFlag(fromBucket: Boolean) {
        entityData.set(FROM_BUCKET, fromBucket)
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val harvest = CrystalJellyBottleHarvest.tryHarvest(this, player, hand)
        if (harvest != InteractionResult.PASS) return harvest
        return super.mobInteract(player, hand)
    }

    fun reactToHarvest(player: Player) {
        pulseController.interruptPulse()

        val away = position().subtract(player.position()).normalize()
        deltaMovement = deltaMovement.add(away.x * HARVEST_RECOIL_HORIZONTAL, HARVEST_RECOIL_VERTICAL, away.z * HARVEST_RECOIL_HORIZONTAL)
        hurtMarked = true
    }

    override fun getHurtSound(source: DamageSource): SoundEvent = ModSounds.CRYSTAL_JELLY_HURT.get()

    override fun getDeathSound(): SoundEvent = ModSounds.CRYSTAL_JELLY_DEATH.get()

    override fun requiresCustomPersistence(): Boolean = super.requiresCustomPersistence() || isFromBucket()

    override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean =
        !isFromBucket() && !hasCustomName()

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putBoolean("FromBucket", isFromBucket())
        output.putLong(TAG_BOTTLE_READY, bottleReadyGameTime)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        setFromBucketFlag(input.getBooleanOr("FromBucket", false))
        bottleReadyGameTime = input.getLongOr(TAG_BOTTLE_READY, 0L)
    }
}
