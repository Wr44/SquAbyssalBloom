package fr.deotexh.cubicbezier

import org.joml.Math
import org.joml.Vector2f
import org.joml.Vector3fc
import org.joml.Vector3f
import kotlin.math.pow

object CubicBezier {
    private const val GETCUBIC_START_POINT: Int = 0
    private const val GETCUBIC_END_POINT: Int = 1

    fun applyToVector3F(currentPos: Vector3fc, nextPos: Vector3fc, keyframeDelta: Float, animationVecCache: Vector3f): Vector3f{
        return currentPos.lerp(nextPos, getCubic(keyframeDelta), animationVecCache)
    }

    private fun getCubic(delta: Float, easeOutFactor: Float = 0.42f, easeInFactor: Float = 0.58f): Float{
        return (1 - delta).pow(3) * GETCUBIC_START_POINT + 3 * (1 - delta).pow(2) * delta * easeOutFactor + 3 * (1 - delta) * (delta).pow(2) * easeInFactor + (delta).pow(3) * GETCUBIC_END_POINT;
    }
}