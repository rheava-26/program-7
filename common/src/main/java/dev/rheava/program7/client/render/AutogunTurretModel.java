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
 * The Tier 1 fixed defense, v3: a taller open skeletal frame standing on
 * four splayed legs, with the autogun's mechanism left fully exposed —
 * receiver, barrel, and ammo drum all riding bare on the yoke. Light
 * turrets don't waste material armoring themselves. The base never moves —
 * only the head pans and tilts to track whatever it's shooting at.
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
						.uv(0, 0).cuboid(-3.0f, -1.0f, -3.0f, 6.0f, 1.0f, 6.0f)
						.uv(0, 8).cuboid(-1.0f, -10.0f, -1.0f, 2.0f, 9.0f, 2.0f),
				ModelTransform.pivot(0.0f, 24.0f, 0.0f));

		// Four legs splayed outward-down from a single point onto the pad.
		for (int i = 0; i < 4; i++) {
			float legYaw = (float) (Math.PI / 4.0 + i * Math.PI / 2.0);
			base.addChild("leg" + i,
					ModelPartBuilder.create()
							.uv(9, 8).cuboid(-0.5f, 0.0f, -0.5f, 1.0f, 7.0f, 1.0f),
					ModelTransform.of(0.0f, -8.0f, 0.0f, 0.40f, legYaw, 0.0f));
		}

		base.addChild("head",
				ModelPartBuilder.create()
						.uv(14, 8).cuboid(-2.5f, -1.5f, -1.0f, 1.0f, 3.0f, 2.0f)
						.uv(14, 8).cuboid(1.5f, -1.5f, -1.0f, 1.0f, 3.0f, 2.0f)
						.uv(20, 8).cuboid(-1.5f, -1.5f, -4.5f, 3.0f, 3.0f, 7.0f)
						.uv(40, 8).cuboid(-0.5f, -0.5f, -10.5f, 1.0f, 1.0f, 6.0f)
						.uv(40, 16).cuboid(-1.0f, -1.0f, -11.5f, 2.0f, 2.0f, 1.0f)
						.uv(46, 16).cuboid(1.5f, -0.5f, -2.0f, 2.0f, 4.0f, 3.0f)
						.uv(0, 20).cuboid(-1.0f, -3.5f, -2.5f, 2.0f, 1.0f, 2.0f),
				ModelTransform.pivot(0.0f, -11.0f, 0.0f));

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
