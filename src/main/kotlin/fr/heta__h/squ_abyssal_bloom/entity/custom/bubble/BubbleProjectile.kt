package fr.heta__h.squ_abyssal_bloom.entity.custom.bubble

import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.data_component.bubble.SplatterData
import fr.heta__h.squ_abyssal_bloom.data_component.bubble.SplatterEntry
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import fr.heta__h.squ_abyssal_bloom.event.nautilus.bubble.NautilusBubbleSlowHandler
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.*

class BubbleProjectile(val entityType: EntityType<out BubbleProjectile>, level: Level) :
    Projectile(entityType, level) {

    data class BounceAxis(val x: Boolean, val y: Boolean, val z: Boolean)

    data class BubbleStageData(
        val size: Float,
        val burstRadius: Double,
        val burstDamage: Float,
        val burstKnockback: Double,
        val burstPitch: Float,
        val burstVolume: Float,
        val particleRings: Int,
        val particlePerRing: Int,
        val particleSplashCount: Int,
        val dragLateral: Double,
        val dragVertical: Double,
        val buoyancy: Double,
    )

    companion object {
        const val TICKS_TO_STAGE_1 = 12
        const val TICKS_TO_STAGE_2 = 26
        const val TICKS_TO_OVERCHARGE = 55

        const val HELD_DIST_BASE = 1.3
        const val HELD_DIST_PER_STAGE = 0.65
        const val HELD_HEIGHT_DIVISOR = 3.0

        const val CHILD_OFFSET_DIST_RATIO = 0.25
        const val CHILD_BURST_SPEED_MIN = 0.5
        const val CHILD_BURST_SPEED_RANGE = 0.5
        const val CHILD_PARENT_MOMENTUM_RETAIN = 0.3
        const val NUM_CHILDREN = 2

        const val BOUNCE_NUDGE_SCALE = 0.1
        const val BOUNCE_SPEED_RETENTION = 0.6
        const val MAX_BOUNCES = 2

        const val BURST_MIN_DIST = 0.01

        const val TRAIL_PARTICLE_CHANCE = 0.6f
        const val TRAIL_PARTICLE_VELOCITY_DAMPEN = 0.3

        const val INTER_BUBBLE_COOLDOWN_TICKS = 10
        const val ENTITY_SCAN_INFLATE = 0.1
        const val KNOCKBACK_MIN_NY = 0.1
        const val BURST_EVENT_ID: Byte = 77

        const val LERP_FACTOR = 0.7

        const val LEASH_MAX_LENGTH = 5.0
        const val LEASH_IDLE_PULL = 0.03
        const val LEASH_IDLE_MAX_VEL_Y = 0.12

        const val CLOUD_RADIUS_RATIO = 2f
        const val CLOUD_RADIUS_ON_USE = -0.05f
        const val CLOUD_WAIT_TIME = 10
        const val CLOUD_DURATION_RATIO = 0.25
        const val CHILD_CLOUD_RADIUS_RATIO = 1f

        const val EFFECT_PARTICLE_CHANCE = 0.4f

        const val TORPEDO_DECAY_BASE = 0.08

        val STAGES = arrayOf(
            BubbleStageData(
                size = 0.5f, burstRadius = 1.0, burstDamage = 2.5f,
                burstKnockback = 0.5, burstPitch = 1.2f, burstVolume = 0.8f,
                particleRings = 2, particlePerRing = 10, particleSplashCount = 6,
                dragLateral = 0.85, dragVertical = 0.98, buoyancy = 0.04,
            ),
            BubbleStageData(
                size = 1.0f, burstRadius = 1.75, burstDamage = 5.0f,
                burstKnockback = 0.75, burstPitch = 0.9f, burstVolume = 0.9f,
                particleRings = 3, particlePerRing = 16, particleSplashCount = 12,
                dragLateral = 0.90, dragVertical = 0.96, buoyancy = 0.03,
            ),
            BubbleStageData(
                size = 2.0f, burstRadius = 2.5, burstDamage = 10.0f,
                burstKnockback = 1.25, burstPitch = 0.6f, burstVolume = 1.0f,
                particleRings = 5, particlePerRing = 24, particleSplashCount = 20,
                dragLateral = 0.95, dragVertical = 0.90, buoyancy = 0.02,
            ),
        )

        fun stageFor(ticks: Int) = when {
            ticks < TICKS_TO_STAGE_1 -> 0
            ticks < TICKS_TO_STAGE_2 -> 1
            else -> 2
        }

        private val BUBBLE_STAGE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BubbleProjectile::class.java, EntityDataSerializers.INT)
        private val IS_HELD: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BubbleProjectile::class.java, EntityDataSerializers.BOOLEAN)
        private val HOLD_TICKS_SYNC: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BubbleProjectile::class.java, EntityDataSerializers.INT)
        private val ATTACHED_PLAYER_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BubbleProjectile::class.java, EntityDataSerializers.INT)
        private val EFFECT_COLOR: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BubbleProjectile::class.java, EntityDataSerializers.INT)
        private val TORPEDO_LEVEL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BubbleProjectile::class.java, EntityDataSerializers.INT)
    }

    var releaseYaw: Float = 0f
    var releaseTick: Int = -1
    var player: Player? = null

    private var cachedEffectColor: Int = 0

    var splatterEntries: List<SplatterEntry> = emptyList()
    private var isChildBubble = false

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(BUBBLE_STAGE, 0)
        builder.define(IS_HELD, false)
        builder.define(HOLD_TICKS_SYNC, 0)
        builder.define(ATTACHED_PLAYER_ID, -1)
        builder.define(EFFECT_COLOR, 0)
        builder.define(TORPEDO_LEVEL, 0)
    }

    var bubbleStage: Int
        get() = entityData.get(BUBBLE_STAGE)
        set(value) {
            entityData.set(BUBBLE_STAGE, value.coerceIn(0, STAGES.lastIndex))
            refreshDimensions()
        }

    var isHeld: Boolean
        get() = entityData.get(IS_HELD)
        set(value) {
            val previous = entityData.get(IS_HELD)
            entityData.set(IS_HELD, value)
            if (!level().isClientSide && previous != value) {
                val ownerNautilus = owner?.getEntity(level(), Entity::class.java) as? AbstractNautilus
                if (ownerNautilus != null) {
                    if (value) NautilusBubbleSlowHandler.onBubbleHeld(ownerNautilus.uuid)
                    else NautilusBubbleSlowHandler.onBubbleReleased(ownerNautilus.uuid)
                }
            }
        }

    var holdTicks: Int
        get() = entityData.get(HOLD_TICKS_SYNC)
        set(value) { entityData.set(HOLD_TICKS_SYNC, value) }

    var attachedPlayerId: Int
        get() = entityData.get(ATTACHED_PLAYER_ID)
        set(value) { entityData.set(ATTACHED_PLAYER_ID, value) }

    var effectColor: Int
        get() = entityData.get(EFFECT_COLOR)
        set(value) { entityData.set(EFFECT_COLOR, value) }

    var torpedoLevel: Int
        get() = entityData.get(TORPEDO_LEVEL)
        set(value) { entityData.set(TORPEDO_LEVEL, value) }

    var torpedoFactor: Double = 0.0

    private val stageData get() = STAGES[bubbleStage]

    override fun getDimensions(pose: Pose): EntityDimensions {
        val s = stageData.size
        return EntityDimensions.scalable(s, s)
    }

    private var bounceCount = 0
    private var isBursting = false
    private var spawnCooldown = 0
    private var interBubbleCooldown = 0

    fun applySplatter(data: SplatterData) {
        splatterEntries = data.entries
        effectColor = data.color
    }

    fun hasSplatter(): Boolean = splatterEntries.isNotEmpty()

    override fun tick() {
        super.tick()

        if (spawnCooldown > 0) spawnCooldown--
        if (interBubbleCooldown > 0) interBubbleCooldown--

        if (!isBoxFullySubmerged()) {
            if (!level().isClientSide) {
                burst()
                return
            }
        } else {
            if (!isHeld && torpedoLevel > 0 && torpedoFactor > 0.001) {
                torpedoFactor *= 1.0 - TORPEDO_DECAY_BASE * 0.5.pow((torpedoLevel - 1).toDouble())
                if (torpedoFactor < 0.001) torpedoFactor = 0.0
            }

            val baseMultiplier = if (torpedoFactor > 0.0) 0.5.pow(torpedoLevel.toDouble()) else 1.0
            val frictionMultiplier = baseMultiplier + (1.0 - baseMultiplier) * (1.0 - torpedoFactor)
            val effectiveDragLateral = 1.0 - (1.0 - stageData.dragLateral) * frictionMultiplier
            val effectiveDragVertical = 1.0 - (1.0 - stageData.dragVertical) * frictionMultiplier
            val effectiveBuoyancy = stageData.buoyancy * (1.0 - torpedoFactor)

            deltaMovement = deltaMovement.multiply(
                effectiveDragLateral, effectiveDragVertical, effectiveDragLateral
            ).add(0.0, effectiveBuoyancy, 0.0)
        }

        if (!level().isClientSide && attachedPlayerId != -1) {
            val p = level().getEntity(attachedPlayerId) as? LivingEntity

            if (p == null || !p.isAlive) {
                detachWithLeash()
            } else if (p.isShiftKeyDown) {
                detachWithLeash()
            } else {
                val dx = x - p.x
                val dy = y - p.y
                val dz = z - p.z
                val dist = sqrt(dx * dx + dy * dy + dz * dz)

                val moveTarget: Entity = if (p.isPassenger) p.rootVehicle else p

                if (dist > LEASH_MAX_LENGTH) {
                    val excess = dist - LEASH_MAX_LENGTH
                    val nx = dx / dist
                    val ny = dy / dist
                    val nz = dz / dist
                    val speed = (excess * 0.5).coerceAtMost(2.0)
                    moveTarget.deltaMovement = Vec3(
                        moveTarget.deltaMovement.x * 0.2 + nx * speed,
                        moveTarget.deltaMovement.y * 0.2 + ny * speed,
                        moveTarget.deltaMovement.z * 0.2 + nz * speed
                    )
                } else {
                    moveTarget.deltaMovement = Vec3(
                        moveTarget.deltaMovement.x,
                        (moveTarget.deltaMovement.y + LEASH_IDLE_PULL).coerceAtMost(LEASH_IDLE_MAX_VEL_Y),
                        moveTarget.deltaMovement.z
                    )
                }

                if (moveTarget is ServerPlayer) {
                    moveTarget.connection.send(ClientboundSetEntityMotionPacket(moveTarget))
                }

                if (p is ServerPlayer && p !== moveTarget) {
                    p.connection.send(ClientboundSetEntityMotionPacket(p))
                }

                if (p.boundingBox.intersects(this.boundingBox)) {
                    p.airSupply = p.maxAirSupply
                }
            }
        }

        val nextBB = boundingBox.move(deltaMovement)
        if (!level().isClientSide && !level().noCollision(this, nextBB)) {
            handleBlockHit(computeBounceAxis(), level())
            return
        }

        if (isHeld) {
            handleHeldTick()
            return
        }

        if (level().isClientSide) {
            if (random.nextFloat() < TRAIL_PARTICLE_CHANCE) {
                spawnTrailParticle()
            }
            if (effectColor != 0 && random.nextFloat() < EFFECT_PARTICLE_CHANCE) {
                spawnEffectParticle()
            }
        }

        if (!level().isClientSide && interBubbleCooldown <= 0) {
            val nearby = level().getEntities(this, boundingBox.inflate(ENTITY_SCAN_INFLATE)) { canHitEntity(it) }
            if (nearby.isNotEmpty()) {
                burstWithSplit()
                return
            }
        }

        setPos(x + deltaMovement.x, y + deltaMovement.y, z + deltaMovement.z)
    }

    private fun handleHeldTick() {
        val o = owner ?: run { if (!level().isClientSide) discard(); return }
        val nautilus = o.getEntity(level(), Entity::class.java) as? AbstractNautilus
            ?: run { if (!level().isClientSide) discard(); return }

        if (!level().isClientSide) {
            val newTicks = holdTicks + 1
            holdTicks = newTicks

            val newStage = when {
                newTicks < TICKS_TO_STAGE_1 -> 0
                newTicks < TICKS_TO_STAGE_2 -> 1
                else -> 2
            }
            if (newStage != bubbleStage) {
                bubbleStage = newStage
                playSound(ModSounds.BUBBLE_PROJECTILE_STAGE_UP.get(), stageData.burstVolume, stageData.burstPitch)
            }

            if (newTicks >= TICKS_TO_OVERCHARGE) {
                burstWithSplit()
                return
            }
        }

        val controller = nautilus.controllingPassenger ?: nautilus
        val yawRad = Math.toRadians(controller.yRot.toDouble())
        val dirX = -sin(yawRad)
        val dirZ = cos(yawRad)
        val dist = HELD_DIST_BASE + bubbleStage * HELD_DIST_PER_STAGE

        val targetX = nautilus.x + dirX * dist
        val targetY = nautilus.y + nautilus.bbHeight / HELD_HEIGHT_DIVISOR
        val targetZ = nautilus.z + dirZ * dist

        val nautilusVel = nautilus.deltaMovement

        deltaMovement = Vec3(
            nautilusVel.x + (targetX - x - nautilusVel.x) * LERP_FACTOR,
            nautilusVel.y + (targetY - y - nautilusVel.y) * LERP_FACTOR,
            nautilusVel.z + (targetZ - z - nautilusVel.z) * LERP_FACTOR
        )
        setPos(x + deltaMovement.x, y + deltaMovement.y, z + deltaMovement.z)

        val yaw = controller.yRot
        val pitch = controller.xRot
        this.setRot(yaw, pitch)
        this.xRotO = pitch
        this.yRotO = yaw
    }

    fun release(velocity: Vec3) {
        isHeld = false
        releaseTick = tickCount
        val nautilus = owner?.getEntity(level(), Entity::class.java) as? AbstractNautilus
        val controller = nautilus?.controllingPassenger ?: nautilus
        releaseYaw = controller?.yRot ?: 0f
        deltaMovement = velocity
        playSound(ModSounds.BUBBLE_PROJECTILE_LAUNCH.get(), stageData.burstVolume, stageData.burstPitch)
        player = nautilus?.controllingPassenger as? Player
    }

    private fun computeBounceAxis(): BounceAxis {
        val blockedX = !level().noCollision(this, boundingBox.move(Vec3(deltaMovement.x, 0.0, 0.0)))
        val blockedY = !level().noCollision(this, boundingBox.move(Vec3(0.0, deltaMovement.y, 0.0)))
        val blockedZ = !level().noCollision(this, boundingBox.move(Vec3(0.0, 0.0, deltaMovement.z)))
        return BounceAxis(blockedX, blockedY, blockedZ)
    }

    private fun handleBlockHit(axis: BounceAxis, level: Level) {
        if (bounceCount >= MAX_BOUNCES) { burstWithSplit(); return }

        playSound(ModSounds.BUBBLE_PROJECTILE_BOUNCING.get(), stageData.burstVolume, stageData.burstPitch)

        val vel = deltaMovement
        deltaMovement = Vec3(
            if (axis.x) -vel.x else vel.x,
            if (axis.y) -vel.y else vel.y,
            if (axis.z) -vel.z else vel.z,
        ).scale(BOUNCE_SPEED_RETENTION)

        val nudge = deltaMovement.normalize().scale(BOUNCE_NUDGE_SCALE)
        setPos(x + nudge.x, y + nudge.y, z + nudge.z)
        bounceCount++
    }

    override fun onHit(result: HitResult) {}

    fun burstWithSplit() {
        if (isBursting) return
        if (bubbleStage > 0 && !level().isClientSide) spawnChildBubbles()
        burst()
    }

    fun burstNoSplit() = burst()

    private fun spawnChildBubbles() {
        val level = level() as? ServerLevel ?: return
        val childStage = bubbleStage - 1

        for (i in 0 until NUM_CHILDREN) {
            val child = BubbleProjectile(entityType, level)
            child.bubbleStage = childStage

            val randomDir = Vec3(
                random.nextDouble() - 0.5,
                random.nextDouble() - 0.5,
                random.nextDouble() - 0.5
            ).normalize()

            val offsetDist = stageData.size * CHILD_OFFSET_DIST_RATIO
            child.setPos(
                x + randomDir.x * offsetDist,
                y + randomDir.y * offsetDist,
                z + randomDir.z * offsetDist
            )

            val burstSpeed = CHILD_BURST_SPEED_MIN + random.nextDouble() * CHILD_BURST_SPEED_RANGE
            child.deltaMovement = randomDir.scale(burstSpeed).add(deltaMovement.scale(CHILD_PARENT_MOMENTUM_RETAIN))
            child.owner = owner
            child.player = player
            child.interBubbleCooldown = INTER_BUBBLE_COOLDOWN_TICKS
            child.holdTicks = holdTicks
            child.isChildBubble = true

            if (hasSplatter()) {
                child.splatterEntries = splatterEntries
                child.effectColor = effectColor
            }

            level.addFreshEntity(child)
        }
    }

    private fun burst() {
        if (isBursting) return
        isBursting = true
        if (isHeld) isHeld = false
        if (attachedPlayerId != -1) detachWithLeash()

        val lvl = level()
        playSound(ModSounds.BUBBLE_PROJECTILE_BURST.get(), stageData.burstVolume, stageData.burstPitch)

        val chainTargets = if (lvl is ServerLevel) {
            lvl.getEntitiesOfClass(BubbleProjectile::class.java, boundingBox.inflate(stageData.burstRadius))
            { it !== this && !it.isBursting && it.interBubbleCooldown <= 0 }
        } else emptyList()

        if (lvl is ServerLevel) {
            val targets = lvl.getEntitiesOfClass(
                LivingEntity::class.java, boundingBox.inflate(stageData.burstRadius)
            ).toMutableSet()

            targets.toList().forEach { e ->
                e.passengers.filterIsInstance<LivingEntity>().forEach { targets.add(it) }
            }

            targets.forEach { target ->
                val dx = target.x - x
                val dy = (target.y + target.eyeHeight) - y
                val dz = target.z - z
                val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(BURST_MIN_DIST)

                val coef = if (target is AbstractNautilus) 0.33f else if (target is BrineEntity) 0.25f else 1f
                val damage = stageData.burstDamage * coef

                target.hurtServer(lvl, bubbleBurstSource(), damage)
                target.deltaMovement = target.deltaMovement.add(
                    (dx / dist) * stageData.burstKnockback,
                    (dy / dist).coerceAtLeast(KNOCKBACK_MIN_NY) * stageData.burstKnockback,
                    (dz / dist) * stageData.burstKnockback,
                )

                if (target.isUnderWater) target.airSupply = target.maxAirSupply
                target.hurtMarked = true

                if (target.controllingPassenger is ServerPlayer)
                    (target.controllingPassenger as ServerPlayer).connection.send(ClientboundSetEntityMotionPacket(target))
                if (target is ServerPlayer) target.connection.send(ClientboundSetEntityMotionPacket(target))

                if (hasSplatter()) {
                    for (entry in splatterEntries) {
                        target.addEffect(MobEffectInstance(entry.effect, entry.duration / 2, entry.amplifier))
                    }
                }
            }

            if (hasSplatter()) {
                spawnLingeringCloud(lvl)
            }
        }

        lvl.broadcastEntityEvent(this, BURST_EVENT_ID)
        discard()
        chainTargets.forEach { it.burstWithSplit() }
    }

    private fun spawnLingeringCloud(level: ServerLevel) {

        if (splatterEntries.isEmpty()) return

        val cloud = AreaEffectCloud(level, x, y, z)
        val radiusRatio = if (isChildBubble) CHILD_CLOUD_RADIUS_RATIO else CLOUD_RADIUS_RATIO
        val maxDuration = splatterEntries.maxOf { it.duration }

        cloud.apply {
            radius = stageData.burstRadius.toFloat() * radiusRatio
            radiusOnUse = CLOUD_RADIUS_ON_USE
            waitTime = CLOUD_WAIT_TIME
            duration = (maxDuration * CLOUD_DURATION_RATIO).toInt().coerceAtLeast(60).coerceAtMost(200)
            radiusPerTick = -radius / duration.toFloat()
        }

        for (entry in splatterEntries) {
            cloud.addEffect(MobEffectInstance(entry.effect, entry.duration / 4, entry.amplifier))
        }

        cloud.owner = player ?: owner?.getEntity(level, Entity::class.java) as? LivingEntity
        level.addFreshEntity(cloud)
    }

    override fun hurtServer(p_376191_: ServerLevel, p_376581_: DamageSource, p_376638_: Float): Boolean {
        burstWithSplit()
        return true
    }

    private fun spawnTrailParticle() {
        val vel = deltaMovement
        val radius = bbWidth / 2.0
        val theta = random.nextDouble() * 2 * Math.PI
        val phi = acos(2 * random.nextDouble() - 1)
        val px = x + sin(phi) * cos(theta) * radius
        val py = y + bbHeight / 2.0 + sin(phi) * sin(theta) * radius
        val pz = z + cos(phi) * radius

        level().addParticle(
            ParticleTypes.BUBBLE_POP,
            px, py, pz,
            -vel.x * TRAIL_PARTICLE_VELOCITY_DAMPEN,
            -vel.y * TRAIL_PARTICLE_VELOCITY_DAMPEN,
            -vel.z * TRAIL_PARTICLE_VELOCITY_DAMPEN,
        )

        val col = cachedEffectColor
        if (col != 0 && random.nextFloat() < 0.35f) {
            level().addParticle(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, col),
                px, py, pz, 0.0, 0.01, 0.0
            )
        }
    }

    private fun spawnEffectParticle() {
        val col = effectColor
        if (col == 0) return
        val radius = bbWidth / 2.0
        val ox = (random.nextDouble() - 0.5) * radius * 2
        val oy = random.nextDouble() * bbHeight
        val oz = (random.nextDouble() - 0.5) * radius * 2
        level().addParticle(
            ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, col),
            x + ox, y + oy, z + oz,
            0.0, 0.02, 0.0,
        )
    }

    override fun handleEntityEvent(id: Byte) {
        if (id != BURST_EVENT_ID) { super.handleEntityEvent(id); return }

        val lvl = level()
        val cx = x
        val cy = y + bbHeight / 2.0
        val cz = z
        val halfW = bbWidth / 2.0
        val halfH = bbHeight / 2.0
        val data = stageData
        val col = effectColor

        repeat(data.particleRings) { ring ->
            val progress = (ring + 1).toDouble() / data.particleRings
            val speed = 0.15 + progress * 0.25
            repeat(data.particlePerRing) { i ->
                val angle = (i.toDouble() / data.particlePerRing) * 2 * Math.PI
                val cosA = cos(angle); val sinA = sin(angle)
                val px = cx + cosA * halfW
                val py = cy + (random.nextDouble() - 0.5) * halfH
                val pz = cz + sinA * halfW
                lvl.addParticle(ParticleTypes.BUBBLE_POP, px, py, pz,
                    cosA * speed, (random.nextDouble() - 0.3) * 0.1, sinA * speed)
                if (col != 0 && random.nextFloat() < 0.4f) {
                    lvl.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, col),
                        px, py, pz, cosA * speed * 0.5, 0.02, sinA * speed * 0.5)
                }
            }
        }

        repeat(data.particleSplashCount) {
            val angle = random.nextDouble() * 2 * Math.PI
            val r = random.nextDouble() * halfW
            val px = cx + cos(angle) * r
            val pz = cz + sin(angle) * r
            lvl.addParticle(ParticleTypes.SPLASH, px, cy + halfH, pz,
                (random.nextDouble() - 0.5) * 0.3, 0.3 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.3)
            if (col != 0 && random.nextFloat() < 0.3f) {
                lvl.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, col),
                    px, cy + halfH, pz, 0.0, 0.05, 0.0)
            }
        }

        lvl.addParticle(ParticleTypes.BUBBLE_POP, cx, cy, cz, 0.0, 0.0, 0.0)

        if (col != 0) {
            repeat(data.particlePerRing) {
                val angle = random.nextDouble() * 2 * Math.PI
                val r = random.nextDouble() * halfW * 1.5
                val speed = 0.1 + random.nextDouble() * 0.2
                lvl.addParticle(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, col),
                    cx + cos(angle) * r, cy + (random.nextDouble() - 0.5) * halfH, cz + sin(angle) * r,
                    cos(angle) * speed, 0.05, sin(angle) * speed,
                )
            }
        }
    }

    private fun isBoxFullySubmerged(): Boolean {
        val bb = boundingBox
        val lv = level()
        if (lv.getFluidState(BlockPos.containing(bb.minX, bb.minY, bb.minZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.maxX, bb.minY, bb.minZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.minX, bb.maxY, bb.minZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.maxX, bb.maxY, bb.minZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.minX, bb.minY, bb.maxZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.maxX, bb.minY, bb.maxZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.minX, bb.maxY, bb.maxZ)).isEmpty) return false
        if (lv.getFluidState(BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ)).isEmpty) return false
        return true
    }

    override fun addAdditionalSaveData(p_422546_: ValueOutput) {
        super.addAdditionalSaveData(p_422546_)
        p_422546_.putInt("BubbleStage", bubbleStage)
        p_422546_.putInt("BounceCount", bounceCount)

        if (splatterEntries.isNotEmpty()) {
            p_422546_.putInt("SplatterCount", splatterEntries.size)
            p_422546_.putInt("SplatterColor", effectColor)
            splatterEntries.forEachIndexed { i, entry ->
                val key = BuiltInRegistries.MOB_EFFECT.getKey(entry.effect.value())
                if (key != null) {
                    p_422546_.putString("SplatterEffect$i", key.toString())
                    p_422546_.putInt("SplatterDuration$i", entry.duration)
                    p_422546_.putInt("SplatterAmplifier$i", entry.amplifier)
                    p_422546_.putInt("TorpedoLevel", torpedoLevel)
                    p_422546_.putFloat("TorpedoFactor", torpedoFactor.toFloat())
                }
            }
        }
    }

    override fun readAdditionalSaveData(p_422548_: ValueInput) {
        super.readAdditionalSaveData(p_422548_)
        bubbleStage = p_422548_.getIntOr("BubbleStage", 0)
        bounceCount = p_422548_.getIntOr("BounceCount", 0)
        if (isHeld) discard()

        val count = p_422548_.getIntOr("SplatterCount", 0)
        if (count > 0) {
            effectColor = p_422548_.getIntOr("SplatterColor", 0)
            val entries = mutableListOf<SplatterEntry>()
            for (i in 0 until count) {
                val effId = p_422548_.getStringOr("SplatterEffect$i", "")
                if (effId.isEmpty()) continue
                val id = Identifier.tryParse(effId) ?: continue
                val holder = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null) ?: continue
                entries.add(SplatterEntry(
                    holder,
                    p_422548_.getIntOr("SplatterDuration$i", 600),
                    p_422548_.getIntOr("SplatterAmplifier$i", 0),
                ))
                torpedoLevel = p_422548_.getIntOr("TorpedoLevel", 0)
                torpedoFactor = p_422548_.getFloatOr("TorpedoFactor", 0f).toDouble()
            }
            splatterEntries = entries
        }
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        if (key == BUBBLE_STAGE) refreshDimensions()
        if (key == EFFECT_COLOR) cachedEffectColor = entityData.get(EFFECT_COLOR)
        if (key == TORPEDO_LEVEL && entityData.get(TORPEDO_LEVEL) > 0) torpedoFactor = 1.0
    }

    override fun remove(reason: RemovalReason) {
        if (!level().isClientSide && isHeld) {
            (owner?.getEntity(level(), Entity::class.java) as? AbstractNautilus)?.let {
                NautilusBubbleSlowHandler.onBubbleReleased(it.uuid)
            }
        }
        super.remove(reason)
    }

    override fun isInvulnerable() = false
    override fun isPickable() = true
    override fun shouldBeSaved() = !isHeld

    fun bubbleBurstSource(): DamageSource = DamageSource(
        level().registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(ModDamagesTypes.BUBBLE_BURST),
        this,
        player ?: owner?.getEntity(level(), Entity::class.java)
    )

    private fun detachWithLeash() {
        val serverLevel = level() as? ServerLevel ?: return
        serverLevel.addFreshEntity(ItemEntity(serverLevel, x, y, z, ItemStack(Items.LEAD)))
        serverLevel.playSound(null, x, y, z, SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f)
        attachedPlayerId = -1
    }

}