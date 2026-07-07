package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.WheeledHaulerEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The logistics hauler: a flatbed on four fat wheels carrying a strapped-down
 * cargo crate and a small forward sensor strip. Wheels roll with travel
 * distance, same as the other wheeled units.
 */
public class WheeledHaulerModel extends SinglePartEntityModel<WheeledHaulerEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("wheeled_hauler"), "main");

	private final ModelPart root;
	private final ModelPart wheelFrontLeft;
	private final ModelPart wheelFrontRight;
	private final ModelPart wheelBackLeft;
	private final ModelPart wheelBackRight;

	public WheeledHaulerModel(ModelPart root) {
		this.root = root;
		this.wheelFrontLeft = root.getChild("wheel_fl");
		this.wheelFrontRight = root.getChild("wheel_fr");
		this.wheelBackLeft = root.getChild("wheel_bl");
		this.wheelBackRight = root.getChild("wheel_br");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -2.0f, -5.0f, 8.0f, 4.0f, 10.0f)
						.uv(0, 15).cuboid(-2.5f, -6.0f, -1.0f, 5.0f, 4.0f, 5.0f)
						.uv(21, 15).cuboid(-2.0f, -1.0f, -5.5f, 4.0f, 2.0f, 1.0f),
				ModelTransform.pivot(0.0f, 18.5f, 0.0f));

		// Fat road wheels, same treatment as the other wheeled units.
		ModelPartBuilder wheel = ModelPartBuilder.create()
				.uv(37, 0).cuboid(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f);
		root.addChild("wheel_fl", wheel, ModelTransform.pivot(4.0f, 21.5f, -3.5f));
		root.addChild("wheel_fr", wheel, ModelTransform.pivot(-4.0f, 21.5f, -3.5f));
		root.addChild("wheel_bl", wheel, ModelTransform.pivot(4.0f, 21.5f, 3.5f));
		root.addChild("wheel_br", wheel, ModelTransform.pivot(-4.0f, 21.5f, 3.5f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(WheeledHaulerEntity entity, float limbAngle, float limbDistance,
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
