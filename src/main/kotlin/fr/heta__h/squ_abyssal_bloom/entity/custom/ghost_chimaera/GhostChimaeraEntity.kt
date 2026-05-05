package fr.heta__h.squ_abyssal_bloom.entity.custom.ghost_chimaera

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.AgeableWaterCreature
import net.minecraft.world.entity.monster.Monster.createMonsterAttributes
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3


class GhostChimaeraEntity(type: EntityType<out AgeableWaterCreature>, level: Level) : AgeableWaterCreature(type, level) {


    private var timeExposedInAir = 0
    private var animationStartTick = 0


    override fun getBreedOffspring(
        p0: ServerLevel,
        p1: AgeableMob
    ): AgeableMob? {
        return null
    }

    init {
        this.xpReward = 10
    }

    companion object {
        private val CURRENT_GOAL_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GhostChimaeraEntity::class.java, EntityDataSerializers.INT)

        private val BODY_ALPHA: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(GhostChimaeraEntity::class.java, EntityDataSerializers.FLOAT)

        fun createAttributes(): AttributeSupplier.Builder =
            createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .add(Attributes.FOLLOW_RANGE, 55.0)
    }

    
    var bodyAlpha: Float
        get() = entityData.get(BODY_ALPHA)
        set(value) = entityData.set(BODY_ALPHA, value.coerceIn(0.0f, 1.0f))

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CURRENT_GOAL_STATE, 0)
        builder.define(BODY_ALPHA, 1.0f)
    }

    override fun registerGoals() {
    }

    override fun tick() {
        super.tick()
    }

    private fun resetAnimationStates(keepClosing: Boolean = false, keepSwallowStop: Boolean = false) {
    }

    override fun aiStep() {
        super.aiStep()
        if (isUnderWater) {
            timeExposedInAir = 0
            airSupply = maxAirSupply
        } else {
            timeExposedInAir++
            if (timeExposedInAir >= 200) {
                hurtServer(level() as ServerLevel, damageSources().drown(), 2.0f)
            }
        }
    }

    override fun travel(travelVector: Vec3) {
        if (isEffectiveAi && isInWater) {
            moveRelative(0.01f, travelVector)
            move(MoverType.SELF, deltaMovement)
            deltaMovement = deltaMovement.scale(0.9)
        } else {
            super.travel(travelVector)
        }
    }
}