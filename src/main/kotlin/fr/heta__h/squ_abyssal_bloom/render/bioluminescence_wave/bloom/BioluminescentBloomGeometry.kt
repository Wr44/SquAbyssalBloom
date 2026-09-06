package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom

class BioluminescentBloomGeometry(
    val centerX: Double,
    val centerZ: Double,
    val surfaceY: Double,
    val waterDepth: Double,
    val haloRadius: Double,
    val satellites: List<BioluminescentBloomSatellite>,
    val lobeAngles: DoubleArray,
    val lobeOrbitRadius: Double,
    val lobeRadius: Double
)
