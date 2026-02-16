package fr.heta__h.squ_abyssal_bloom.damage_type

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.world.damagesource.DamageType

object ModDamagesTypes {
    val BARNACLE_SWALLOW: ResourceKey<DamageType> =
        ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "barnacle_swallow")
        )
}