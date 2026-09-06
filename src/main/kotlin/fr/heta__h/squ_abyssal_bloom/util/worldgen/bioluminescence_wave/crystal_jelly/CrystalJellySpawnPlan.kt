package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.crystal_jelly

data class CrystalJellySpawnPlan(
    val targetPopulation: Int,
    val columns: List<CrystalJellySpawnColumn>,
    val swarmCentres: List<CrystalJellySpawnColumn>
)
