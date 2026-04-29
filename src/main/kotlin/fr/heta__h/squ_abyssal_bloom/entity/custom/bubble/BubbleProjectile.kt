package fr.heta__h.squ_abyssal_bloom.entity.custom.bubble

import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.*

class BubbleProjectile(val entityType: EntityType<out BubbleProjectile>, level: Level) :
    Projectile(entityType, level) {

    companion object {

        val STAGES = arrayOf(
            BubbleStageData(
                size = 0.5f,
                burstRadius = 1.0,
                burstDamage = 2.5f,
                burstKnockback = 0.5,
                burstPitch = 1.2f,
                burstVolume = 0.8f,
                particleRings = 2,
                particlePerRing = 10,
                particleSplashCount = 6,
                dragLateral = 0.85,
                dragVertical = 0.96,
                buoyancy = 0.04,
            ),
            BubbleStageData(
                size = 1.0f,
                burstRadius = 1.75,
                burstDamage = 5.0f,
                burstKnockback = 0.75,
                burstPitch = 0.9f,
                burstVolume = 0.9f,
                particleRings = 3,
                particlePerRing = 16,
                particleSplashCount = 12,
                dragLateral = 0.80,
                dragVertical = 0.94,
                buoyancy = 0.03,
                ),
            BubbleStageData(
                size = 2.0f,
                burstRadius = 2.5,
                burstDamage = 10.0f,
                burstKnockback = 1.25,
                burstPitch = 0.6f,
                burstVolume = 1.0f,
                particleRings = 5,
                particlePerRing = 24,
                particleSplashCount = 20,
                dragLateral = 0.70,
                dragVertical = 0.90,
                buoyancy = 0.02,
            ),
        )

        private val BUBBLE_STAGE: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            BubbleProjectile::class.java,
            EntityDataSerializers.INT
        )

        const val DRAG_LATERAL = 0.85
        const val DRAG_VERTICAL = 0.96
        const val BUOYANCY = 0.04
        const val ENTITY_SCAN_INFLATE = 0.1
        const val KNOCKBACK_MIN_NY = 0.1

        const val BOUNCE_SPEED_RETENTION = 0.6
        const val MAX_BOUNCES = 2

        const val SPLIT_ANGLE_DEG = 15.0
        const val TRAIL_PARTICLE_CHANCE = 0.6f

        const val SPAWN_COOLDOWN_TICKS = 10
        const val INTER_BUBBLE_COOLDOWN_TICKS = 10

        const val BURST_EVENT_ID: Byte = 77
    }


    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(BUBBLE_STAGE, 0)
    }

    var bubbleStage: Int
        get() = entityData.get(BUBBLE_STAGE)
        set(value) {
            entityData.set(BUBBLE_STAGE, value.coerceIn(0, STAGES.lastIndex))
            refreshDimensions()
        }

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

        if (level().isClientSide && random.nextFloat() < TRAIL_PARTICLE_CHANCE) {
            spawnTrailParticle()
        }

        if (!level().isClientSide && interBubbleCooldown <= 0) {
            val nearby = level().getEntities(this, boundingBox.inflate(ENTITY_SCAN_INFLATE)) {
                canHitEntity(it)
            }
            if (nearby.isNotEmpty()) {
                burstWithSplit()
                return
            }
        }

        val nextBB = boundingBox.move(deltaMovement)
        val blockHit = !level().isClientSide && level().noCollision(this, nextBB).not()

        if (blockHit) {
            handleBlockHit(computeBounceAxis(), level())
            return
        }

        setPos(x + deltaMovement.x, y + deltaMovement.y, z + deltaMovement.z)

        if (!isBoxFullySubmerged()) {
            if (!level().isClientSide) burst()
        } else {
            deltaMovement = deltaMovement.multiply(
                stageData.dragLateral,
                stageData.dragVertical,
                stageData.dragLateral
            ).add(0.0, stageData.buoyancy, 0.0)
        }
    }


    private fun computeBounceAxis(): BounceAxis {
        val dx = Vec3(deltaMovement.x, 0.0, 0.0)
        val dy = Vec3(0.0, deltaMovement.y, 0.0)
        val dz = Vec3(0.0, 0.0, deltaMovement.z)

        val blockedX = !level().noCollision(this, boundingBox.move(dx))
        val blockedY = !level().noCollision(this, boundingBox.move(dy))
        val blockedZ = !level().noCollision(this, boundingBox.move(dz))

        return BounceAxis(blockedX, blockedY, blockedZ)
    }


    private fun handleBlockHit(axis: BounceAxis, level: Level) {
        if (bounceCount >= MAX_BOUNCES) {
            burstWithSplit()
            return
        }

        level.playSound(
            null, x, y, z,
            ModSounds.BUBBLE_PROJECTILE_BOUNCING,
            SoundSource.HOSTILE,
            stageData.burstVolume,
            stageData.burstPitch,
        )

        val vel = deltaMovement
        deltaMovement = Vec3(
            if (axis.x) -vel.x else vel.x,
            if (axis.y) -vel.y else vel.y,
            if (axis.z) -vel.z else vel.z,
        ).scale(BOUNCE_SPEED_RETENTION)

        val nudge = deltaMovement.normalize().scale(0.1)
        setPos(x + nudge.x, y + nudge.y, z + nudge.z)

        bounceCount++
    }

    override fun onHit(result: HitResult) {}

    private fun burstWithSplit() {
        if (isBursting) return

        if (bubbleStage > 0 && !level().isClientSide) {
            spawnChildBubbles()
        }
        burst()
    }

    private fun spawnChildBubbles() {
        val level = level() as? ServerLevel ?: return
        val childStage = bubbleStage - 1

        val numChildren = 2

        for (i in 0 until numChildren) {
            val child = BubbleProjectile(entityType, level)
            child.bubbleStage = childStage

            val dirX = random.nextDouble() - 0.5
            val dirY = random.nextDouble() - 0.5
            val dirZ = random.nextDouble() - 0.5
            val randomDir = Vec3(dirX, dirY, dirZ).normalize()

            val offsetDist = stageData.size * 0.25
            child.setPos(
                x + randomDir.x * offsetDist,
                y + randomDir.y * offsetDist,
                z + randomDir.z * offsetDist
            )

            val burstSpeed = 0.5 + random.nextDouble() * 0.5

            child.deltaMovement = randomDir.scale(burstSpeed).add(deltaMovement.scale(0.3))

            child.owner = owner
            child.interBubbleCooldown = SPAWN_COOLDOWN_TICKS
            level.addFreshEntity(child)
        }
    }

    private fun burst() {

        if (isBursting) return
        isBursting = true

        val lvl = level()

        lvl.playSound(
            null, x, y, z,
            ModSounds.BUBBLE_PROJECTILE_BURST,
            SoundSource.HOSTILE,
            stageData.burstVolume,
            stageData.burstPitch,
        )

        val chainTargets = if (lvl is ServerLevel) {
            lvl.getEntitiesOfClass(
                BubbleProjectile::class.java,
                boundingBox.inflate(stageData.burstRadius)
            ) { it !== this && !it.isBursting && it.interBubbleCooldown <= 0 }
        } else {
            emptyList()
        }

        if (lvl is ServerLevel) {
            val radius = stageData.burstRadius
            val baseDamage = stageData.burstDamage
            val knockbackStrength = stageData.burstKnockback

            lvl.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity::class.java,
                boundingBox.inflate(radius)
            ).forEach { target ->
                val dx = target.x - x
                val dy = (target.y + target.eyeHeight) - y
                val dz = target.z - z
                val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01)

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
            -vel.x * 0.3,
            -vel.y * 0.3,
            -vel.z * 0.3,
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
    }


    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        if (key == BUBBLE_STAGE) refreshDimensions()
    }


    override fun isInvulnerable() = false
    override fun isPickable() = true

    fun bubbleBurstSource(): DamageSource = DamageSource(
        level().registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(ModDamagesTypes.BUBBLE_BURST),
        this
    )
}