package fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import net.minecraft.world.entity.ai.control.LookControl

class CrystalJellyLookControl(crystalJelly: CrystalJellyEntity) : LookControl(crystalJelly) {

    override fun resetXRotOnTick(): Boolean = false
}
