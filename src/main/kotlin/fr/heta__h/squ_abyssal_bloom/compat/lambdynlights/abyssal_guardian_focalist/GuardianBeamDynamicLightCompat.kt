package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.abyssal_guardian_focalist

import dev.lambdaurora.lambdynlights.api.behavior.LineLightBehavior
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.AbstractDynamicLightCompat
import net.minecraft.world.entity.player.Player
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap

object GuardianBeamDynamicLightCompat : AbstractDynamicLightCompat() {

    private val activeLightBeams = ConcurrentHashMap<Int, LineLightBehavior>()

    fun updateBeamLight(player: Player, targetX: Double, targetY: Double, targetZ: Double, chargeScale: Float) {
        if (!isInitialized) return

        val luminance = 7 + (chargeScale * 8).toInt()
        val playerPos = Vector3d(player.x, player.y + player.eyeHeight, player.z)
        val targetPos = Vector3d(targetX, targetY, targetZ)

        updateLineLight(activeLightBeams, player.id, playerPos, targetPos, luminance) { player.isRemoved }
    }

    fun removeBeamLight(playerId: Int) {
        if (!isInitialized) return

        removeLineLight(activeLightBeams, playerId)
    }
}