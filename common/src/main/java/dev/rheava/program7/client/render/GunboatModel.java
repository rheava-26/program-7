package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunboatEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * The Tier 3 waterborne gunboat: a long fore/aft hull, a central deckhouse
 * with a mast and stack, and a bow-mounted twin-barrel turret that tracks
 * its target. The whole hull sways gently at sea.
 */
public class GunboatModel extends SinglePartEntityModel<GunboatEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("gunboat"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart turret;

	public GunboatModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.turret = this.body.getChild("turret");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-10.0f, -4.0f, -24.0f, 20.0f, 8.0f, 24.0f)
						.uv(0, 0).cuboid(-10.0f, -4.0f, 0.0f, 20.0f, 8.0f, 24.0f)
						.uv(0, 33).cuboid(-6.0f, -10.0f, -6.0f, 12.0f, 6.0f, 16.0f)
						.uv(90, 33).cuboid(-1.0f, -16.0f, 2.0f, 2.0f, 6.0f, 2.0f)
						.uv(90, 44).cuboid(-2.0f, -14.0f, 8.0f, 4.0f, 5.0f, 4.0f),
				ModelTransform.pivot(0.0f, 20.0f, 0.0f));

		body.addChild("turret",
				ModelPartBuilder.create()
						.uv(57, 33).cuboid(-4.0f, -4.0f, -4.0f, 8.0f, 4.0f, 8.0f)
						.uv(57, 46).cuboid(-2.0f, -3.0f, -12.0f, 1.0f, 1.0f, 8.0f)
						.uv(57, 46).cuboid(1.0f, -3.0f, -12.0f, 1.0f, 1.0f, 8.0f),
				ModelTransform.pivot(0.0f, -4.0f, -16.0f));

		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(GunboatEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);
		this.body.roll = MathHelper.sin(animationProgress * 0.06f) * 0.02f;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
