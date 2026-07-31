package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class BioluminescentTexture(
    private val textureManager: TextureManager
) : AutoCloseable {
    val identifier: Identifier = Identifier.fromNamespaceAndPath(
        SquAbyssalBloom.ID,
        "dynamic/bioluminescent_water"
    )

    var isVisible: Boolean = false
        private set

    private val texture = DynamicTexture(
        "Squ Abyssal Bloom bioluminescent water",
        RESOLUTION,
        RESOLUTION,
        true
    )
    private val densities = FloatArray(PIXEL_COUNT)
    private val colors = IntArray(PIXEL_COUNT)

    private var preparedSeed: Long? = null
    private var isClear = true
    private var closed = false

    init {
        textureManager.register(identifier, texture)
        texture.upload()
    }

    fun prepare(state: BioluminescentBloomState) {
        checkOpen()

        val sampler = BioluminescentNoiseSampler(state.seed)
        val geometryRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(state.seed xor GEOMETRY_SEED_SALT)
        )
        val centerX = (geometryRandom.nextDouble() - 0.5) * 0.18
        val centerZ = (geometryRandom.nextDouble() - 0.5) * 0.18
        val radiusX = 0.72 + geometryRandom.nextDouble() * 0.20
        val radiusZ = 0.62 + geometryRandom.nextDouble() * 0.26
        val rotation = geometryRandom.nextDouble() * PI * 2.0
        val rotationCos = cos(rotation)
        val rotationSin = sin(rotation)

        for (pixelZ in 0 until RESOLUTION) {
            for (pixelX in 0 until RESOLUTION) {
                val index = pixelZ * RESOLUTION + pixelX
                val normalizedX = (pixelX + 0.5) / RESOLUTION * 2.0 - 1.0 - centerX
                val normalizedZ = (pixelZ + 0.5) / RESOLUTION * 2.0 - 1.0 - centerZ
                val rotatedX = normalizedX * rotationCos - normalizedZ * rotationSin
                val rotatedZ = normalizedX * rotationSin + normalizedZ * rotationCos
                val ellipticalDistance = sqrt(
                    rotatedX * rotatedX / (radiusX * radiusX) +
                        rotatedZ * rotatedZ / (radiusZ * radiusZ)
                )
                val radialMask = 1.0 - ModUtilities.smoothstep(0.52, 1.08, ellipticalDistance)

                val sampleX = pixelX + 0.5
                val sampleZ = pixelZ + 0.5
                val largeNoise = (
                    sampler.sampleLarge(sampleX * 0.15, sampleZ * 0.15) * 0.72 +
                        sampler.sampleLarge((sampleX + 19.0) * 0.28, (sampleZ - 13.0) * 0.28) * 0.28
                    )
                val detailNoise = (
                    sampler.sampleDetail(sampleX * 0.55, sampleZ * 0.55) * 0.72 +
                        sampler.sampleDetail((sampleX - 31.0) * 0.95, (sampleZ + 23.0) * 0.95) * 0.28
                    )

                val irregularField = radialMask +
                    (largeNoise - 0.5) * 0.58 +
                    (detailNoise - 0.5) * 0.12
                val silhouette = ModUtilities.smoothstep(0.18, 0.54, irregularField) *
                    ModUtilities.smoothstep(0.0, 0.08, radialMask)
                val holeMask = ModUtilities.smoothstep(
                    0.37,
                    0.50,
                    detailNoise * 0.72 + largeNoise * 0.28
                )
                val concentration = largeNoise * 0.62 + detailNoise * 0.38
                val density = (
                    silhouette * holeMask * (0.42 + concentration * 0.58)
                    ).coerceIn(0.0, 1.0)

                if (density < MINIMUM_DENSITY) {
                    densities[index] = 0.0f
                    colors[index] = 0
                    continue
                }

                densities[index] = density.toFloat()

                val colorMix = ModUtilities.smoothstep(
                    0.16,
                    0.84,
                    sampler.sampleColor(sampleX * 0.065, sampleZ * 0.065)
                )
                val localColor = lerpColor(
                    state.palette.firstColor,
                    state.palette.secondColor,
                    colorMix
                )
                val brightness = 0.46 + ModUtilities.smoothstep(0.08, 0.78, density) * 0.54
                val coloredGlow = scaleColor(localColor, brightness)
                val highlightMix = ModUtilities.smoothstep(0.62, 0.92, density) * 0.78
                colors[index] = lerpColor(
                    coloredGlow,
                    state.palette.highlightColor,
                    highlightMix
                )
            }
        }

        preparedSeed = state.seed
    }

    fun update(state: BioluminescentBloomState, gameTime: Long) {
        checkOpen()
        check(preparedSeed == state.seed) { "Bioluminescent texture was not prepared for this bloom" }

        val temporalIntensity = state.lifecycleIntensityAt(gameTime) * state.pulseAt(gameTime)
        val pixels = texture.pixels
        var visible = false

        for (index in 0 until PIXEL_COUNT) {
            val density = densities[index]
            val alpha = (density * temporalIntensity * MAX_ALPHA).roundToInt().coerceIn(0, 255)
            val pixelX = index % RESOLUTION
            val pixelZ = index / RESOLUTION

            if (alpha == 0) {
                pixels.setPixel(pixelX, pixelZ, 0)
            } else {
                pixels.setPixel(pixelX, pixelZ, (alpha shl 24) or colors[index])
                visible = true
            }
        }

        texture.upload()
        isVisible = visible
        isClear = !visible
    }

    fun clear() {
        checkOpen()
        if (isClear) return

        val pixels = texture.pixels
        for (pixelZ in 0 until RESOLUTION) {
            for (pixelX in 0 until RESOLUTION) {
                pixels.setPixel(pixelX, pixelZ, 0)
            }
        }

        texture.upload()
        isVisible = false
        isClear = true
    }

    override fun close() {
        if (closed) return
        textureManager.release(identifier)
        closed = true
        isVisible = false
    }

    private fun checkOpen() {
        check(!closed) { "Bioluminescent texture is closed" }
    }

    private fun lerpColor(first: Int, second: Int, amount: Double): Int {
        val clamped = amount.coerceIn(0.0, 1.0)
        val red = lerpChannel(first shr 16 and 0xFF, second shr 16 and 0xFF, clamped)
        val green = lerpChannel(first shr 8 and 0xFF, second shr 8 and 0xFF, clamped)
        val blue = lerpChannel(first and 0xFF, second and 0xFF, clamped)
        return (red shl 16) or (green shl 8) or blue
    }

    private fun scaleColor(color: Int, scale: Double): Int {
        val red = ((color shr 16 and 0xFF) * scale).roundToInt().coerceIn(0, 255)
        val green = ((color shr 8 and 0xFF) * scale).roundToInt().coerceIn(0, 255)
        val blue = ((color and 0xFF) * scale).roundToInt().coerceIn(0, 255)
        return (red shl 16) or (green shl 8) or blue
    }

    private fun lerpChannel(first: Int, second: Int, amount: Double): Int {
        return (first + (second - first) * amount).roundToInt().coerceIn(0, 255)
    }

    private companion object {
        const val RESOLUTION = 64
        const val PIXEL_COUNT = RESOLUTION * RESOLUTION
        const val MAX_ALPHA = 240.0f
        const val MINIMUM_DENSITY = 0.025
        const val GEOMETRY_SEED_SALT = 0x510E527FADE682D1L
    }
}
