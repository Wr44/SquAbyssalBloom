package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.worldgen.ore.AbyssalOreCatalog
import fr.heta__h.squ_abyssal_bloom.worldgen.ore.AbyssalSurfaceOrePlacer
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PlacedFeature::class)
abstract class PlacedFeatureOreBoostMixin {
    @Inject(method = ["placeWithBiomeCheck"], at = [At("RETURN")], cancellable = true)
    private fun runAbyssalOrePasses(
        level: WorldGenLevel,
        generator: ChunkGenerator,
        random: RandomSource,
        origin: BlockPos,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (!ModServerConfig.ABYSSAL_ORE_ENRICHMENT_ENABLED.get()) return

        val feature = this as Any as PlacedFeature
        if (!AbyssalOreCatalog.contains(feature)) return

        val samplePos = BlockPos(origin.x + 8, level.seaLevel - 1, origin.z + 8)
        if (!level.getBiome(samplePos).`is`(ModTags.Biomes.IS_ABYSSAL)) return

        val passes = ModServerConfig.ABYSSAL_ORE_BONUS_PASSES.get()
        if (passes <= 0) return

        if (AbyssalSurfaceOrePlacer.place(level, random, origin, feature, passes)) {
            cir.returnValue = true
        }
    }
}
