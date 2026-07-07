package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AirUAVEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Air UAV, v2: a modern military drone silhouette — long slim fuselage
 * with a bulged satcom nose, one high-aspect slender wing, a V-tail, and a
 * pusher propeller at the rear. The sensor ball hangs under the chin. Sleek,
 * quiet, and unmistakable against the sky.
 */
public class AirUAVModel extends SinglePartEntityModel<AirUAVEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("air_uav"), "main");

	private final ModelPart root;
	private final ModelPart propeller;

	public AirUAVModel(ModelPart root) {
		this.root = root;
		this.propeller = root.getChild("body").getChild("propeller");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-1.5f, -1.5f, -9.0f, 3.0f, 3.0f, 19.0f)
						.uv(0, 23).cuboid(-2.0f, -3.5f, -9.0f, 4.0f, 2.0f, 4.0f)
						.uv(0, 30).cuboid(-14.0f, -0.5f, -1.0f, 28.0f, 1.0f, 3.0f)
						.uv(44, 23).cuboid(-1.0f, 1.5f, -7.0f, 2.0f, 1.0f, 2.0f),
				ModelTransform.pivot(0.0f, 14.0f, 0.0f));

		// V-tail: two fins leaning outward from the tail cone.
		body.addChild("left_tail_fin",
				ModelPartBuilder.create()
						.uv(20, 23).cuboid(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f),
				ModelTransform.of(1.0f, 0.0f, 8.5f, 0.0f, 0.0f, -0.6f));
		body.addChild("right_tail_fin",
				ModelPartBuilder.create()
						.uv(20, 23).cuboid(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f),
				ModelTransform.of(-1.0f, 0.0f, 8.5f, 0.0f, 0.0f, 0.6f));

		// Pusher prop on the tail, not the nose — modern drones shove, not pull.
		body.addChild("propeller",
				ModelPartBuilder.create()
						.uv(30, 23).cuboid(-2.5f, -2.5f, -0.5f, 5.0f, 5.0f, 1.0f),
				ModelTransform.pivot(0.0f, 0.0f, 10.0f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(AirUAVEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.propeller.roll = animationProgress * 3.0f;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
