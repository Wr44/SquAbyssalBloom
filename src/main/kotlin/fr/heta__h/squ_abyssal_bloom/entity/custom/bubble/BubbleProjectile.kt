package fr.heta__h.squ_abyssal_bloom.entity.custom.bubble

import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.*

class BubbleProjectile(val entityType: EntityType<out BubbleProjectile>, level: Level) :
    Projectile(entityType, level) {

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

        
        const val SPAWN_COOLDOWN_TICKS = 10
        const val INTER_BUBBLE_COOLDOWN_TICKS = 10
        const val ENTITY_SCAN_INFLATE = 0.1
        const val KNOCKBACK_MIN_NY = 0.1
        const val BURST_EVENT_ID: Byte = 77

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
    }

    var releaseYaw: Float = 0f
    var releaseTick: Int = -1
    var player: Player? = null

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(BUBBLE_STAGE, 0)
        builder.define(IS_HELD, false)
        builder.define(HOLD_TICKS_SYNC, 0)
    }

    var bubbleStage: Int
        get() = entityData.get(BUBBLE_STAGE)
        set(value) {
            entityData.set(BUBBLE_STAGE, value.coerceIn(0, STAGES.lastIndex))
            refreshDimensions()
        }

    var isHeld: Boolean
        get() = entityData.get(IS_HELD)
        set(value) { entityData.set(IS_HELD, value) }

    var holdTicks: Int
        get() = entityData.get(HOLD_TICKS_SYNC)
        set(value) { entityData.set(HOLD_TICKS_SYNC, value) }

    private val stageData get() = STAGES[bubbleStage]

    override fun getDimensions(pose: Pose): EntityDimensions {
        val s = stageData.size
        return EntityDimensions.scalable(s, s)
    }

    private var bounceCount = 0
    private var isBursting = false
    private var spawnCooldown = 0
    private var interBubbleCooldown = 0

    override fun tick() {
        super.tick()

        if (spawnCooldown > 0) spawnCooldown--
        if (interBubbleCooldown > 0) interBubbleCooldown--

        if (!isBoxFullySubmerged()) {
            if (!level().isClientSide) burst()
        } else {
            deltaMovement = deltaMovement.multiply(
                stageData.dragLateral, stageData.dragVertical, stageData.dragLateral
            ).add(0.0, stageData.buoyancy, 0.0)
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

        if (level().isClientSide && random.nextFloat() < TRAIL_PARTICLE_CHANCE) {
            spawnTrailParticle()
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
        val nautilus = o.getEntity(level(), Entity::class.java) as? AbstractNautilus ?: run { if (!level().isClientSide) discard(); return }

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
                playSound(
                    ModSounds.BUBBLE_PROJECTILE_STAGE_UP.get(),
                    stageData.burstVolume, stageData.burstPitch
                )
            }

            if (newTicks >= TICKS_TO_OVERCHARGE) {
                burstWithSplit()
                return
            }
        }

        val controller = nautilus.controllingPassenger ?: nautilus
        val yawRad = Math.toRadians(controller.yRot.toDouble())
        val dirX = -sin(yawRad)
        val dirZ =  cos(yawRad)
        val dist = HELD_DIST_BASE + bubbleStage * HELD_DIST_PER_STAGE

        setPos(
            nautilus.x + dirX * dist,
            nautilus.y + nautilus.bbHeight / HELD_HEIGHT_DIVISOR,
            nautilus.z + dirZ * dist
        )

        deltaMovement = Vec3.ZERO

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

            val dirX = random.nextDouble() - 0.5
            val dirY = random.nextDouble() - 0.5
            val dirZ = random.nextDouble() - 0.5
            val randomDir = Vec3(dirX, dirY, dirZ).normalize()

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
            child.interBubbleCooldown = SPAWN_COOLDOWN_TICKS
            child.holdTicks = holdTicks
            level.addFreshEntity(child)
        }
    }

    private fun burst() {
        if (isBursting) return
        isBursting = true

        val lvl = level()
        playSound(ModSounds.BUBBLE_PROJECTILE_BURST.get(), stageData.burstVolume, stageData.burstPitch)

        val chainTargets = if (lvl is ServerLevel) {
            lvl.getEntitiesOfClass(BubbleProjectile::class.java, boundingBox.inflate(stageData.burstRadius))
            { it !== this && !it.isBursting && it.interBubbleCooldown <= 0 }
        } else emptyList()

        if (lvl is ServerLevel) {
            val knockbackStrength = stageData.burstKnockback
            val radius = stageData.burstRadius
            val baseDamage = stageData.burstDamage

            val targets = lvl.getEntitiesOfClass(
                LivingEntity::class.java, boundingBox.inflate(radius)
            ).toMutableSet()

            targets.toList().forEach { e ->
                e.passengers.filterIsInstance<LivingEntity>().forEach { targets.add(it) }
            }

            targets.forEach { target ->
                val dx = target.x - x
                val dy = (target.y + target.eyeHeight) - y
                val dz = target.z - z
                val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(BURST_MIN_DIST)

                target.hurtServer(lvl, bubbleBurstSource(), baseDamage)
                target.deltaMovement = target.deltaMovement.add(
                    (dx / dist) * knockbackStrength,
                    (dy / dist).coerceAtLeast(KNOCKBACK_MIN_NY) * knockbackStrength,
                    (dz / dist) * knockbackStrength,
                )

                if (target.isUnderWater) {
                    target.airSupply = target.maxAirSupply
                }
                target.hurtMarked = true

                val rider = target.controllingPassenger
                if (rider is ServerPlayer) {
                    rider.connection.send(ClientboundSetEntityMotionPacket(target))
                }
                if (target is ServerPlayer) {
                    target.connection.send(ClientboundSetEntityMotionPacket(target))
                }
            }
        }

        lvl.broadcastEntityEvent(this, BURST_EVENT_ID)
        discard()
        chainTargets.forEach { it.burstWithSplit() }
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
        val sx = sin(phi) * cos(theta)
        val sy = sin(phi) * sin(theta)
        val sz = cos(phi)

        level().addParticle(
            ParticleTypes.BUBBLE_POP,
            x + sx * radius,
            y + bbHeight / 2.0 + sy * radius,
            z + sz * radius,
            -vel.x * TRAIL_PARTICLE_VELOCITY_DAMPEN,
            -vel.y * TRAIL_PARTICLE_VELOCITY_DAMPEN,
            -vel.z * TRAIL_PARTICLE_VELOCITY_DAMPEN,
        )
    }

    override fun handleEntityEvent(id: Byte) {
        if (id != BURST_EVENT_ID) {
            super.handleEntityEvent(id)
            return
        }

        val lvl = level()
        val cx = x
        val cy = y + bbHeight / 2.0
        val cz = z
        val halfW = bbWidth / 2.0
        val halfH = bbHeight / 2.0
        val data = stageData

        repeat(data.particleRings) { ring ->
            val progress = (ring + 1).toDouble() / data.particleRings
            val speed = 0.15 + progress * 0.25
            repeat(data.particlePerRing) { i ->
                val angle = (i.toDouble() / data.particlePerRing) * 2 * Math.PI
                val cosA = cos(angle)
                val sinA = sin(angle)
                lvl.addParticle(
                    ParticleTypes.BUBBLE_POP,
                    cx + cosA * halfW,
                    cy + (random.nextDouble() - 0.5) * halfH,
                    cz + sinA * halfW,
                    cosA * speed,
                    (random.nextDouble() - 0.3) * 0.1,
                    sinA * speed,
                )
            }
        }

        repeat(data.particleSplashCount) {
            val angle = random.nextDouble() * 2 * Math.PI
            val r = random.nextDouble() * halfW
            lvl.addParticle(
                ParticleTypes.SPLASH,
                cx + cos(angle) * r,
                cy + halfH,
                cz + sin(angle) * r,
                (random.nextDouble() - 0.5) * 0.3,
                0.3 + random.nextDouble() * 0.4,
                (random.nextDouble() - 0.5) * 0.3,
            )
        }

        lvl.addParticle(ParticleTypes.BUBBLE_POP, cx, cy, cz, 0.0, 0.0, 0.0)
    }

    private fun isBoxFullySubmerged(): Boolean {
        val bb = boundingBox
        return listOf(
            Triple(bb.minX, bb.minY, bb.minZ), Triple(bb.maxX, bb.minY, bb.minZ),
            Triple(bb.minX, bb.maxY, bb.minZ), Triple(bb.maxX, bb.maxY, bb.minZ),
            Triple(bb.minX, bb.minY, bb.maxZ), Triple(bb.maxX, bb.minY, bb.maxZ),
            Triple(bb.minX, bb.maxY, bb.maxZ), Triple(bb.maxX, bb.maxY, bb.maxZ),
        ).all { (x, y, z) ->
            !level().getFluidState(BlockPos.containing(x, y, z)).isEmpty
        }
    }

    override fun addAdditionalSaveData(p_422546_: ValueOutput) {
        super.addAdditionalSaveData(p_422546_)
        p_422546_.putInt("BubbleStage", bubbleStage)
        p_422546_.putInt("BounceCount", bounceCount)
    }

    override fun readAdditionalSaveData(p_422548_: ValueInput) {
        super.readAdditionalSaveData(p_422548_)
        bubbleStage = p_422548_.getIntOr("BubbleStage", 0)
        bounceCount = p_422548_.getIntOr("BounceCount", 0)
        if (isHeld) discard()
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        if (key == BUBBLE_STAGE) refreshDimensions()
    }

    override fun isInvulnerable() = false
    override fun isPickable() = true
    override fun shouldBeSaved() = !isHeld

    fun bubbleBurstSource(): DamageSource {
        return DamageSource(
            level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ModDamagesTypes.BUBBLE_BURST),
            this,
            player ?: owner?.getEntity(level(), Entity::class.java)
        )
    }
}