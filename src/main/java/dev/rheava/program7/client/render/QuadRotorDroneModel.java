package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * A boxy quad-rotor chassis shared by the small flyers: armored body,
 * forward sensor strip, four spinning rotors. Deliberately reads as
 * "machine", not "creature". Each unit gets its own texture + model layer.
 */
public class QuadRotorDroneModel<T extends MobEntity> extends SinglePartEntityModel<T> {
	public static final EntityModelLayer SURVEYOR_LAYER = new EntityModelLayer(Program7.id("surveyor_drone"), "main");
	public static final EntityModelLayer ATTACK_LAYER = new EntityModelLayer(Program7.id("attack_drone"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart rotorFrontLeft;
	private final ModelPart rotorFrontRight;
	private final ModelPart rotorBackLeft;
	private final ModelPart rotorBackRight;

	public QuadRotorDroneModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.rotorFrontLeft = this.body.getChild("rotor_fl");
		this.rotorFrontRight = this.body.getChild("rotor_fr");
		this.rotorBackLeft = this.body.getChild("rotor_bl");
		this.rotorBackRight = this.body.getChild("rotor_br");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -2.0f, -4.0f, 8.0f, 4.0f, 8.0f)
						.uv(0, 13).cuboid(-2.0f, -1.0f, -5.0f, 4.0f, 2.0f, 1.0f),
				ModelTransform.pivot(0.0f, 17.0f, 0.0f));

		ModelPartBuilder rotor = ModelPartBuilder.create()
				.uv(33, 0).cuboid(-2.0f, -0.5f, -2.0f, 4.0f, 1.0f, 4.0f);
		body.addChild("rotor_fl", rotor, ModelTransform.pivot(-3.0f, -2.5f, -3.0f));
		body.addChild("rotor_fr", rotor, ModelTransform.pivot(3.0f, -2.5f, -3.0f));
		body.addChild("rotor_bl", rotor, ModelTransform.pivot(-3.0f, -2.5f, 3.0f));
		body.addChild("rotor_br", rotor, ModelTransform.pivot(3.0f, -2.5f, 3.0f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float spin = animationProgress * 2.2f;
		this.rotorFrontLeft.yaw = spin;
		this.rotorFrontRight.yaw = -spin;
		this.rotorBackLeft.yaw = -spin;
		this.rotorBackRight.yaw = spin;

		// Idle hover bob plus a slight nose-down lean toward whatever it watches.
		this.body.pivotY = 17.0f + MathHelper.sin(animationProgress * 0.18f) * 0.6f;
		this.body.pitch = headPitch * ((float) Math.PI / 180.0f) * 0.25f;
		this.body.yaw = headYaw * ((float) Math.PI / 180.0f) * 0.15f;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
