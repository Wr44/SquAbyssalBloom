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
            "animations/entity/barnacle/mouth_open.json"
        )
    )

    val mouth_close: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/mouth_close.json"
        )
    )
    val move_still: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/move_still.json"
        )
    )

    val move_rush: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/move_rush.json"
        )
    )
    val flee_still: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/flee_still.json"
        )
    )

    val flee_rush: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/flee_rush.json"
        )
    )
    val still_mouth_close: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "barnacle/still_mouth_close.json"
        )
    )

    val still_mouth_open: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/still_mouth_open.json"
        )
    )

    val swallow: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/swallow.json"
        )
    )

    val swallow_stop: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/swallow_stop.json"
        )
    )

    val swallow_start: AnimationHolder = AnimationLoader.INSTANCE.getAnimationHolder(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/swallow_start.json"
        )
    )
}