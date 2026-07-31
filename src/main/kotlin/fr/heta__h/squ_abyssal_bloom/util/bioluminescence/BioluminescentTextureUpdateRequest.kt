package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

data class BioluminescentTextureUpdateRequest(
    val bloom: BioluminescentBloom,
    val gameTime: Long,
    val lifecycleIntensity: Float,
    val pulse: Float,
    val flicker: Float,
    val intensityScale: Float,
    val priority: BioluminescentTextureUpdatePriority,
    val overdueTicks: Long,
    val uploadCostUnits: Int
)
