package fr.heta__h.squ_abyssal_bloom.entity.custom.brine

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BubbleColumnBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class BrineEntity(type: EntityType<out Monster>, level: Level) : Monster(type, level) {

    val idleAnimationState        = AnimationState()
    val startAttackAnimationState = AnimationState()
    val stopAttackAnimationState  = AnimationState()
    val loopAttackAnimationState  = AnimationState()

    private var ticksOutOfWater       = 0
    private val activeColumnPositions = mutableSetOf<BlockPos>()

    companion object {
        
        const val PHASE_IDLE = 0
        const val PHASE_RUSH = 1
        const val PHASE_START_ATTACK = 2
        const val PHASE_LOOP_ATTACK = 3
        const val PHASE_STOP_ATTACK = 4
        const val PHASE_DIRECT_ATTACK = 5

        
        const val TICKS_BEFORE_DEATH  = 3
        const val INSTANT_KILL_DAMAGE = 10000f

        
        const val MAX_HEALTH     = 15.0
        const val ATTACK_DAMAGE  = 4.0
        const val MOVEMENT_SPEED = 0.25
        const val FOLLOW_RANGE   = 16.0

        
        const val FLOAT_SPEED                = 0.25
        const val FLOAT_LERP_FACTOR          = 0.12
        const val FLOAT_Y_SCALE              = 0.6
        const val FLOAT_MOVING_THRESHOLD_SQR = 0.002

        
        const val STILL_MIN_TICKS      = 40
        const val STILL_MAX_TICKS      = 80
        const val MOVE_MIN_TICKS       = 50
        const val MOVE_MAX_TICKS       = 100
        const val STILL_FRICTION       = 0.88
        const val STILL_ROTATION_SPEED = 1.8f

        
        const val HOVER_FLOOR_SCAN_DEPTH = 8
        const val HOVER_TARGET_HEIGHT    = 0.5
        const val HOVER_SPRING_K         = 0.05
        const val HOVER_DAMPING          = 0.45
        const val HOVER_FORCE_CLAMP      = 0.15

        
        const val ROTATION_MOVING_THRESHOLD = 0.01

        
        const val SHADOW_SPEED      = 0.20
        const val ATTACK_MOVE_SPEED = SHADOW_SPEED * 0.5
        const val SHADOW_STOP_DIST  = 0.15

        
        const val ATTACK_ENTER_RADIUS = 2.0
        const val ATTACK_EXIT_RADIUS = 5.0
        const val START_ATTACK_DURATION_TICKS = 40
        const val STOP_ATTACK_DURATION_TICKS = 40

        const val COLUMN_VS_DIRECT_Y_THRESHOLD = 2.0

        val CROSS_OFFSETS = listOf(0 to 0, 1 to 0, -1 to 0, 0 to 1, 0 to -1)
        const val BUBBLE_COLUMN_HEIGHT = 10

        const val COLUMN_BUBBLE_COOLDOWN_TICKS = 20
        const val DIRECT_BUBBLE_COOLDOWN_TICKS = 30
        const val BUBBLE_COLUMN_SPEED = 0.45
        const val DIRECT_SHOT_SPEED = 0.55
        const val DIRECT_SHOT_FRONT_OFFSET = 0.6

        const val PARTICLE_BUBBLE_COUNT   = 3
        const val PARTICLE_BUBBLE_SPREAD  = 0.5
        const val PARTICLE_BUBBLE_SPEED_Y = 0.08

        private val IS_MOVING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BrineEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val ATTACK_PHASE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BrineEntity::class.java, EntityDataSerializers.INT)

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
        builder.define(ATTACK_PHASE, PHASE_IDLE)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, BrineFloatGoal())
        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun hurtServer(level: ServerLevel, source: DamageSource, amount: Float): Boolean {
        val projectile = source.directEntity as? BubbleProjectile

        if (projectile != null && projectile.owner != this) {
            return false
        }

        return super.hurtServer(level, source, amount)
    }

    private fun fireColumnBubble() {
        val bubbleType = ModEntities.BUBBLE.get()
        val serverLevel = level() as? ServerLevel ?: return
        val stage = if (random.nextBoolean()) 0 else 1
        val (ox, oz) = CROSS_OFFSETS[random.nextInt(CROSS_OFFSETS.size)]
        val startY = ceil(boundingBox.maxY).toInt()
        val bubble = BubbleProjectile(bubbleType, serverLevel)
        bubble.bubbleStage = stage
        bubble.owner = this
        bubble.setPos(blockPosition().x + ox + 0.5, startY + 0.5, blockPosition().z + oz + 0.5)
        bubble.deltaMovement = Vec3(0.0, BUBBLE_COLUMN_SPEED, 0.0)
        val stageInfo = BubbleProjectile.STAGES[stage]
        serverLevel.addFreshEntity(bubble)
        playSound(ModSounds.BUBBLE_PROJECTILE_LAUNCH.get(), stageInfo.burstVolume, stageInfo.burstPitch)
    }


    private fun fireDirectBubble(player: Player) {
        val bubbleType = ModEntities.BUBBLE.get()
        val serverLevel = level() as? ServerLevel ?: return
        val stage = if (random.nextBoolean()) 0 else 1

        val yawRad = Math.toRadians(yRot.toDouble())
        val frontX = x - sin(yawRad) * DIRECT_SHOT_FRONT_OFFSET
        val frontY = y + bbHeight * 0.5
        val frontZ = z + Math.cos(Math.toRadians(yRot.toDouble())) * DIRECT_SHOT_FRONT_OFFSET

        val dx = player.x - frontX
        val dy = (player.y + player.bbHeight * 0.5) - frontY
        val dz = player.z - frontZ
        val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01)

        val bubble = BubbleProjectile(bubbleType, serverLevel)
        bubble.bubbleStage = stage
        bubble.owner = this
        bubble.setPos(frontX, frontY, frontZ)
        bubble.deltaMovement = Vec3(dx / dist, dy / dist, dz / dist).scale(DIRECT_SHOT_SPEED)
        serverLevel.addFreshEntity(bubble)
        val stageInfo2 = BubbleProjectile.STAGES[stage]
        playSound(ModSounds.BUBBLE_PROJECTILE_LAUNCH.get(), stageInfo2.burstVolume, stageInfo2.burstPitch)
    }

    fun placeOrUpdateBubbleColumn() {
        val serverLevel = level() as? ServerLevel ?: return
        val bx = blockPosition().x
        val bz = blockPosition().z
        val startY = ceil(boundingBox.maxY).toInt()
        val colState = Blocks.BUBBLE_COLUMN.defaultBlockState()
            .setValue(BubbleColumnBlock.DRAG_DOWN, false)

        val newPositions = mutableSetOf<BlockPos>()

        for ((dx, dz) in CROSS_OFFSETS) {
            for (i in 0 until BUBBLE_COLUMN_HEIGHT) {
                val pos = BlockPos(bx + dx, startY + i, bz + dz)
                val current = serverLevel.getBlockState(pos)
                when {
                    current.`is`(Blocks.BUBBLE_COLUMN) -> newPositions.add(pos)
                    current.`is`(Blocks.WATER) -> {
                        val chunk = serverLevel.getChunk(pos.x shr 4, pos.z shr 4) as? LevelChunk ?: continue
                        chunk.setBlockState(pos, colState, 0)
                        serverLevel.sendBlockUpdated(pos, current, colState, 2)
                        newPositions.add(pos)
                    }
                    else -> break
                }
            }
        }

        for (pos in activeColumnPositions - newPositions) restoreToWater(pos, serverLevel)
        activeColumnPositions.clear()
        activeColumnPositions.addAll(newPositions)
    }

    fun clearBubbleColumn() {
        val serverLevel = level() as? ServerLevel ?: return
        for (pos in activeColumnPositions) restoreToWater(pos, serverLevel)
        activeColumnPositions.clear()
    }

    private fun restoreToWater(pos: BlockPos, serverLevel: ServerLevel) {
        val current = serverLevel.getBlockState(pos)
        if (!current.`is`(Blocks.BUBBLE_COLUMN)) return
        val waterState = Blocks.WATER.defaultBlockState()
        val chunk = serverLevel.getChunk(pos.x shr 4, pos.z shr 4) as? LevelChunk ?: return
        chunk.setBlockState(pos, waterState, 0)
        serverLevel.sendBlockUpdated(pos, current, waterState, 2)
    }

    override fun remove(reason: RemovalReason) {
        if (!level().isClientSide) clearBubbleColumn()
        super.remove(reason)
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide) return
        if (!isInWater) { idleAnimationState.stop(); return }

        val phase = entityData.get(ATTACK_PHASE)

        repeat(PARTICLE_BUBBLE_COUNT) {
            level().addParticle(
                ParticleTypes.BUBBLE,
                getRandomX(PARTICLE_BUBBLE_SPREAD), randomY, getRandomZ(PARTICLE_BUBBLE_SPREAD),
                (random.nextDouble() - 0.5) * 0.02,
                PARTICLE_BUBBLE_SPEED_Y + random.nextDouble() * 0.04,
                (random.nextDouble() - 0.5) * 0.02
            )
        }

        when (phase) {
            PHASE_IDLE, PHASE_RUSH -> {
                startAttackAnimationState.stop()
                loopAttackAnimationState.stop()
                stopAttackAnimationState.stop()
                idleAnimationState.startIfStopped(tickCount)
            }
            PHASE_START_ATTACK -> {
                idleAnimationState.stop()
                loopAttackAnimationState.stop()
                stopAttackAnimationState.stop()
                startAttackAnimationState.startIfStopped(tickCount)
            }
            PHASE_LOOP_ATTACK -> {
                idleAnimationState.stop()
                startAttackAnimationState.stop()
                stopAttackAnimationState.stop()
                loopAttackAnimationState.startIfStopped(tickCount)
            }
            PHASE_STOP_ATTACK -> {
                idleAnimationState.stop()
                startAttackAnimationState.stop()
                loopAttackAnimationState.stop()
                stopAttackAnimationState.startIfStopped(tickCount)
            }
            PHASE_DIRECT_ATTACK -> {
                startAttackAnimationState.stop()
                loopAttackAnimationState.stop()
                stopAttackAnimationState.stop()
                idleAnimationState.startIfStopped(tickCount)
            }
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
        }

        val moving = deltaMovement.lengthSqr() > FLOAT_MOVING_THRESHOLD_SQR
        if (entityData.get(IS_MOVING) != moving) entityData.set(IS_MOVING, moving)
    }

    private fun distanceToSolidFloor(): Double {
        val origin = blockPosition()
        for (i in 0..HOVER_FLOOR_SCAN_DEPTH) {
            val pos = BlockPos(origin.x, origin.y - i, origin.z)
            if (level().getBlockState(pos).isSolid) return y - (pos.y + 1.0)
        }
        return (HOVER_FLOOR_SCAN_DEPTH + 1).toDouble()
    }

    private fun computeHoverSpringForce(): Double {
        val error = HOVER_TARGET_HEIGHT - distanceToSolidFloor()
        return (error * HOVER_SPRING_K - deltaMovement.y * HOVER_DAMPING)
            .coerceIn(-HOVER_FORCE_CLAMP, HOVER_FORCE_CLAMP)
    }

    private fun xzDistTo(player: Player): Double {
        val dx = player.x - x
        val dz = player.z - z
        return sqrt(dx * dx + dz * dz)
    }

    inner class BrineFloatGoal : Goal() {

        private var isStillPhase = true
        private var phaseTicks = 0
        private var phaseDuration = 0
        private var targetVelocity = Vec3.ZERO

        private var attackPhase = PHASE_IDLE
        private var attackPhaseTicks = 0
        private var bubbleAttackCooldown = 0

        init {
            flags = EnumSet.of(Flag.MOVE)
        }

        override fun canUse(): Boolean = isInWater
        override fun requiresUpdateEveryTick() = true

        override fun start() {
            enterStillPhase()
            setPhase(PHASE_IDLE)
        }

        override fun tick() {
            deltaMovement = Vec3(
                deltaMovement.x,
                deltaMovement.y + computeHoverSpringForce(),
                deltaMovement.z
            )
            if (bubbleAttackCooldown > 0) bubbleAttackCooldown--

            val player = target as? Player
            if (player == null) {
                when (attackPhase) {
                    PHASE_START_ATTACK,
                    PHASE_LOOP_ATTACK,
                    PHASE_DIRECT_ATTACK -> { clearBubbleColumn(); enterStopAttack() }
                    PHASE_STOP_ATTACK   -> tickStopAttack()
                    else -> {
                        setPhase(PHASE_IDLE)
                        if (isStillPhase) tickStill() else tickMove()
                    }
                }
            } else {
                tickShadowPlayer(player)
            }
        }

        private fun tickShadowPlayer(player: Player) {
            val xzDist = xzDistTo(player)
            val playerAboveY = player.y - y

            when (attackPhase) {

                PHASE_IDLE, PHASE_RUSH -> {
                    if (xzDist <= ATTACK_ENTER_RADIUS) enterStartAttack()
                    else { setPhase(PHASE_RUSH); moveToPlayerBlock(player, xzDist, SHADOW_SPEED) }
                }

                PHASE_START_ATTACK -> {
                    if (xzDist > ATTACK_EXIT_RADIUS) {
                        clearBubbleColumn()
                        enterStopAttack()
                        return
                    }
                    if (playerAboveY <= COLUMN_VS_DIRECT_Y_THRESHOLD) {
                        clearBubbleColumn()
                        enterDirectAttack()
                        return
                    }
                    moveToPlayerBlock(player, xzDist, ATTACK_MOVE_SPEED)
                    placeOrUpdateBubbleColumn()
                    fireBubbleIfReady(COLUMN_BUBBLE_COOLDOWN_TICKS) {
                        fireColumnBubble()
                    }
                    if (++attackPhaseTicks >= START_ATTACK_DURATION_TICKS) enterLoopAttack()
                }

                PHASE_LOOP_ATTACK -> {
                    if (xzDist > ATTACK_EXIT_RADIUS) {
                        clearBubbleColumn()
                        enterStopAttack()
                        return
                    }
                    if (playerAboveY <= COLUMN_VS_DIRECT_Y_THRESHOLD) {
                        clearBubbleColumn()
                        enterDirectAttack()
                        return
                    }
                    moveToPlayerBlock(player, xzDist, ATTACK_MOVE_SPEED)
                    placeOrUpdateBubbleColumn()
                    fireBubbleIfReady(COLUMN_BUBBLE_COOLDOWN_TICKS) {
                        fireColumnBubble()
                    }
                }

                PHASE_DIRECT_ATTACK -> {
                    if (xzDist > ATTACK_EXIT_RADIUS) {
                        enterStopAttack()
                        return
                    }
                    if (playerAboveY > COLUMN_VS_DIRECT_Y_THRESHOLD) {
                        enterStartAttack()
                        return
                    }

                    moveToPlayerBlock(player, xzDist, ATTACK_MOVE_SPEED)
                    fireBubbleIfReady(DIRECT_BUBBLE_COOLDOWN_TICKS) {
                        fireDirectBubble(player)
                    }

                    val animPhase = if (attackPhaseTicks < STOP_ATTACK_DURATION_TICKS) PHASE_STOP_ATTACK else PHASE_DIRECT_ATTACK
                    if (entityData.get(ATTACK_PHASE) != animPhase)
                        entityData.set(ATTACK_PHASE, animPhase)
                    attackPhaseTicks++
                }

                PHASE_STOP_ATTACK -> {
                    brakeHorizontal()
                    tickStopAttack()
                }
            }
        }

        private inline fun fireBubbleIfReady(cooldownTicks: Int, fire: () -> Unit) {
            if (bubbleAttackCooldown <= 0) {
                bubbleAttackCooldown = cooldownTicks
                fire()
            }
        }

        private fun moveToPlayerBlock(player: Player, xzDist: Double, speed: Double) {
            if (xzDist <= SHADOW_STOP_DIST) { brakeHorizontal(); return }
            val targetX = floor(player.x) + 0.5
            val targetZ = floor(player.z) + 0.5
            val dx = targetX - x
            val dz = targetZ - z
            deltaMovement = Vec3((dx / xzDist) * speed, deltaMovement.y, (dz / xzDist) * speed)
            yRot = (atan2(dz, dx) * (180.0 / Math.PI)).toFloat() - 90f
            yBodyRot = yRot
            yHeadRot = yRot
        }

        private fun brakeHorizontal() {
            deltaMovement = Vec3(
                deltaMovement.x * STILL_FRICTION, deltaMovement.y, deltaMovement.z * STILL_FRICTION
            )
        }

        private fun enterStartAttack() {
            attackPhaseTicks = 0
            bubbleAttackCooldown = COLUMN_BUBBLE_COOLDOWN_TICKS
            setPhase(PHASE_START_ATTACK)
        }

        private fun enterLoopAttack()  { attackPhaseTicks = 0; setPhase(PHASE_LOOP_ATTACK) }
        private fun enterStopAttack()  { attackPhaseTicks = 0; setPhase(PHASE_STOP_ATTACK) }

        private fun enterDirectAttack() {
            attackPhaseTicks = 0
            bubbleAttackCooldown = DIRECT_BUBBLE_COOLDOWN_TICKS
            attackPhase = PHASE_DIRECT_ATTACK
            entityData.set(ATTACK_PHASE, PHASE_STOP_ATTACK)
        }

        private fun tickStopAttack() {
            if (++attackPhaseTicks >= STOP_ATTACK_DURATION_TICKS) {
                setPhase(PHASE_IDLE)
                enterStillPhase()
            }
        }

        private fun setPhase(phase: Int) {
            attackPhase = phase
            entityData.set(ATTACK_PHASE, phase)
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
            clearBubbleColumn()
            setPhase(PHASE_IDLE)
            entityData.set(IS_MOVING, false)
        }
    }
}