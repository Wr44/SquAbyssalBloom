package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader



object BarnacleAnimation {
    val mouth_open: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/mouth_open.json"
        )
    )

    val mouth_close: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/mouth_close.json"
        )
    )
    val move_still: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/move_still.json"
        )
    )

    val move_rush: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/move_rush.json"
        )
    )
    val flee_still: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/flee_still.json"
        )
    )

    val flee_rush: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/flee_rush.json"
        )
    )
    val still_mouth_close: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/still_mouth_close.json"
        )
    )

    val still_mouth_open: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/still_mouth_open.json"
        )
    )

    val swallow: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/swallow.json"
        )
    )

    val swallow_stop: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/swallow_stop.json"
        )
    )

    val swallow_start: AnimationDefinition? = AnimationLoader.INSTANCE.getAnimation(
        ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "animations/entity/barnacle/swallow_start.json"
        )
    )
}