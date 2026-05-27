package fr.heta__h.squ_abyssal_bloom.entity.custom.brine

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.client.brine.BrineAnimation
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BubbleColumnBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class BrineEntity(type: EntityType<out Monster>, level: Level) : Monster(type, level) {

    val idleAnimationState = AnimationState()
    val startAttackAnimationState = AnimationState()
    val stopAttackAnimationState = AnimationState()
    val loopAttackAnimationState = AnimationState()

    private var ticksOutOfWater = 0
    private val activeColumnPositions = mutableSetOf<BlockPos>()
    private val nextColumnPositions = mutableSetOf<BlockPos>()

    
    var knockbackTicks = 0
    var climbingTicks = 0

    
    private var clientAnimPhase = CANIM_IDLE
    private var clientAnimTicks = 0


    companion object {

        
        const val ANIMATION_FPS = 20
        const val CANIM_IDLE = 0
        const val CANIM_STARTING = 1
        const val CANIM_LOOPING = 2
        const val CANIM_STOPPING = 3

        
        const val TICKS_BEFORE_DEATH = 3
        const val INSTANT_KILL_DAMAGE = 10000f

        
        const val MAX_HEALTH = 15.0
        const val ATTACK_DAMAGE = 4.0
        const val MOVEMENT_SPEED = 0.25
        const val FOLLOW_RANGE = 24.0

        
        const val FLOAT_SPEED = 0.25
        const val FLOAT_LERP_FACTOR = 0.12
        const val FLOAT_Y_SCALE = 0.6
        const val FLOAT_MOVING_THRESHOLD_SQR = 0.002

        
        const val STILL_MIN_TICKS = 40
        const val STILL_MAX_TICKS = 80
        const val MOVE_MIN_TICKS = 50
        const val MOVE_MAX_TICKS = 100
        const val STILL_FRICTION = 0.88
        const val STILL_ROTATION_SPEED = 1.8f

        
        const val HOVER_FLOOR_SCAN_DEPTH = 8
        const val HOVER_TARGET_HEIGHT = 0.5
        const val HOVER_SPRING_K = 0.05
        const val HOVER_DAMPING = 0.45
        const val HOVER_FORCE_CLAMP = 0.15

        
        const val ROTATION_MOVING_THRESHOLD = 0.01

        
        const val SHADOW_SPEED = 0.20
        const val ATTACK_MOVE_SPEED = SHADOW_SPEED * 0.5
        const val SHADOW_STOP_DIST = 0.15

        
        const val ATTACK_ENTER_RADIUS = 2.0
        const val ATTACK_ENTER_RADIUS_SQR = ATTACK_ENTER_RADIUS * ATTACK_ENTER_RADIUS 
        const val ATTACK_EXIT_RADIUS = 5.0
        const val ATTACK_EXIT_RADIUS_SQR = ATTACK_EXIT_RADIUS * ATTACK_EXIT_RADIUS 
        const val DIRECT_EXIT_GRACE_TICKS = 15
        const val DIRECT_Y_GRACE_TICKS = 15

        
        const val COLUMN_VS_DIRECT_Y_THRESHOLD = 2.0

        val CROSS_OFFSETS = listOf(0 to 0, 1 to 0, -1 to 0, 0 to 1, 0 to -1)
        const val BUBBLE_COLUMN_HEIGHT = 10

        const val COLUMN_BUBBLE_COOLDOWN_TICKS = 20
        const val DIRECT_BUBBLE_COOLDOWN_TICKS = 30
        const val BUBBLE_COLUMN_SPEED = 0.45
        const val DIRECT_SHOT_SPEED = 0.55
        const val DIRECT_SHOT_FRONT_OFFSET = 0.6

        const val PARTICLE_BUBBLE_COUNT = 3
        const val PARTICLE_BUBBLE_SPREAD = 0.5
        const val PARTICLE_BUBBLE_SPEED_Y = 0.08

        
        private val IS_MOVING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BrineEntity::class.java, EntityDataSerializers.BOOLEAN)
        val COLUMN_ACTIVE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BrineEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder =
            createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(IS_MOVING, false)
        builder.define(COLUMN_ACTIVE, false)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, BrineHoverGoal())
        goalSelector.addGoal(1, BrineColumnAttackGoal())
        goalSelector.addGoal(1, BrineDirectAttackGoal())
        goalSelector.addGoal(2, BrineFollowGoal())
        goalSelector.addGoal(3, BrineIdleGoal())

        targetSelector.addGoal(0, HurtByTargetGoal(this))
        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun hurtServer(level: ServerLevel, source: DamageSource, amount: Float): Boolean {
        val projectile = source.directEntity
        if (projectile is BubbleProjectile && projectile.owner == this) return false
        return super.hurtServer(level, source, amount)
    }

    override fun knockback(strength: Double, x: Double, z: Double) {
        super.knockback(strength, x, z)
        knockbackTicks = 10
    }

    override fun die(damageSource: DamageSource) {
        if (!level().isClientSide && !isInWater) {
            val serverLevel = level() as? ServerLevel
            if (serverLevel != null) {
                val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), serverLevel)
                bubble.bubbleStage = 2
                bubble.setPos(x, y + bbHeight * 0.5, z)
                serverLevel.addFreshEntity(bubble)
                bubble.burstNoSplit()
            }
        }
        super.die(damageSource)
        if (!isInWater) remove(RemovalReason.KILLED)
    }


    fun brakeHorizontal() {
        if (knockbackTicks > 0) return
        deltaMovement = Vec3(
            deltaMovement.x * STILL_FRICTION, deltaMovement.y, deltaMovement.z * STILL_FRICTION
        )
    }

    fun moveToPlayerBlock(player: LivingEntity, xzDist: Double, speed: Double) {
        if (knockbackTicks > 0) return
        if (xzDist <= SHADOW_STOP_DIST) {
            brakeHorizontal()
            return
        }
        val targetX = floor(player.x) + 0.5
        val targetZ = floor(player.z) + 0.5
        val dx = targetX - x
        val dz = targetZ - z
        val dirX = dx / xzDist
        val dirZ = dz / xzDist

        val footY = floor(y).toInt()
        val mutablePos = BlockPos.MutableBlockPos()

        fun blockedAt(dist: Double): Boolean {
            val ax = floor(x + dirX * dist).toInt()
            val az = floor(z + dirZ * dist).toInt()
            val lv = level()

            fun hasCollision(yOff: Int): Boolean {
                mutablePos.set(ax, footY + yOff, az)
                return !lv.getBlockState(mutablePos).getCollisionShape(lv, mutablePos).isEmpty
            }

            return (hasCollision(0) || hasCollision(1)) && !hasCollision(2)
        }

        if (blockedAt(0.7) || blockedAt(1.2) || blockedAt(1.6)) {
            climbingTicks = 6
        } else if (climbingTicks > 0) {
            climbingTicks--
        }

        val yVel = if (climbingTicks > 0) 0.15 else deltaMovement.y

        deltaMovement = Vec3(dirX * speed, yVel, dirZ * speed)
        yRot = (atan2(dz, dx) * (180.0 / Math.PI)).toFloat() - 90f
        yBodyRot = yRot
        yHeadRot = yRot
    }


    private fun fireColumnBubble() {
        val serverLevel = level() as? ServerLevel ?: return
        val stage = if (random.nextBoolean()) 0 else 1
        val (ox, oz) = CROSS_OFFSETS[random.nextInt(CROSS_OFFSETS.size)]
        val startY = ceil(boundingBox.maxY).toInt()
        val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), serverLevel)
        bubble.bubbleStage = stage
        bubble.owner = this
        bubble.setPos(blockPosition().x + ox + 0.5, startY + 0.5, blockPosition().z + oz + 0.5)
        bubble.deltaMovement = Vec3(0.0, BUBBLE_COLUMN_SPEED, 0.0)
        val info = BubbleProjectile.STAGES[stage]
        serverLevel.addFreshEntity(bubble)
        playSound(ModSounds.BUBBLE_PROJECTILE_LAUNCH.get(), info.burstVolume, info.burstPitch)
    }

    private fun fireDirectBubble(player: LivingEntity) {
        val serverLevel = level() as? ServerLevel ?: return
        val stage = if (random.nextBoolean()) 0 else 1
        val yawRad = Math.toRadians(yRot.toDouble())
        val frontX = x - sin(yawRad) * DIRECT_SHOT_FRONT_OFFSET
        val frontY = y + bbHeight * 0.5
        val frontZ = z + cos(yawRad) * DIRECT_SHOT_FRONT_OFFSET
        val dx = player.x - frontX
        val dy = (player.y + player.bbHeight * 0.5) - frontY
        val dz = player.z - frontZ
        val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01)
        val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), serverLevel)
        bubble.bubbleStage = stage
        bubble.owner = this
        bubble.setPos(frontX, frontY, frontZ)
        bubble.deltaMovement = Vec3(dx / dist, dy / dist, dz / dist).scale(DIRECT_SHOT_SPEED)
        serverLevel.addFreshEntity(bubble)
        val info = BubbleProjectile.STAGES[stage]
        playSound(ModSounds.BUBBLE_PROJECTILE_LAUNCH.get(), info.burstVolume, info.burstPitch)
    }

    fun placeOrUpdateBubbleColumn() {
        val serverLevel = level() as? ServerLevel ?: return
        val bx = blockPosition().x
        val bz = blockPosition().z
        val startY = ceil(boundingBox.maxY).toInt()
        val colState = Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false)

        nextColumnPositions.clear()
        val mutablePos = BlockPos.MutableBlockPos()

        for ((dx, dz) in CROSS_OFFSETS) {
            for (i in 0 until BUBBLE_COLUMN_HEIGHT) {
                mutablePos.set(bx + dx, startY + i, bz + dz)
                val current = serverLevel.getBlockState(mutablePos)
                when {
                    current.`is`(Blocks.BUBBLE_COLUMN) -> {
                        nextColumnPositions.add(mutablePos.immutable())
                    }
                    current.`is`(Blocks.WATER) -> {
                        val immutable = mutablePos.immutable()
                        
                        serverLevel.setBlock(immutable, colState, 3)
                        nextColumnPositions.add(immutable)
                    }
                    else -> break
                }
            }
        }

        val iterator = activeColumnPositions.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()
            if (!nextColumnPositions.contains(pos)) {
                restoreToWater(pos, serverLevel)
                iterator.remove()
            }
        }

        activeColumnPositions.addAll(nextColumnPositions)

        if (!entityData.get(COLUMN_ACTIVE)) entityData.set(COLUMN_ACTIVE, true)
    }

    fun clearBubbleColumn() {
        if (entityData.get(COLUMN_ACTIVE)) entityData.set(COLUMN_ACTIVE, false)
        val serverLevel = level() as? ServerLevel ?: return
        for (pos in activeColumnPositions) restoreToWater(pos, serverLevel)
        activeColumnPositions.clear()
    }

    private fun restoreToWater(pos: BlockPos, serverLevel: ServerLevel) {
        val current = serverLevel.getBlockState(pos)
        if (!current.`is`(Blocks.BUBBLE_COLUMN)) return
        val waterState = Blocks.WATER.defaultBlockState()
        serverLevel.setBlock(pos, waterState, 3)
    }

    override fun remove(reason: RemovalReason) {
        if (!level().isClientSide) clearBubbleColumn()
        super.remove(reason)
    }

    private fun transition(newPhase: Int) {
        clientAnimPhase = newPhase
        clientAnimTicks = 0
        idleAnimationState.stop()
        startAttackAnimationState.stop()
        loopAttackAnimationState.stop()
        stopAttackAnimationState.stop()
    }


    override fun playHurtSound(source: DamageSource) {
        if (!isInWater) return
        super.playHurtSound(source)
    }

    override fun getAmbientSound(): SoundEvent? {
        return ModSounds.BRINE_AMBIENT.get()
    }

    override fun getDeathSound(): SoundEvent {
        return  ModSounds.BRINE_DEATH.get()
    }

    override fun getHurtSound(source: DamageSource): SoundEvent {
        return ModSounds.BRINE_HURT.get()
    }


    override fun tick() {
        super.tick()
        if (!level().isClientSide) return

        if (!isInWater) {
            transition(CANIM_IDLE)
            return
        }

        repeat(PARTICLE_BUBBLE_COUNT) {
            level().addParticle(
                ParticleTypes.BUBBLE,
                getRandomX(PARTICLE_BUBBLE_SPREAD), randomY, getRandomZ(PARTICLE_BUBBLE_SPREAD),
                (random.nextDouble() - 0.5) * 0.02,
                PARTICLE_BUBBLE_SPEED_Y + random.nextDouble() * 0.04,
                (random.nextDouble() - 0.5) * 0.02
            )
        }

        val columnActive = entityData.get(COLUMN_ACTIVE)
        clientAnimTicks++

        when (clientAnimPhase) {
            CANIM_IDLE -> {
                if (columnActive) transition(CANIM_STARTING)
            }
            CANIM_STARTING -> when {
                !columnActive ->
                    transition(CANIM_STOPPING)
                clientAnimTicks >= BrineAnimation.start_attack.lengthInSeconds * ANIMATION_FPS ->
                    transition(CANIM_LOOPING)
            }
            CANIM_LOOPING -> {
                if (!columnActive) transition(CANIM_STOPPING)
            }
            CANIM_STOPPING -> when {
                columnActive ->
                    transition(CANIM_STARTING)
                clientAnimTicks >= BrineAnimation.stop_attack.lengthInSeconds * ANIMATION_FPS ->
                    transition(CANIM_IDLE)
            }
        }

        when (clientAnimPhase) {
            CANIM_IDLE -> idleAnimationState.startIfStopped(tickCount)
            CANIM_STARTING -> startAttackAnimationState.startIfStopped(tickCount)
            CANIM_LOOPING -> loopAttackAnimationState.startIfStopped(tickCount)
            CANIM_STOPPING -> stopAttackAnimationState.startIfStopped(tickCount)
        }
    }

    override fun aiStep() {
        super.aiStep()
        if (level().isClientSide) return
        if (!isInWater) {
            if (++ticksOutOfWater >= TICKS_BEFORE_DEATH)
                hurtServer(level() as ServerLevel, damageSources().drown(), INSTANT_KILL_DAMAGE)
        } else {
            ticksOutOfWater = 0
            airSupply = maxAirSupply
            if (knockbackTicks > 0) knockbackTicks--
        }
        val moving = deltaMovement.lengthSqr() > FLOAT_MOVING_THRESHOLD_SQR
        if (entityData.get(IS_MOVING) != moving) entityData.set(IS_MOVING, moving)
    }

    fun distanceToSolidFloor(): Double {
        val origin = blockPosition()
        val lv = level()
        val mutablePos = BlockPos.MutableBlockPos()
        for (i in 0..HOVER_FLOOR_SCAN_DEPTH) {
            mutablePos.set(origin.x, origin.y - i, origin.z)
            if (!lv.getBlockState(mutablePos).getCollisionShape(lv, mutablePos).isEmpty)
                return y - (mutablePos.y + 1.0)
        }
        return (HOVER_FLOOR_SCAN_DEPTH + 1).toDouble()
    }

    fun computeHoverSpringForce(): Double {
        val error = HOVER_TARGET_HEIGHT - distanceToSolidFloor()
        return (error * HOVER_SPRING_K - deltaMovement.y * HOVER_DAMPING)
            .coerceIn(-HOVER_FORCE_CLAMP, HOVER_FORCE_CLAMP)
    }

    fun xzDistTo(entity: LivingEntity): Double {
        val dx = entity.x - x
        val dz = entity.z - z
        return sqrt(dx * dx + dz * dz)
    }

    fun xzDistToSqr(entity: LivingEntity): Double {
        val dx = entity.x - x
        val dz = entity.z - z
        return dx * dx + dz * dz
    }

    inner class BrineHoverGoal : Goal() {
        init { flags = EnumSet.noneOf(Flag::class.java) }

        override fun canUse() = isInWater
        override fun requiresUpdateEveryTick() = true

        override fun tick() {
            deltaMovement = Vec3(
                deltaMovement.x,
                deltaMovement.y + computeHoverSpringForce(),
                deltaMovement.z
            )
        }
    }


    inner class BrineIdleGoal : Goal() {
        private var isStillPhase = true
        private var phaseTicks = 0
        private var phaseDuration = 0
        private var targetVelocity = Vec3.ZERO

        init { flags = EnumSet.of(Flag.MOVE) }

        override fun canUse() = isInWater && target == null
        override fun requiresUpdateEveryTick() = true

        override fun start() { enterStillPhase() }

        override fun tick() {
            if (isStillPhase) tickStill() else tickMove()
        }

        private fun enterStillPhase() {
            isStillPhase = true; phaseTicks = 0
            phaseDuration = STILL_MIN_TICKS + random.nextInt(STILL_MAX_TICKS - STILL_MIN_TICKS)
        }

        private fun enterMovePhase() {
            isStillPhase = false; phaseTicks = 0
            phaseDuration = MOVE_MIN_TICKS + random.nextInt(MOVE_MAX_TICKS - MOVE_MIN_TICKS)
            targetVelocity = Vec3(
                (random.nextDouble() - 0.5) * 2.0,
                (random.nextDouble() - 0.5) * FLOAT_Y_SCALE,
                (random.nextDouble() - 0.5) * 2.0
            ).normalize().scale(FLOAT_SPEED)
        }

        private fun tickStill() {
            brakeHorizontal()
            yRot += STILL_ROTATION_SPEED; yBodyRot = yRot; yHeadRot = yRot
            if (++phaseTicks >= phaseDuration) enterMovePhase()
        }

        private fun tickMove() {
            if (++phaseTicks >= phaseDuration) { enterStillPhase(); return }
            deltaMovement = Vec3(
                deltaMovement.x + (targetVelocity.x - deltaMovement.x) * FLOAT_LERP_FACTOR,
                deltaMovement.y,
                deltaMovement.z + (targetVelocity.z - deltaMovement.z) * FLOAT_LERP_FACTOR
            )
            val h = sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z)
            if (h > ROTATION_MOVING_THRESHOLD) {
                yRot = (atan2(deltaMovement.z, deltaMovement.x) * (180.0 / Math.PI)).toFloat() - 90f
                yBodyRot = yRot; yHeadRot = yRot
                xRot = (atan2(deltaMovement.y, h) * -(180.0 / Math.PI)).toFloat()
            }
        }

        override fun stop() {
            deltaMovement = Vec3.ZERO
        }
    }


    inner class BrineFollowGoal : Goal() {
        init { flags = EnumSet.of(Flag.MOVE) }

        override fun canUse(): Boolean {
            val p = target ?: return false
            return isInWater && xzDistToSqr(p) > ATTACK_ENTER_RADIUS_SQR
        }

        override fun canContinueToUse(): Boolean {
            val p = target ?: return false
            return isInWater && xzDistToSqr(p) > ATTACK_ENTER_RADIUS_SQR
        }

        override fun requiresUpdateEveryTick() = true

        override fun tick() {
            val p = target ?: return
            moveToPlayerBlock(p, xzDistTo(p), SHADOW_SPEED)
        }
    }


    inner class BrineColumnAttackGoal : Goal() {
        private var bubbleCooldown = 0

        init { flags = EnumSet.of(Flag.MOVE) }

        override fun canUse(): Boolean {
            val p = target ?: return false
            if (!isInWater) return false
            return xzDistToSqr(p) <= ATTACK_ENTER_RADIUS_SQR && (p.y - y) > COLUMN_VS_DIRECT_Y_THRESHOLD
        }

        override fun canContinueToUse(): Boolean {
            val p = target ?: return false
            if (!isInWater) return false
            return xzDistToSqr(p) <= ATTACK_EXIT_RADIUS_SQR && (p.y - y) > COLUMN_VS_DIRECT_Y_THRESHOLD
        }

        override fun requiresUpdateEveryTick() = true

        override fun start() {
            bubbleCooldown = COLUMN_BUBBLE_COOLDOWN_TICKS
        }

        override fun tick() {
            if (bubbleCooldown > 0) bubbleCooldown--
            val p = target ?: return
            moveToPlayerBlock(p, xzDistTo(p), ATTACK_MOVE_SPEED)
            placeOrUpdateBubbleColumn()
            if (bubbleCooldown <= 0) {
                bubbleCooldown = COLUMN_BUBBLE_COOLDOWN_TICKS
                fireColumnBubble()
            }
        }

        override fun stop() {
            clearBubbleColumn()
        }
    }


    inner class BrineDirectAttackGoal : Goal() {
        private var bubbleCooldown = 0
        var directExitTimer = 0
        var directYTimer = 0

        init { flags = EnumSet.of(Flag.MOVE) }

        override fun canUse(): Boolean {
            val p = target ?: return false
            if (!isInWater) return false
            return xzDistToSqr(p) <= ATTACK_ENTER_RADIUS_SQR && (p.y - y) <= COLUMN_VS_DIRECT_Y_THRESHOLD
        }

        override fun canContinueToUse(): Boolean {
            val p = target ?: return false
            if (!isInWater) return false
            if (xzDistToSqr(p) > ATTACK_EXIT_RADIUS_SQR && directExitTimer >= DIRECT_EXIT_GRACE_TICKS) return false
            if ((p.y - y) > COLUMN_VS_DIRECT_Y_THRESHOLD && directYTimer >= DIRECT_Y_GRACE_TICKS) return false
            return true
        }

        override fun requiresUpdateEveryTick() = true

        override fun start() {
            bubbleCooldown = DIRECT_BUBBLE_COOLDOWN_TICKS
            directExitTimer = 0
            directYTimer = 0
        }

        override fun tick() {
            if (bubbleCooldown > 0) bubbleCooldown--
            val p = target ?: return
            val xzDist = xzDistTo(p)
            val aboveY = p.y - y

            if (xzDist > ATTACK_EXIT_RADIUS) { directExitTimer++
                directYTimer = 0
                return
            }
            directExitTimer = 0

            if (aboveY > COLUMN_VS_DIRECT_Y_THRESHOLD) {
                directYTimer++
                return
            }

            directYTimer = 0

            moveToPlayerBlock(p, xzDist, ATTACK_MOVE_SPEED)
            if (bubbleCooldown <= 0) {
                bubbleCooldown = DIRECT_BUBBLE_COOLDOWN_TICKS
                fireDirectBubble(p)
            }
        }

        override fun stop() {
            directExitTimer = 0
            directYTimer = 0
        }
    }
}