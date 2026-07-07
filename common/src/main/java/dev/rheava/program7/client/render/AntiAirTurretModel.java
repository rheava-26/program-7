package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AntiAirTurretEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The dedicated anti-air emplacement, built on the same v3 open-frame
 * skeleton as the autogun turret: a pad, a tall standing column, and four
 * splayed legs. The head carries twin elevated AA barrels, a rear radar
 * panel, and a pair of exposed ammo boxes riding bare on either flank. The
 * base never moves — only the head pans and tilts to track its target.
 */
public class AntiAirTurretModel extends SinglePartEntityModel<AntiAirTurretEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("anti_air_turret"), "main");

	private final ModelPart root;
	private final ModelPart head;

	public AntiAirTurretModel(ModelPart root) {
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
						.uv(0, 8).cuboid(-1.0f, -12.0f, -1.0f, 2.0f, 11.0f, 2.0f),
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
						.uv(14, 8).cuboid(-2.0f, -1.0f, -2.0f, 4.0f, 2.0f, 4.0f)
						.uv(30, 8).cuboid(-1.5f, -0.75f, -9.0f, 1.0f, 1.0f, 7.0f)
						.uv(30, 8).cuboid(0.5f, -0.75f, -9.0f, 1.0f, 1.0f, 7.0f)
						.uv(46, 8).cuboid(-2.0f, -4.0f, 0.5f, 4.0f, 3.0f, 1.0f)
						.uv(0, 24).cuboid(2.0f, -1.0f, -2.0f, 2.0f, 3.0f, 3.0f)
						.uv(0, 24).cuboid(-4.0f, -1.0f, -2.0f, 2.0f, 3.0f, 3.0f),
				ModelTransform.pivot(0.0f, -13.0f, 0.0f));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(AntiAirTurretEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.head.yaw = headYaw * (float) (Math.PI / 180.0);
		this.head.pitch = headPitch * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
