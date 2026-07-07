package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.BatteryCenterEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Battery Center: a squat mobile power hull carrying a pair of battery
 * packs and a small cab up front with a coil antenna, all riding on six
 * wheels.
 */
public class BatteryCenterModel extends SinglePartEntityModel<BatteryCenterEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("battery_center"), "main");

	private final ModelPart root;
	private final ModelPart wheelFrontLeft;
	private final ModelPart wheelFrontRight;
	private final ModelPart wheelMiddleLeft;
	private final ModelPart wheelMiddleRight;
	private final ModelPart wheelBackLeft;
	private final ModelPart wheelBackRight;

	public BatteryCenterModel(ModelPart root) {
		this.root = root;
		this.wheelFrontLeft = root.getChild("wheel_fl");
		this.wheelFrontRight = root.getChild("wheel_fr");
		this.wheelMiddleLeft = root.getChild("wheel_ml");
		this.wheelMiddleRight = root.getChild("wheel_mr");
		this.wheelBackLeft = root.getChild("wheel_bl");
		this.wheelBackRight = root.getChild("wheel_br");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-9.0f, -3.0f, -13.0f, 18.0f, 8.0f, 26.0f)
						.uv(0, 35).cuboid(-7.0f, -9.0f, -9.0f, 14.0f, 6.0f, 8.0f)
						.uv(0, 35).cuboid(-7.0f, -9.0f, 1.0f, 14.0f, 6.0f, 8.0f)
						.uv(45, 35).cuboid(-6.0f, -7.0f, -13.0f, 12.0f, 4.0f, 4.0f)
						.uv(78, 35).cuboid(-1.0f, -14.0f, 6.0f, 2.0f, 5.0f, 2.0f),
				ModelTransform.pivot(0.0f, 11.0f, 0.0f));

		ModelPartBuilder wheel = ModelPartBuilder.create()
				.uv(93, 0).cuboid(-2.5f, -2.5f, -2.5f, 5.0f, 5.0f, 5.0f);
		root.addChild("wheel_fl", wheel, ModelTransform.pivot(9.0f, 21.5f, -9.0f));
		root.addChild("wheel_fr", wheel, ModelTransform.pivot(-9.0f, 21.5f, -9.0f));
		root.addChild("wheel_ml", wheel, ModelTransform.pivot(9.0f, 21.5f, 0.0f));
		root.addChild("wheel_mr", wheel, ModelTransform.pivot(-9.0f, 21.5f, 0.0f));
		root.addChild("wheel_bl", wheel, ModelTransform.pivot(9.0f, 21.5f, 9.0f));
		root.addChild("wheel_br", wheel, ModelTransform.pivot(-9.0f, 21.5f, 9.0f));

		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(BatteryCenterEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float roll = limbAngle * 0.6f;
		this.wheelFrontLeft.pitch = roll;
		this.wheelFrontRight.pitch = roll;
		this.wheelMiddleLeft.pitch = roll;
		this.wheelMiddleRight.pitch = roll;
		this.wheelBackLeft.pitch = roll;
		this.wheelBackRight.pitch = roll;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
