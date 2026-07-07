package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AutogunTurretEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Tier 1 fixed defense: a twin-barrel autogun bolted onto a pedestal
 * base. The base never moves — only the head pans and tilts to track
 * whatever it's shooting at.
 */
public class AutogunTurretModel extends SinglePartEntityModel<AutogunTurretEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("autogun_turret"), "main");

	private final ModelPart root;
	private final ModelPart head;

	public AutogunTurretModel(ModelPart root) {
		this.root = root;
		ModelPart base = root.getChild("base");
		this.head = base.getChild("head");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData base = root.addChild("base",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -2.0f, -4.0f, 8.0f, 2.0f, 8.0f)
						.uv(0, 11).cuboid(-2.0f, -8.0f, -2.0f, 4.0f, 6.0f, 4.0f),
				ModelTransform.pivot(0.0f, 24.0f, 0.0f));

		base.addChild("head",
				ModelPartBuilder.create()
						.uv(17, 11).cuboid(-3.0f, -2.0f, -3.0f, 6.0f, 4.0f, 6.0f)
						.uv(42, 11).cuboid(-2.0f, -1.0f, -9.0f, 1.0f, 1.0f, 6.0f)
						.uv(42, 11).cuboid(1.0f, -1.0f, -9.0f, 1.0f, 1.0f, 6.0f)
						.uv(42, 19).cuboid(-1.0f, -3.0f, -1.0f, 2.0f, 1.0f, 2.0f)
						.uv(33, 22).cuboid(3.0f, -1.5f, -1.0f, 2.0f, 3.0f, 4.0f),
				ModelTransform.pivot(0.0f, -8.0f, 0.0f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(AutogunTurretEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.head.yaw = headYaw * (float) (Math.PI / 180.0);
		this.head.pitch = headPitch * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
