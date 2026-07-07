package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MortarEmplacementEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Tier 1 indirect-fire emplacement: a fixed ground plate carrying a
 * traversing carriage with a high-angle tube, bipod struts, and a small
 * shell rack for the loader. The plate never moves — the whole carriage
 * pans to face the target.
 */
public class MortarEmplacementModel extends SinglePartEntityModel<MortarEmplacementEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("mortar_emplacement"), "main");

	private final ModelPart root;
	private final ModelPart carriage;

	public MortarEmplacementModel(ModelPart root) {
		this.root = root;
		ModelPart base = root.getChild("base");
		this.carriage = base.getChild("carriage");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData base = root.addChild("base",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-3.5f, -1.0f, -3.5f, 7.0f, 1.0f, 7.0f),
				ModelTransform.pivot(0.0f, 24.0f, 0.0f));

		ModelPartData carriage = base.addChild("carriage",
				ModelPartBuilder.create()
						.uv(18, 10).cuboid(2.5f, -2.0f, -2.0f, 2.0f, 2.0f, 4.0f)
						.uv(31, 10).cuboid(3.0f, -4.0f, -1.0f, 1.0f, 2.0f, 1.0f)
						.uv(36, 10).cuboid(-3.0f, -1.0f, 2.0f, 2.0f, 1.0f, 2.0f),
				ModelTransform.pivot(0.0f, -1.0f, 0.0f));

		carriage.addChild("tube",
				ModelPartBuilder.create()
						.uv(0, 10).cuboid(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f),
				ModelTransform.of(0.0f, 0.0f, 1.0f, -0.5f, 0.0f, 0.0f));

		// Struts lean back toward the tube, bracing it like a bipod.
		carriage.addChild("strut_left",
				ModelPartBuilder.create()
						.uv(13, 10).cuboid(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f),
				ModelTransform.of(1.5f, 0.0f, -1.5f, -0.35f, 0.0f, 0.0f));
		carriage.addChild("strut_right",
				ModelPartBuilder.create()
						.uv(13, 10).cuboid(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f),
				ModelTransform.of(-1.5f, 0.0f, -1.5f, -0.35f, 0.0f, 0.0f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(MortarEmplacementEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.carriage.yaw = headYaw * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
