package fr.heta__h.squ_abyssal_bloom.damage_type

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.world.damagesource.DamageType

object ModDamagesTypes {
    val BARNACLE_SWALLOW: ResourceKey<DamageType> =
        ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "barnacle_swallow")
        )

    val BUBBLE_BURST: ResourceKey<DamageType> =
        ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bubble_burst")
        )

    val PRESSURE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "pressure")
    )
}