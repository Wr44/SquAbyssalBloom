package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class BioluminescentTexture(
    private val textureManager: TextureManager,
    val identifier: Identifier,
    val widthInBlocks: Int,
    val lengthInBlocks: Int,
    val shape: BioluminescentBloomShape,
    private val rotationRadians: Double? = null
) : AutoCloseable {
    val resolutionX = chooseResolution(widthInBlocks)
    val resolutionZ = chooseResolution(lengthInBlocks)

    var isVisible: Boolean = false
        private set

    private val pixelCount = resolutionX * resolutionZ
    val uploadCostUnits = max(1, pixelCount / REFERENCE_TEXTURE_PIXELS)
    private val texture = DynamicTexture(
        "Squ Abyssal Bloom bioluminescent water ${identifier.path}",
        resolutionX,
        resolutionZ,
        true
    )
    private val densities = FloatArray(pixelCount)
    private val colors = IntArray(pixelCount)
    private val uploadedPixels = IntArray(pixelCount)
    private val blockCoverages = FloatArray(widthInBlocks * lengthInBlocks)
    private val blockSampleCounts = IntArray(widthInBlocks * lengthInBlocks)

    private var preparedSeed: Long? = null
    var isClear = true
        private set
    var hasUploadedVisualState = false
        private set
    private var lastUploadedLifecycleIntensity = Float.NaN
    private var lastUploadedPulse = Float.NaN
    private var lastUploadedFlicker = Float.NaN
    private var lastUploadedIntensityScale = Float.NaN
    private var closed = false

    init {
        require(widthInBlocks > 0)
        require(lengthInBlocks > 0)
        textureManager.register(identifier, texture)
        texture.upload()
    }

    fun prepare(
        state: BioluminescentBloomState,
        zoneSeed: Long,
        worldOriginX: Int,
        worldOriginZ: Int
    ) {
        checkOpen()

        blockCoverages.fill(0.0f)
        blockSampleCounts.fill(0)

        val sampler = BioluminescentNoiseSampler(state.seed, zoneSeed)
        val geometryRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(state.seed xor GEOMETRY_SEED_SALT)
        )
        val centerX = (geometryRandom.nextDouble() - 0.5) * 0.32
        val centerZ = (geometryRandom.nextDouble() - 0.5) * 0.32
        val radiusX = 0.58 + geometryRandom.nextDouble() * 0.24
        val radiusZ = 0.50 + geometryRandom.nextDouble() * 0.28
        val rotation = rotationRadians ?: geometryRandom.nextDouble() * PI * 2.0
        val rotationCos = cos(rotation)
        val rotationSin = sin(rotation)
        val roundedCorner = 0.16 + geometryRandom.nextDouble() * 0.18
        val capsuleLength = 0.42 + geometryRandom.nextDouble() * 0.24
        val capsuleRadius = 0.23 + geometryRandom.nextDouble() * 0.14
        val contourWarpStrength = 0.28 + geometryRandom.nextDouble() * 0.16

        val clusterCount = 5 + geometryRandom.nextInt(4)
        val clusterX = DoubleArray(clusterCount)
        val clusterZ = DoubleArray(clusterCount)
        val clusterRadiusX = DoubleArray(clusterCount)
        val clusterRadiusZ = DoubleArray(clusterCount)
        for (index in 0 until clusterCount) {
            clusterX[index] = (geometryRandom.nextDouble() - 0.5) * 1.24
            clusterZ[index] = (geometryRandom.nextDouble() - 0.5) * 1.12
            clusterRadiusX[index] = 0.26 + geometryRandom.nextDouble() * 0.34
            clusterRadiusZ[index] = 0.23 + geometryRandom.nextDouble() * 0.32
        }

        val colorPhaseOffset = state.colorPhase / (PI * 2.0) * COLOR_PHASE_RANGE

        for (pixelZ in 0 until resolutionZ) {
            for (pixelX in 0 until resolutionX) {
                val index = pixelZ * resolutionX + pixelX
                val textureX = (pixelX + 0.5) / resolutionX * 2.0 - 1.0
                val textureZ = (pixelZ + 0.5) / resolutionZ * 2.0 - 1.0
                val normalizedX = textureX - centerX
                val normalizedZ = textureZ - centerZ
                val rotatedX = normalizedX * rotationCos - normalizedZ * rotationSin
                val rotatedZ = normalizedX * rotationSin + normalizedZ * rotationCos
                val sampleX = (pixelX + 0.5) / resolutionX * widthInBlocks * PIXELS_PER_BLOCK_REFERENCE
                val sampleZ = (pixelZ + 0.5) / resolutionZ * lengthInBlocks * PIXELS_PER_BLOCK_REFERENCE
                val contourWarpX = sampler.sampleLarge(
                    (sampleX + 101.0) * CONTOUR_WARP_SCALE,
                    (sampleZ - 47.0) * CONTOUR_WARP_SCALE
                ) * 2.0 - 1.0
                val contourWarpZ = sampler.sampleLarge(
                    (sampleZ + 151.0) * CONTOUR_WARP_SCALE,
                    (sampleX - 89.0) * CONTOUR_WARP_SCALE
                ) * 2.0 - 1.0
                val warpedX = rotatedX + contourWarpX * contourWarpStrength
                val warpedZ = rotatedZ + contourWarpZ * contourWarpStrength

                val organicLobes = clusteredMask(
                    warpedX,
                    warpedZ,
                    clusterX,
                    clusterZ,
                    clusterRadiusX,
                    clusterRadiusZ
                )
                val baseMask = when (shape) {
                    BioluminescentBloomShape.ELLIPTICAL -> ellipseMask(
                        warpedX,
                        warpedZ,
                        radiusX,
                        radiusZ
                    )
                    BioluminescentBloomShape.RECTANGULAR -> roundedRectangleMask(
                        warpedX,
                        warpedZ,
                        radiusX,
                        radiusZ,
                        roundedCorner
                    )
                    BioluminescentBloomShape.ELONGATED -> capsuleMask(
                        warpedX,
                        warpedZ,
                        capsuleLength,
                        capsuleRadius
                    )
                    BioluminescentBloomShape.CLUSTERED -> organicLobes
                }
                val organicBaseMask = if (shape == BioluminescentBloomShape.CLUSTERED) {
                    organicLobes
                } else {
                    max(baseMask, organicLobes * ORGANIC_LOBE_INFLUENCE)
                }

                val largeNoise = sampler.sampleLarge(sampleX * 0.15, sampleZ * 0.15) * 0.72 +
                    sampler.sampleLarge((sampleX + 19.0) * 0.28, (sampleZ - 13.0) * 0.28) * 0.28
                val detailNoise = sampler.sampleDetail(sampleX * 0.55, sampleZ * 0.55) * 0.72 +
                    sampler.sampleDetail((sampleX - 31.0) * 0.95, (sampleZ + 23.0) * 0.95) * 0.28
                val contourNoise = sampler.sampleLarge(
                    (sampleX - 173.0) * CONTOUR_NOISE_SCALE,
                    (sampleZ + 211.0) * CONTOUR_NOISE_SCALE
                )

                val irregularField = organicBaseMask +
                    (contourNoise - 0.5) * 0.88 +
                    (largeNoise - 0.5) * 0.30 +
                    (detailNoise - 0.5) * 0.06
                val textureEdgeDistance = max(abs(textureX), abs(textureZ))
                val textureEdgeStart = (
                    TEXTURE_EDGE_FADE_START +
                        (contourNoise - 0.5) * TEXTURE_EDGE_NOISE_INFLUENCE +
                        (largeNoise - 0.5) * TEXTURE_EDGE_LARGE_NOISE_INFLUENCE
                    ).coerceIn(MINIMUM_TEXTURE_EDGE_FADE_START, MAXIMUM_TEXTURE_EDGE_FADE_START)
                val textureEdgeEnvelope = 1.0 - ModUtilities.smooth(
                    textureEdgeStart,
                    (textureEdgeStart + TEXTURE_EDGE_FADE_WIDTH).coerceAtMost(TEXTURE_EDGE_FADE_END),
                    textureEdgeDistance
                )
                val silhouette = ModUtilities.smooth(0.22, 0.62, irregularField) * textureEdgeEnvelope
                val holeMask = ModUtilities.smooth(
                    0.37,
                    0.50,
                    detailNoise * 0.72 + largeNoise * 0.28
                )
                val concentration = largeNoise * 0.62 + detailNoise * 0.38
                val density = (
                    silhouette * holeMask * (0.42 + concentration * 0.58)
                    ).coerceIn(0.0, 1.0)
                val blockX = pixelX * widthInBlocks / resolutionX
                val blockZ = pixelZ * lengthInBlocks / resolutionZ
                val blockIndex = blockZ * widthInBlocks + blockX
                blockSampleCounts[blockIndex]++

                if (density < MINIMUM_DENSITY) {
                    densities[index] = 0.0f
                    colors[index] = 0
                    continue
                }

                densities[index] = density.toFloat()
                blockCoverages[blockIndex] += density.toFloat()

                val colorMix = ModUtilities.smooth(
                    0.16,
                    0.84,
                    sampler.sampleColor(
                        (worldOriginX + (pixelX + 0.5) / resolutionX * widthInBlocks) * COLOR_WORLD_SCALE +
                            colorPhaseOffset,
                        (worldOriginZ + (pixelZ + 0.5) / resolutionZ * lengthInBlocks) * COLOR_WORLD_SCALE -
                            colorPhaseOffset
                    )
                )
                val localColor = lerpColor(
                    state.palette.firstColor,
                    state.palette.secondColor,
                    colorMix
                )
                val brightness = (
                    (0.46 + ModUtilities.smooth(0.08, 0.78, density) * 0.54) * state.brightnessScale
                    ).coerceIn(0.0, 1.08)
                val coloredGlow = scaleColor(localColor, brightness)
                val highlightMix = ModUtilities.smooth(0.62, 0.92, density) * 0.78
                colors[index] = lerpColor(
                    coloredGlow,
                    state.palette.highlightColor,
                    highlightMix
                )
            }
        }

        for (index in blockCoverages.indices) {
            val sampleCount = blockSampleCounts[index]
            if (sampleCount > 0) blockCoverages[index] /= sampleCount.toFloat()
        }

        preparedSeed = state.seed
        hasUploadedVisualState = false
        lastUploadedLifecycleIntensity = Float.NaN
        lastUploadedPulse = Float.NaN
        lastUploadedFlicker = Float.NaN
        lastUploadedIntensityScale = Float.NaN
    }

    fun coverageAtCell(index: Int): Float = blockCoverages[index]

    fun hasUploadedFlickerBoost(): Boolean {
        return hasUploadedVisualState && lastUploadedFlicker > ACTIVE_UPLOADED_FLICKER_THRESHOLD
    }

    fun needsUpload(
        state: BioluminescentBloomState,
        lifecycleIntensity: Float,
        pulse: Float,
        flicker: Float,
        intensityScale: Float
    ): Boolean {
        checkOpen()
        check(preparedSeed == state.seed) { "Bioluminescent texture was not prepared for this bloom" }

        val clampedLifecycle = lifecycleIntensity.coerceIn(0.0f, 1.0f)
        val clampedPulse = pulse.coerceIn(MINIMUM_PULSE, MAXIMUM_PULSE)
        val clampedFlicker = flicker.coerceIn(1.0f, MAXIMUM_FLICKER)
        val clampedScale = intensityScale.coerceIn(0.0f, MAXIMUM_INTENSITY_SCALE)
        val finalBaseIntensity = clampedLifecycle * clampedPulse * clampedScale

        if (finalBaseIntensity <= CLEAR_INTENSITY_THRESHOLD) return !isClear
        if (!hasUploadedVisualState || isClear) return true
        return abs(clampedLifecycle - lastUploadedLifecycleIntensity) >= LIFECYCLE_UPLOAD_THRESHOLD ||
            abs(clampedPulse - lastUploadedPulse) >= PULSE_UPLOAD_THRESHOLD ||
            abs(clampedFlicker - lastUploadedFlicker) >= FLICKER_UPLOAD_THRESHOLD ||
            abs(clampedScale - lastUploadedIntensityScale) >= INTENSITY_SCALE_UPLOAD_THRESHOLD
    }

    fun update(
        state: BioluminescentBloomState,
        lifecycleIntensity: Float,
        pulse: Float,
        flicker: Float,
        intensityScale: Float
    ): Boolean {
        checkOpen()
        check(preparedSeed == state.seed) { "Bioluminescent texture was not prepared for this bloom" }

        val clampedLifecycle = lifecycleIntensity.coerceIn(0.0f, 1.0f)
        val clampedPulse = pulse.coerceIn(MINIMUM_PULSE, MAXIMUM_PULSE)
        val clampedFlicker = flicker.coerceIn(1.0f, MAXIMUM_FLICKER)
        val clampedScale = intensityScale.coerceIn(0.0f, MAXIMUM_INTENSITY_SCALE)
        if (!needsUpload(state, clampedLifecycle, clampedPulse, clampedFlicker, clampedScale)) return false

        val temporalIntensity = clampedLifecycle * clampedPulse * clampedScale
        val flickerGain = clampedFlicker - 1.0f
        val pixels = texture.pixels
        var visible = false
        var changed = false

        for (index in 0 until pixelCount) {
            val density = densities[index]
            val localFlicker = 1.0f + flickerGain * density
            val alpha = (density * temporalIntensity * localFlicker * MAX_ALPHA)
                .roundToInt()
                .coerceIn(0, 255)
            val pixel = if (alpha == 0) 0 else (alpha shl 24) or colors[index]

            if (pixel != uploadedPixels[index]) {
                uploadedPixels[index] = pixel
                pixels.setPixel(index % resolutionX, index / resolutionX, pixel)
                changed = true
            }
            if (alpha != 0) visible = true
        }

        if (changed) texture.upload()
        isVisible = visible
        isClear = !visible
        hasUploadedVisualState = true
        lastUploadedLifecycleIntensity = clampedLifecycle
        lastUploadedPulse = clampedPulse
        lastUploadedFlicker = clampedFlicker
        lastUploadedIntensityScale = clampedScale
        return changed
    }

    fun clear() {
        checkOpen()
        if (isClear) return

        val pixels = texture.pixels
        var changed = false
        for (index in 0 until pixelCount) {
            if (uploadedPixels[index] == 0) continue
            uploadedPixels[index] = 0
            pixels.setPixel(index % resolutionX, index / resolutionX, 0)
            changed = true
        }

        if (changed) texture.upload()
        isVisible = false
        isClear = true
        hasUploadedVisualState = true
        lastUploadedLifecycleIntensity = 0.0f
        lastUploadedPulse = 1.0f
        lastUploadedFlicker = 1.0f
        lastUploadedIntensityScale = 0.0f
    }

    override fun close() {
        if (closed) return
        textureManager.release(identifier)
        preparedSeed = null
        closed = true
        isVisible = false
        isClear = true
        hasUploadedVisualState = false
    }

    private fun checkOpen() {
        check(!closed) { "Bioluminescent texture is closed" }
    }

    private fun ellipseMask(x: Double, z: Double, radiusX: Double, radiusZ: Double): Double {
        val distance = sqrt(x * x / (radiusX * radiusX) + z * z / (radiusZ * radiusZ))
        return 1.0 - ModUtilities.smooth(0.52, 1.08, distance)
    }

    private fun roundedRectangleMask(
        x: Double,
        z: Double,
        halfWidth: Double,
        halfLength: Double,
        cornerRadius: Double
    ): Double {
        val qx = abs(x) - halfWidth + cornerRadius
        val qz = abs(z) - halfLength + cornerRadius
        val outside = hypot(max(qx, 0.0), max(qz, 0.0))
        val inside = min(max(qx, qz), 0.0)
        val signedDistance = outside + inside - cornerRadius
        return 1.0 - ModUtilities.smooth(-0.10, 0.12, signedDistance)
    }

    private fun capsuleMask(
        x: Double,
        z: Double,
        halfSegmentLength: Double,
        radius: Double
    ): Double {
        val closestX = x.coerceIn(-halfSegmentLength, halfSegmentLength)
        val distance = hypot(x - closestX, z) / radius
        return 1.0 - ModUtilities.smooth(0.62, 1.14, distance)
    }

    private fun clusteredMask(
        x: Double,
        z: Double,
        centersX: DoubleArray,
        centersZ: DoubleArray,
        radiiX: DoubleArray,
        radiiZ: DoubleArray
    ): Double {
        var mask = 0.0
        for (index in centersX.indices) {
            mask = max(
                mask,
                ellipseMask(
                    x - centersX[index],
                    z - centersZ[index],
                    radiiX[index],
                    radiiZ[index]
                )
            )
        }
        return mask
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

    companion object {
        const val MINIMUM_RESOLUTION = 64
        const val MAXIMUM_RESOLUTION = 256
        private const val PIXELS_PER_BLOCK_REFERENCE = 8.0
        private const val MAX_ALPHA = 240.0f
        private const val MINIMUM_DENSITY = 0.025
        private const val MINIMUM_PULSE = 0.85f
        private const val MAXIMUM_PULSE = 1.05f
        private const val MAXIMUM_FLICKER = 1.40f
        private const val ACTIVE_UPLOADED_FLICKER_THRESHOLD = 1.002f
        private const val MAXIMUM_INTENSITY_SCALE = 1.10f
        private const val CLEAR_INTENSITY_THRESHOLD = 0.0005f
        const val LIFECYCLE_UPLOAD_THRESHOLD = 0.008f
        const val PULSE_UPLOAD_THRESHOLD = 0.012f
        const val FLICKER_UPLOAD_THRESHOLD = 0.010f
        const val INTENSITY_SCALE_UPLOAD_THRESHOLD = 0.008f
        private const val REFERENCE_TEXTURE_PIXELS = 64 * 64
        private const val ORGANIC_LOBE_INFLUENCE = 0.72
        private const val TEXTURE_EDGE_FADE_START = 0.84
        private const val TEXTURE_EDGE_FADE_END = 0.995
        private const val TEXTURE_EDGE_FADE_WIDTH = 0.13
        private const val TEXTURE_EDGE_NOISE_INFLUENCE = 0.16
        private const val TEXTURE_EDGE_LARGE_NOISE_INFLUENCE = 0.06
        private const val MINIMUM_TEXTURE_EDGE_FADE_START = 0.76
        private const val MAXIMUM_TEXTURE_EDGE_FADE_START = 0.90
        private const val CONTOUR_WARP_SCALE = 0.027
        private const val CONTOUR_NOISE_SCALE = 0.052
        private const val COLOR_WORLD_SCALE = 0.52
        private const val COLOR_PHASE_RANGE = 2.08
        private const val GEOMETRY_SEED_SALT = 0x510E527FADE682D1L

        fun chooseResolution(blocks: Int): Int {
            val target = (blocks * PIXELS_PER_BLOCK_REFERENCE.toInt())
                .coerceIn(MINIMUM_RESOLUTION, MAXIMUM_RESOLUTION)
            var resolution = MINIMUM_RESOLUTION
            while (resolution < target && resolution < MAXIMUM_RESOLUTION) {
                resolution *= 2
            }
            return resolution.coerceAtMost(MAXIMUM_RESOLUTION)
        }
    }
}
