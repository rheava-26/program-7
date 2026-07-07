package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HeavyAttackDroneEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Tier 3 flagship of the attack drone line: a car-sized armored quad
 * hull with a chin-mounted twin-barrel turret, a forward sensor cluster,
 * and angled side skirts. Four heavy motor arms carry oversized rotor
 * discs, same family silhouette as the other quad drones, scaled up again.
 */
public class HeavyAttackDroneModel extends SinglePartEntityModel<HeavyAttackDroneEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("heavy_attack_drone"), "main");

	private static final String[] ARM_NAMES = {"arm_fl", "arm_fr", "arm_bl", "arm_br"};

	private final ModelPart root;
	private final ModelPart[] rotors = new ModelPart[4];

	public HeavyAttackDroneModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		for (int i = 0; i < ARM_NAMES.length; i++) {
			this.rotors[i] = body.getChild(ARM_NAMES[i]).getChild("rotor");
		}
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-12.0f, -5.0f, -14.0f, 24.0f, 10.0f, 28.0f)
						.uv(0, 40).cuboid(-4.0f, 5.0f, -10.0f, 8.0f, 4.0f, 6.0f)
						.uv(29, 40).cuboid(-3.0f, 6.0f, -16.0f, 1.0f, 1.0f, 6.0f)
						.uv(29, 40).cuboid(2.0f, 6.0f, -16.0f, 1.0f, 1.0f, 6.0f)
						.uv(44, 40).cuboid(-5.0f, -3.0f, -15.0f, 10.0f, 3.0f, 1.0f),
				ModelTransform.pivot(0.0f, 8.0f, 0.0f));

		body.addChild("left_skirt",
				ModelPartBuilder.create()
						.uv(0, 50).cuboid(-1.0f, -4.0f, -10.0f, 2.0f, 8.0f, 20.0f),
				ModelTransform.of(12.0f, 1.0f, 0.0f, 0.0f, 0.0f, -0.12f));
		body.addChild("right_skirt",
				ModelPartBuilder.create()
						.uv(0, 50).cuboid(-1.0f, -4.0f, -10.0f, 2.0f, 8.0f, 20.0f),
				ModelTransform.of(-12.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.12f));

		addArm(body, "arm_fl", -10.0f, -10.0f, (float) (-Math.PI / 4.0));
		addArm(body, "arm_fr", 10.0f, -10.0f, (float) (Math.PI / 4.0));
		addArm(body, "arm_bl", -10.0f, 10.0f, (float) (-Math.PI * 3.0 / 4.0));
		addArm(body, "arm_br", 10.0f, 10.0f, (float) (Math.PI * 3.0 / 4.0));

		return TexturedModelData.of(modelData, 128, 128);
	}

	private static void addArm(ModelPartData body, String name, float x, float z, float yaw) {
		ModelPartData arm = body.addChild(name,
				ModelPartBuilder.create()
						.uv(67, 40).cuboid(-1.0f, -1.0f, -10.0f, 2.0f, 2.0f, 10.0f)
						.uv(67, 53).cuboid(-2.0f, -3.0f, -12.0f, 4.0f, 4.0f, 4.0f),
				ModelTransform.of(x, -6.0f, z, 0.0f, yaw, 0.0f));
		// Rotor disc riding on the motor pod.
		arm.addChild("rotor",
				ModelPartBuilder.create().uv(0, 80).cuboid(-7.0f, -0.5f, -7.0f, 14.0f, 1.0f, 14.0f),
				ModelTransform.pivot(0.0f, -3.0f, -10.0f));
	}

	@Override
	public void setAngles(HeavyAttackDroneEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float spin = animationProgress * 2.8f;
		for (int i = 0; i < this.rotors.length; i++) {
			this.rotors[i].yaw = (i % 2 == 0) ? spin : -spin;
		}
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
