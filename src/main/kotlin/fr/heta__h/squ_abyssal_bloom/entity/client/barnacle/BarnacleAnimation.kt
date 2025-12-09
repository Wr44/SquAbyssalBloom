package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader


object BarnacleAnimation {
    val mouth_open: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/mouth_open"
        )
    )

    val mouth_close: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/mouth_close"
        )
    )
    val move_still: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/move_still"
        )
    )

    val move_rush: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/move_rush"
        )
    )
    val flee_still: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/flee_still"
        )
    )

    val flee_rush: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/flee_rush"
        )
    )
    val still_mouth_close: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/still_mouth_close"
        )
    )

    val still_mouth_open: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/still_mouth_open"
        )
    )

    val swallow: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/swallow"
        )
    )

    val swallow_stop: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/swallow_stop"
        )
    )

    val swallow_start: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/swallow_start"
        )
    )
}