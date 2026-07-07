package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The lovable idiot: an armored shoebox on four fat wheels with a cargo
 * hopper on its back and a small sensor strip up front. Wheels roll with
 * travel distance.
 */
public class HarvesterDroneModel extends SinglePartEntityModel<HarvesterDroneEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("harvester_drone"), "main");

	private final ModelPart root;
	private final ModelPart wheelFrontLeft;
	private final ModelPart wheelFrontRight;
	private final ModelPart wheelBackLeft;
	private final ModelPart wheelBackRight;

	public HarvesterDroneModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		this.wheelFrontLeft = body.getChild("wheel_fl");
		this.wheelFrontRight = body.getChild("wheel_fr");
		this.wheelBackLeft = body.getChild("wheel_bl");
		this.wheelBackRight = body.getChild("wheel_br");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-5.0f, -2.5f, -4.0f, 10.0f, 5.0f, 8.0f)
						.uv(0, 14).cuboid(-2.0f, -1.0f, -5.0f, 4.0f, 2.0f, 1.0f)
						.uv(0, 18).cuboid(-3.0f, -4.5f, -2.0f, 6.0f, 2.0f, 4.0f),
				ModelTransform.pivot(0.0f, 19.5f, 0.0f));

		ModelPartBuilder wheel = ModelPartBuilder.create()
				.uv(37, 0).cuboid(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f);
		body.addChild("wheel_fl", wheel, ModelTransform.pivot(-4.0f, 3.5f, -2.5f));
		body.addChild("wheel_fr", wheel, ModelTransform.pivot(4.0f, 3.5f, -2.5f));
		body.addChild("wheel_bl", wheel, ModelTransform.pivot(-4.0f, 3.5f, 2.5f));
		body.addChild("wheel_br", wheel, ModelTransform.pivot(4.0f, 3.5f, 2.5f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(HarvesterDroneEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
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
