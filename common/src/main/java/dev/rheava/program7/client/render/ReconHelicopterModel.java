package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ReconHelicopterEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Recon Helicopter: a slim fuselage riding on a pair of skid struts, a
 * long tail boom capped with a vertical fin, a searchlight slung under the
 * nose, and a main rotor overhead plus a tail rotor that never stop turning.
 * When it looks down at you, the nose dips slightly - predatory.
 */
public class ReconHelicopterModel extends SinglePartEntityModel<ReconHelicopterEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("recon_helicopter"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart rotor;
	private final ModelPart tailRotor;

	public ReconHelicopterModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.rotor = this.body.getChild("rotor");
		this.tailRotor = this.body.getChild("tail_rotor");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-5.0f, -5.0f, -12.0f, 10.0f, 10.0f, 20.0f)
						.uv(0, 31).cuboid(-1.5f, -3.0f, 8.0f, 3.0f, 3.0f, 14.0f)
						.uv(35, 31).cuboid(-0.5f, -7.0f, 20.0f, 1.0f, 4.0f, 3.0f)
						.uv(80, 0).cuboid(-1.5f, 5.0f, -10.0f, 3.0f, 2.0f, 3.0f),
				ModelTransform.pivot(0.0f, 12.0f, 0.0f));

		body.addChild("left_skid",
				ModelPartBuilder.create()
						.uv(44, 31).cuboid(-0.5f, 0.0f, -8.0f, 1.0f, 1.0f, 16.0f),
				ModelTransform.pivot(4.0f, 5.0f, 0.0f));
		body.addChild("right_skid",
				ModelPartBuilder.create()
						.uv(44, 31).cuboid(-0.5f, 0.0f, -8.0f, 1.0f, 1.0f, 16.0f),
				ModelTransform.pivot(-4.0f, 5.0f, 0.0f));

		body.addChild("rotor",
				ModelPartBuilder.create()
						.uv(0, 50).cuboid(-14.0f, -0.5f, -1.5f, 28.0f, 1.0f, 3.0f),
				ModelTransform.pivot(0.0f, -5.5f, 0.0f));

		body.addChild("tail_rotor",
				ModelPartBuilder.create()
						.uv(64, 50).cuboid(-0.5f, -3.0f, -3.0f, 1.0f, 6.0f, 6.0f),
				ModelTransform.pivot(0.0f, -1.5f, 21.5f));

		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(ReconHelicopterEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.rotor.yaw = animationProgress * 3.0f;
		this.tailRotor.pitch = animationProgress * 3.0f;
		this.body.pitch = headPitch * (float) (Math.PI / 180.0) * 0.3f;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
