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

        val playerId = player.id
        val luminance = 7 + (chargeScale * 8).toInt()

        val playerPos = Vector3d(player.x, player.y + player.eyeHeight, player.z)
        val targetPos = Vector3d(targetX, targetY, targetZ)

        val existingBeam = activeLightBeams[playerId]

        if (existingBeam != null) {
            existingBeam.startPoint = playerPos
            existingBeam.endPoint = targetPos
            existingBeam.luminance = luminance
        } else {
            val newBeam = object : LineLightBehavior(playerPos, targetPos, luminance) {
                override fun isRemoved(): Boolean = player.isRemoved
            }
            activeLightBeams[playerId] = newBeam

            addDynamicLight(newBeam)
        }
    }

    fun removeBeamLight(playerId: Int) {
        if (!isInitialized) return

        val beam = activeLightBeams.remove(playerId)
        if (beam != null) {
            removeDynamicLight(beam)
        }
    }
}