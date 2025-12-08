package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder

enum class BarnacleAnimationState(val anim: AnimationHolder) {
    STILL_MOUTH_CLOSE(BarnacleAnimation.mouth_close),
    STILL_MOUTH_OPEN(BarnacleAnimation.mouth_open),
    MOUTH_OPEN(BarnacleAnimation.mouth_open),
    MOUTH_CLOSE(BarnacleAnimation.mouth_close),
    MOVE_STILL(BarnacleAnimation.move_still),
    MOVE_RUSH(BarnacleAnimation.move_rush),
    FLEE_STILL(BarnacleAnimation.flee_still),
    FLEE_RUSH(BarnacleAnimation.flee_rush),
    SWALLOW(BarnacleAnimation.swallow),
    SWALLOW_START(BarnacleAnimation.swallow_start),
    SWALLOW_STOP(BarnacleAnimation.swallow_stop);
}