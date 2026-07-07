package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * A boxy little hauler: flat hull, forward sensor stub, and an underslung
 * cargo crate hanging below the belly. Four diagonal motor arms with
 * oversized rotor discs, same family silhouette as the other quad drones.
 */
public class LogisticsDroneModel extends SinglePartEntityModel<LogisticsDroneEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("logistics_drone"), "main");

	private static final String[] ARM_NAMES = {"arm_fl", "arm_fr", "arm_bl", "arm_br"};

	private final ModelPart root;
	private final ModelPart[] rotors = new ModelPart[4];

	public LogisticsDroneModel(ModelPart root) {
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
						.uv(0, 0).cuboid(-3.5f, -1.5f, -3.0f, 7.0f, 3.0f, 6.0f)
						.uv(28, 0).cuboid(-1.5f, -0.5f, -4.0f, 3.0f, 1.0f, 1.0f),
				ModelTransform.pivot(0.0f, 16.0f, 0.0f));

		// Underslung cargo crate.
		body.addChild("crate",
				ModelPartBuilder.create().uv(0, 10).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, 4.0f, 5.0f),
				ModelTransform.pivot(0.0f, 1.5f, 0.0f));

		addArm(body, "arm_fl", -3.0f, -2.5f, (float) (-Math.PI / 4.0));
		addArm(body, "arm_fr", 3.0f, -2.5f, (float) (Math.PI / 4.0));
		addArm(body, "arm_bl", -3.0f, 2.5f, (float) (-Math.PI * 3.0 / 4.0));
		addArm(body, "arm_br", 3.0f, 2.5f, (float) (Math.PI * 3.0 / 4.0));

		return TexturedModelData.of(modelData, 64, 64);
	}

	private static void addArm(ModelPartData body, String name, float x, float z, float yaw) {
		ModelPartData arm = body.addChild(name,
				ModelPartBuilder.create()
						.uv(37, 0).cuboid(-0.5f, -0.5f, -4.0f, 1.0f, 1.0f, 4.0f)
						.uv(37, 7).cuboid(-1.0f, -1.5f, -5.0f, 2.0f, 2.0f, 2.0f),
				ModelTransform.of(x, -1.0f, z, 0.0f, yaw, 0.0f));
		// Rotor disc riding on the motor pod.
		arm.addChild("rotor",
				ModelPartBuilder.create().uv(0, 20).cuboid(-3.5f, -0.5f, -3.5f, 7.0f, 1.0f, 7.0f),
				ModelTransform.pivot(0.0f, -2.0f, -4.0f));
	}

	@Override
	public void setAngles(LogisticsDroneEntity entity, float limbAngle, float limbDistance,
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
