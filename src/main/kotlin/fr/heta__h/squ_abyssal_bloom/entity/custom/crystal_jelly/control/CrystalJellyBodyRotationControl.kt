package fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import net.minecraft.util.Mth
import net.minecraft.util.Mth.approachDegrees
import net.minecraft.world.entity.ai.control.BodyRotationControl

class CrystalJellyBodyRotationControl(
    private val crystalJelly: CrystalJellyEntity
) : BodyRotationControl(crystalJelly) {

    override fun clientTick() {
        if (!crystalJelly.isInWater) {
            crystalJelly.yHeadRot = crystalJelly.yBodyRot
            return
        }

        crystalJelly.yBodyRot = approachDegrees(
            crystalJelly.yBodyRot,
            crystalJelly.yRot,
            CrystalJellyEntity.BODY_TURN_DEGREES_PER_TICK
        )
        crystalJelly.yHeadRot = crystalJelly.yBodyRot
    }
}
