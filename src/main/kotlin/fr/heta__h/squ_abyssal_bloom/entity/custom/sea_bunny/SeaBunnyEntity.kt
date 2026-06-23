package fr.heta__h.squ_abyssal_bloom.entity.custom.sea_bunny

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level


class SeaBunnyEntity(type : EntityType<out SeaBunnyEntity>, level : Level) : TamableAnimal(type, level) {

    companion object {
        const val DROWN_DAMAGE : Float = 2.0f
    }

    override fun isFood(p0: ItemStack): Boolean {
        TODO("Don't care")
    }

    override fun getBreedOffspring(
        level: ServerLevel,
        partner: AgeableMob
    ): AgeableMob {
        TODO("Don't care")
    }

    fun handleAirSupply(level: ServerLevel, airSupply : Int) {
        if (!this.isAlive || hasEnoughPressure()) {
            this.airSupply = maxAirSupply
            return
        }

        this.airSupply--
        if (!this.shouldTakeDrowningDamage()) return

        this.airSupply = 0
        this.hurtServer( level, this.damageSources().drown(), DROWN_DAMAGE)
    }

    fun hasEnoughPressure() : Boolean {
        TODO("Not yet, " +
                "add isInWater check"
        )
    }
}