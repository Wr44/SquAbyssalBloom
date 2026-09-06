package fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave

enum class BioluminescentZoneGenerationStage {
    COLLECT_WATER_DOMAIN,
    SELECT_CORES,
    BUILD_GEODESIC_DISTANCES,
    BUILD_SKELETON,
    BUILD_MACRO_FIELD,
    INITIALIZE_REACTION_DIFFUSION,
    RUN_REACTION_DIFFUSION,
    GENERATE_EMISSION,
    CALIBRATE_VISIBLE_COVERAGE,
    CREATE_TILES,
    UPLOAD_TILES,
    READY,
    FAILED
}
