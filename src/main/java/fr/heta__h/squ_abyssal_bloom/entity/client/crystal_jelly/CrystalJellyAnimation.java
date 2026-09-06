package fr.heta__h.squ_abyssal_bloom.entity.client.crystal_jelly;// Save this class in your mod and generate all required imports

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Made with Blockbench 5.1.6
 * Exported for Minecraft version 1.19 or later with Mojang mappings
 * @author Author
 */
public class CrystalJellyAnimation {
	public static final AnimationDefinition swimm = AnimationDefinition.Builder.withLength(0.7917F).looping()
		.addAnimation("up", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0F, 1.3F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.7917F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("intern", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0F, 1.3F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.7917F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("down", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0F, 1.5F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.7917F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("exterior", new AnimationChannel(AnimationChannel.Targets.POSITION, 
			new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.7917F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.build();

	public static final AnimationDefinition idle = AnimationDefinition.Builder.withLength(1.5F).looping()
		.addAnimation("up", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.1F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.5F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("intern", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.1F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.5F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("down", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.1F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.5F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("exterior", new AnimationChannel(AnimationChannel.Targets.POSITION, 
			new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.build();

	public static final AnimationDefinition out_of_water = AnimationDefinition.Builder.withLength(2.0F).looping()
		.addAnimation("up", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.scaleVec(1.12F, 1.4F, 1.12F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.3F, KeyframeAnimations.scaleVec(0.95F, 0.8F, 0.95F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("intern", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.scaleVec(1.12F, 1.4F, 1.12F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.3F, KeyframeAnimations.scaleVec(0.95F, 0.8F, 0.95F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("down", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 0.85F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.scaleVec(1.0F, 1.45F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.3F, KeyframeAnimations.scaleVec(1.0F, 0.78F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.scaleVec(1.0F, 0.85F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle1", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(-93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle2", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle3", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle4", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(-93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle5", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle6", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(-93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle7", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle8", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(-93.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.build();

	public static final AnimationDefinition idle_to_out_of_water = AnimationDefinition.Builder.withLength(0.6F)
		.addAnimation("up", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("intern", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("down", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.scaleVec(1.0F, 0.85F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle1", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(-98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle2", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle3", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle4", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(-98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle5", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle6", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(-98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle7", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle8", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.45F, KeyframeAnimations.degreeVec(-98.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.6F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.build();

	public static final AnimationDefinition out_of_water_to_idle = AnimationDefinition.Builder.withLength(0.4F)
		.addAnimation("up", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("intern", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.95F, 0.85F, 0.95F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("down", new AnimationChannel(AnimationChannel.Targets.SCALE, 
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 0.85F, 1.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle1", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle2", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle3", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle4", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle5", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle6", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle7", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("tentacle8", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.4F, KeyframeAnimations.degreeVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.build();

	public static final AnimationDefinition tentacle_drift = AnimationDefinition.Builder.withLength(3.0F).looping()
		.addAnimation("tentacle", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(0.00F, 0.0F, 1.00F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(0.87F, 0.0F, 0.50F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(0.87F, 0.0F, -0.50F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(0.00F, 0.0F, -1.00F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(-0.87F, 0.0F, -0.50F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(-0.87F, 0.0F, 0.50F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(-0.00F, 0.0F, 1.00F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle1", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(0.00F, 0.0F, 1.75F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(2.17F, 0.0F, 0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(2.17F, 0.0F, -0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(0.00F, 0.0F, -1.75F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(-2.17F, 0.0F, -0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(-2.17F, 0.0F, 0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(-0.00F, 0.0F, 1.75F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle2", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(1.77F, 0.0F, 1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(2.41F, 0.0F, -0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(0.65F, 0.0F, -1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(-1.77F, 0.0F, -1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(-2.41F, 0.0F, 0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(-0.65F, 0.0F, 1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(1.77F, 0.0F, 1.24F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle3", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(2.50F, 0.0F, 0.00F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(1.25F, 0.0F, -1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(-1.25F, 0.0F, -1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(-2.50F, 0.0F, -0.00F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(-1.25F, 0.0F, 1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(1.25F, 0.0F, 1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(2.50F, 0.0F, 0.00F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle4", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(1.77F, 0.0F, -1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(-0.65F, 0.0F, -1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(-2.41F, 0.0F, -0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(-1.77F, 0.0F, 1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(0.65F, 0.0F, 1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(2.41F, 0.0F, 0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(1.77F, 0.0F, -1.24F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle5", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(0.00F, 0.0F, -1.75F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(-2.17F, 0.0F, -0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(-2.17F, 0.0F, 0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(-0.00F, 0.0F, 1.75F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(2.17F, 0.0F, 0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(2.17F, 0.0F, -0.88F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(0.00F, 0.0F, -1.75F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle6", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(-1.77F, 0.0F, -1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(-2.41F, 0.0F, 0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(-0.65F, 0.0F, 1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(1.77F, 0.0F, 1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(2.41F, 0.0F, -0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(0.65F, 0.0F, -1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(-1.77F, 0.0F, -1.24F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle7", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(-2.50F, 0.0F, -0.00F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(-1.25F, 0.0F, 1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(1.25F, 0.0F, 1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(2.50F, 0.0F, 0.00F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(1.25F, 0.0F, -1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(-1.25F, 0.0F, -1.52F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(-2.50F, 0.0F, -0.00F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.addAnimation("tentacle8", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0000F, KeyframeAnimations.degreeVec(-1.77F, 0.0F, 1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(0.5000F, KeyframeAnimations.degreeVec(0.65F, 0.0F, 1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.0000F, KeyframeAnimations.degreeVec(2.41F, 0.0F, 0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5000F, KeyframeAnimations.degreeVec(1.77F, 0.0F, -1.24F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.0000F, KeyframeAnimations.degreeVec(-0.65F, 0.0F, -1.69F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.5000F, KeyframeAnimations.degreeVec(-2.41F, 0.0F, -0.45F), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3.0000F, KeyframeAnimations.degreeVec(-1.77F, 0.0F, 1.24F), AnimationChannel.Interpolations.CATMULLROM)
		))
		.build();
}
