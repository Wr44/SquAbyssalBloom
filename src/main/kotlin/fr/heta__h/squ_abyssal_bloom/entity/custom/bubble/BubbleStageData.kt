package fr.heta__h.squ_abyssal_bloom.entity.custom.bubble

data class BubbleStageData(
    val size: Float,
    val burstRadius: Double,
    val burstDamage: Float,
    val burstKnockback: Double,
    val burstPitch: Float,
    val burstVolume: Float,
    val particleRings: Int,
    val particlePerRing: Int,
    val particleSplashCount: Int,
    val dragLateral: Double,
    val dragVertical: Double,
    val buoyancy: Double,
)