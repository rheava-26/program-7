package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.TransportDroneEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * A cargo hauler carrying twin crates slung either side of its belly,
 * forward sensor stub on the hull, and the same four-arm rotor
 * arrangement as the rest of the quad-drone family.
 */
public class TransportDroneModel extends SinglePartEntityModel<TransportDroneEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("transport_drone"), "main");

	private static final String[] ARM_NAMES = {"arm_fl", "arm_fr", "arm_bl", "arm_br"};

	private final ModelPart root;
	private final ModelPart[] rotors = new ModelPart[4];

	public TransportDroneModel(ModelPart root) {
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
						.uv(0, 0).cuboid(-4.0f, -1.5f, -3.5f, 8.0f, 3.0f, 7.0f)
						.uv(31, 0).cuboid(-1.5f, -0.5f, -4.5f, 3.0f, 1.0f, 1.0f),
				ModelTransform.pivot(0.0f, 16.5f, 0.0f));

		// Twin cargo crates slung either side of the belly.
		body.addChild("left_crate",
				ModelPartBuilder.create().uv(0, 11).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 4.0f, 4.0f),
				ModelTransform.pivot(2.25f, 1.5f, 0.0f));
		body.addChild("right_crate",
				ModelPartBuilder.create().uv(0, 11).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 4.0f, 4.0f),
				ModelTransform.pivot(-2.25f, 1.5f, 0.0f));

		addArm(body, "arm_fl", -3.5f, -1.0f, -3.0f, (float) (-Math.PI / 4.0));
		addArm(body, "arm_fr", 3.5f, -1.0f, -3.0f, (float) (Math.PI / 4.0));
		addArm(body, "arm_bl", -3.5f, -1.0f, 3.0f, (float) (-Math.PI * 3.0 / 4.0));
		addArm(body, "arm_br", 3.5f, -1.0f, 3.0f, (float) (Math.PI * 3.0 / 4.0));

		return TexturedModelData.of(modelData, 64, 64);
	}

	private static void addArm(ModelPartData body, String name, float x, float y, float z, float yaw) {
		ModelPartData arm = body.addChild(name,
				ModelPartBuilder.create()
						.uv(40, 3).cuboid(-0.5f, -0.5f, -5.0f, 1.0f, 1.0f, 5.0f)
						.uv(40, 11).cuboid(-1.0f, -1.5f, -6.0f, 2.0f, 2.0f, 2.0f),
				ModelTransform.of(x, y, z, 0.0f, yaw, 0.0f));
		// Rotor disc riding on the motor pod.
		arm.addChild("rotor",
				ModelPartBuilder.create().uv(0, 20).cuboid(-4.0f, -0.5f, -4.0f, 8.0f, 1.0f, 8.0f),
				ModelTransform.pivot(0.0f, -2.0f, -5.0f));
	}

	@Override
	public void setAngles(TransportDroneEntity entity, float limbAngle, float limbDistance,
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
