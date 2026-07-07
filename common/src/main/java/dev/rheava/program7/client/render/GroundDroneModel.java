package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GroundDroneEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Tier 1 perimeter unit: a low armored hull riding on angled side
 * skirts, four fat wheels, and a small turreted gun on its back that tracks
 * whatever it's shooting at.
 */
public class GroundDroneModel extends SinglePartEntityModel<GroundDroneEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("ground_drone"), "main");

	private final ModelPart root;
	private final ModelPart turret;
	private final ModelPart wheelFrontLeft;
	private final ModelPart wheelFrontRight;
	private final ModelPart wheelBackLeft;
	private final ModelPart wheelBackRight;

	public GroundDroneModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		this.turret = body.getChild("turret");
		this.wheelFrontLeft = root.getChild("wheel_fl");
		this.wheelFrontRight = root.getChild("wheel_fr");
		this.wheelBackLeft = root.getChild("wheel_bl");
		this.wheelBackRight = root.getChild("wheel_br");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.5f, -2.0f, -6.5f, 9.0f, 4.0f, 13.0f)
						.uv(24, 25).cuboid(-2.0f, -1.5f, -7.0f, 4.0f, 1.0f, 1.0f),
				ModelTransform.pivot(0.0f, 19.0f, 0.0f));

		body.addChild("left_skirt",
				ModelPartBuilder.create()
						.uv(0, 17).cuboid(-0.5f, -1.5f, -5.5f, 1.0f, 3.0f, 11.0f),
				ModelTransform.of(4.5f, 0.5f, 0.0f, 0.0f, 0.0f, -0.20f));
		body.addChild("right_skirt",
				ModelPartBuilder.create()
						.uv(0, 17).cuboid(-0.5f, -1.5f, -5.5f, 1.0f, 3.0f, 11.0f),
				ModelTransform.of(-4.5f, 0.5f, 0.0f, 0.0f, 0.0f, 0.20f));

		body.addChild("turret",
				ModelPartBuilder.create()
						.uv(24, 17).cuboid(-2.5f, -3.0f, -2.5f, 5.0f, 3.0f, 5.0f)
						.uv(44, 17).cuboid(-0.5f, -2.5f, -7.5f, 1.0f, 1.0f, 5.0f),
				ModelTransform.pivot(0.0f, -2.0f, -1.0f));

		ModelPartBuilder wheel = ModelPartBuilder.create()
				.uv(46, 25).cuboid(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f);
		root.addChild("wheel_fl", wheel, ModelTransform.pivot(4.5f, 22.5f, -4.0f));
		root.addChild("wheel_fr", wheel, ModelTransform.pivot(-4.5f, 22.5f, -4.0f));
		root.addChild("wheel_bl", wheel, ModelTransform.pivot(4.5f, 22.5f, 4.0f));
		root.addChild("wheel_br", wheel, ModelTransform.pivot(-4.5f, 22.5f, 4.0f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(GroundDroneEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);

		float roll = limbAngle * 0.8f;
		this.wheelFrontLeft.pitch = roll;
		this.wheelFrontRight.pitch = roll;
		this.wheelBackLeft.pitch = roll;
		this.wheelBackRight.pitch = roll;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
