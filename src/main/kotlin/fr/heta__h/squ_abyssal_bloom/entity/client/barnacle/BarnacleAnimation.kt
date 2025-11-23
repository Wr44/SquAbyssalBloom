package fr.heta__h.squ_abyssal_bloom.entity.client

import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.animation.KeyframeAnimations



object BarnacleAnimation {
    val mouth_open: AnimationDefinition = AnimationDefinition.Builder.withLength(0.3333f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0417f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 26.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0417f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.scaleVec(1.0, 1.0, 16.7), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Loca", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(
                    0.25f,
                    KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(
                    0.25f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(
                    0.25f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(
                    0.25f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val mouth_close: AnimationDefinition = AnimationDefinition.Builder.withLength(0.75f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 26.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.2917f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 16.7), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.2917f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Loca", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Arrire", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0833f,
                    KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0833f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0833f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0833f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0833f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0833f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0833f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0833f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val move_still: AnimationDefinition = AnimationDefinition.Builder.withLength(0.7083f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.01, 1.01, 1.1), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.posVec(0.0f, 0.0f, 2.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.scaleVec(1.2, 1.2, 0.7), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.posVec(-1.5f, -1.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.scaleVec(1.3, 1.2, 0.8), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val move_rush: AnimationDefinition = AnimationDefinition.Builder.withLength(0.875f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.01, 1.01, 1.1), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 2.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.2, 1.2, 0.7), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(-1.5f, -1.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.3, 1.2, 0.8), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val flee_still: AnimationDefinition = AnimationDefinition.Builder.withLength(0.7083f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.01, 1.01, 1.1), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.posVec(0.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.scaleVec(1.5, 1.5, 0.4), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.posVec(-2.6f, -2.5f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7083f, KeyframeAnimations.scaleVec(1.6, 1.5, 0.5), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val flee_rush: AnimationDefinition = AnimationDefinition.Builder.withLength(1.25f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.01, 1.01, 1.1), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.5, 1.5, 0.4), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(-2.6f, -2.5f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.6, 1.5, 0.5), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val still_mouth_close: AnimationDefinition = AnimationDefinition.Builder.withLength(1.5f).looping()
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.scaleVec(1.01, 1.01, 1.1), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.posVec(0.0f, 0.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.scaleVec(1.1, 1.1, 0.9), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.posVec(-0.6f, -0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Bouche", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.scaleVec(1.1, 1.1, 0.9), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val still_mouth_open: AnimationDefinition = AnimationDefinition.Builder.withLength(1.5f).looping()
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 26.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.posVec(0.0f, 0.0f, 27.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.posVec(0.0f, 0.0f, 26.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 16.7), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.scaleVec(0.9, 0.9, 17.5), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.scaleVec(1.0, 1.0, 16.7), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Loca", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.posVec(0.0f, 0.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Tentacules", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.scaleVec(1.2, 1.2, 0.8), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Arrire", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.2083f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.75f,
                    KeyframeAnimations.degreeVec(19.1288f, 20.4519f, -2.253f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(1.5f, KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.75f,
                    KeyframeAnimations.degreeVec(-19.1288f, 20.4519f, 2.253f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.5f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.75f,
                    KeyframeAnimations.degreeVec(-19.1288f, -20.4519f, -2.253f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.5f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.75f,
                    KeyframeAnimations.degreeVec(19.1288f, -20.4519f, 2.253f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.5f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val swallow: AnimationDefinition = AnimationDefinition.Builder.withLength(0.375f).looping()
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.1667f, KeyframeAnimations.posVec(0.0f, 0.0f, 17.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(
                    0.1667f,
                    KeyframeAnimations.scaleVec(1.0, 1.0, 12.7),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "Loca", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Arrire", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.1667f,
                    KeyframeAnimations.degreeVec(8.1908f, 11.011f, -5.5606f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.posVec(1.0f, -1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.1667f,
                    KeyframeAnimations.degreeVec(-8.1908f, 11.011f, 5.5606f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.posVec(0.0f, 1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.1667f,
                    KeyframeAnimations.degreeVec(-8.1908f, -11.011f, -5.5606f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.posVec(-1.0f, 1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.1667f,
                    KeyframeAnimations.degreeVec(8.1908f, -11.011f, 5.5606f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3333f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.1667f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val swallow_stop: AnimationDefinition = AnimationDefinition.Builder.withLength(0.375f)
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 17.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.2083f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 12.7), AnimationChannel.Interpolations.CATMULLROM),
                Keyframe(
                    0.2083f,
                    KeyframeAnimations.scaleVec(1.0, 1.0, 1.0),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "Loca", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Arrire", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(0.25f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(0.25f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(0.25f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(0.25f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val swallow_start: AnimationDefinition = AnimationDefinition.Builder.withLength(0.2083f)
        .addAnimation(
            "Arrière", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 26.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.2083f, KeyframeAnimations.posVec(0.0f, 0.0f, 17.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Langue", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 16.7), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.2083f, KeyframeAnimations.scaleVec(1.0, 1.0, 12.7), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Loca", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.125f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.3f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "Arrire", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(0.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(0.0f, KeyframeAnimations.degreeVec(25.0f, 25.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(1.0f, 0.0f, 4.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, 25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BG", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 1.0f, 2.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "BD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(-2.0f, 0.0f, 5.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(25.0f, -25.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "HD", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, -1.0f, 1.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()
}