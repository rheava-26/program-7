package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.SniperDroneEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * A slim overwatch platform built around one silhouette-defining feature:
 * a very long fixed rifle with a scope riding above the barrel. Small
 * canted stabilizer fins flank the rear, and the whole chassis tilts
 * slightly to "aim" the fixed weapon since it has no separate turret.
 */
public class SniperDroneModel extends SinglePartEntityModel<SniperDroneEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("sniper_drone"), "main");

	private static final String[] ARM_NAMES = {"arm_fl", "arm_fr", "arm_bl", "arm_br"};

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart[] rotors = new ModelPart[4];

	public SniperDroneModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		for (int i = 0; i < ARM_NAMES.length; i++) {
			this.rotors[i] = this.body.getChild(ARM_NAMES[i]).getChild("rotor");
		}
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-2.5f, -2.0f, -3.5f, 5.0f, 4.0f, 7.0f)
						.uv(0, 12).cuboid(-0.5f, -0.5f, -13.0f, 1.0f, 1.0f, 10.0f)
						.uv(24, 12).cuboid(-0.5f, -2.0f, -8.0f, 1.0f, 1.0f, 3.0f),
				ModelTransform.pivot(0.0f, 14.0f, 0.0f));

		// Canted stabilizer fins.
		body.addChild("left_fin",
				ModelPartBuilder.create().uv(33, 12).cuboid(-0.5f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f),
				ModelTransform.of(2.5f, 0.5f, 2.0f, 0.0f, 0.0f, -0.45f));
		body.addChild("right_fin",
				ModelPartBuilder.create().uv(33, 12).cuboid(-0.5f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f),
				ModelTransform.of(-2.5f, 0.5f, 2.0f, 0.0f, 0.0f, 0.45f));

		addArm(body, "arm_fl", -2.5f, -2.5f, (float) (-Math.PI / 4.0));
		addArm(body, "arm_fr", 2.5f, -2.5f, (float) (Math.PI / 4.0));
		addArm(body, "arm_bl", -2.5f, 2.5f, (float) (-Math.PI * 3.0 / 4.0));
		addArm(body, "arm_br", 2.5f, 2.5f, (float) (Math.PI * 3.0 / 4.0));

		return TexturedModelData.of(modelData, 64, 64);
	}

	private static void addArm(ModelPartData body, String name, float x, float z, float yaw) {
		ModelPartData arm = body.addChild(name,
				ModelPartBuilder.create()
						.uv(42, 0).cuboid(-0.5f, -0.5f, -4.0f, 1.0f, 1.0f, 4.0f)
						.uv(42, 6).cuboid(-1.0f, -1.5f, -5.0f, 2.0f, 2.0f, 2.0f),
				ModelTransform.of(x, -2.0f, z, 0.0f, yaw, 0.0f));
		// Rotor disc riding on the motor pod.
		arm.addChild("rotor",
				ModelPartBuilder.create().uv(0, 24).cuboid(-3.5f, -0.5f, -3.5f, 7.0f, 1.0f, 7.0f),
				ModelTransform.pivot(0.0f, -2.0f, -4.0f));
	}

	@Override
	public void setAngles(SniperDroneEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float spin = animationProgress * 2.8f;
		for (int i = 0; i < this.rotors.length; i++) {
			this.rotors[i].yaw = (i % 2 == 0) ? spin : -spin;
		}

		// The platform tilts slightly to aim its fixed rifle.
		this.body.pitch = headPitch * ((float) Math.PI / 180.0f) * 0.5f;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
