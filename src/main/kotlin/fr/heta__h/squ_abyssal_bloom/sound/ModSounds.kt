package fr.heta__h.squ_abyssal_bloom.sound

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.registries.Registries
import net.minecraft.sounds.SoundEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModSounds {
    val SOUNDS: DeferredRegister<SoundEvent> =
        DeferredRegister.create(Registries.SOUND_EVENT, SquAbyssalBloom.ID)


    // Barnacle
    val BARNACLE_AMBIENT = SOUNDS.register("barnacle_ambient") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BARNACLE_DEATH = SOUNDS.register("barnacle_death") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BARNACLE_HURT = SOUNDS.register("barnacle_hurt") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BARNACLE_OPEN_MOUTH = SOUNDS.register("barnacle_open_mouth") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BARNACLE_CLOSE_MOUTH = SOUNDS.register("barnacle_close_mouth") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    // Brine
    val BRINE_AMBIENT = SOUNDS.register("brine_ambient") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BRINE_DEATH = SOUNDS.register("brine_death") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BRINE_HURT = SOUNDS.register("brine_hurt") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    // Bubble projectile
    val BUBBLE_PROJECTILE_BURST = SOUNDS.register("bubble_projectile_burst") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BUBBLE_PROJECTILE_BOUNCING = SOUNDS.register("bubble_projectile_bouncing") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BUBBLE_PROJECTILE_STAGE_UP = SOUNDS.register("bubble_projectile_stage_up") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val BUBBLE_PROJECTILE_LAUNCH = SOUNDS.register("bubble_projectile_launch") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }


    // Item

    // Abyssal Guardian Focalist

    val ABYSSAL_GUARDIAN_FOCALIST_READY = SOUNDS.register("abyssal_guardian_focalist_ready") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    // Respiration Bubble

    val RESPIRATION_BUBBLE = SOUNDS.register("respiration_bubble") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    
    // Blocks 
    
    // Conduit
    
    val CONDUIT_ENTERING = SOUNDS.register("conduit_entering") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    val CONDUIT_LEAVING = SOUNDS.register("conduit_leaving") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }


    // Damage Types

    // Pressure
    val PRESSURE_DAMAGE = SOUNDS.register("pressure_damage") { id ->
        SoundEvent.createVariableRangeEvent(id)
    }

    fun register(eventBus: IEventBus) {
        SOUNDS.register(eventBus)
    }
}