package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.IFVEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Tier 3 infantry fighting vehicle: a stepped lower and upper hull
 * carrying a small tracking turret with a long cannon, riding on three
 * pairs of road wheels per side that roll with ground travel.
 */
public class IFVModel extends SinglePartEntityModel<IFVEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("ifv"), "main");

	private final ModelPart root;
	private final ModelPart turret;
	private final ModelPart wheelFrontLeft;
	private final ModelPart wheelFrontRight;
	private final ModelPart wheelMidLeft;
	private final ModelPart wheelMidRight;
	private final ModelPart wheelBackLeft;
	private final ModelPart wheelBackRight;

	public IFVModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		this.turret = body.getChild("turret");
		this.wheelFrontLeft = root.getChild("wheel_fl");
		this.wheelFrontRight = root.getChild("wheel_fr");
		this.wheelMidLeft = root.getChild("wheel_ml");
		this.wheelMidRight = root.getChild("wheel_mr");
		this.wheelBackLeft = root.getChild("wheel_bl");
		this.wheelBackRight = root.getChild("wheel_br");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-9.0f, -4.0f, -14.0f, 18.0f, 9.0f, 28.0f)
						.uv(0, 38).cuboid(-8.0f, -12.0f, -10.0f, 16.0f, 8.0f, 20.0f),
				ModelTransform.pivot(0.0f, 11.0f, 0.0f));

		body.addChild("turret",
				ModelPartBuilder.create()
						.uv(73, 38).cuboid(-5.0f, -6.0f, -5.0f, 10.0f, 6.0f, 10.0f)
						.uv(73, 55).cuboid(-1.0f, -5.0f, -16.0f, 2.0f, 2.0f, 11.0f),
				ModelTransform.pivot(0.0f, -12.0f, -2.0f));

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
	public void setAngles(IFVEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);

		float roll = limbAngle * 0.6f;
		this.wheelFrontLeft.pitch = roll;
		this.wheelFrontRight.pitch = roll;
		this.wheelMidLeft.pitch = roll;
		this.wheelMidRight.pitch = roll;
		this.wheelBackLeft.pitch = roll;
		this.wheelBackRight.pitch = roll;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
