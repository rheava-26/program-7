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
 * The Tier 3 infantry fighting vehicle: a big, dominant armored chassis
 * with a sloped glacis, riding on two continuous tracks that sit flush to
 * the hull. What makes it an IFV and not an APC is the small tracking
 * turret — modest next to all that hull — carrying a thin, high-velocity
 * autocannon rather than a big main gun.
 */
public class IFVModel extends SinglePartEntityModel<IFVEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("ifv"), "main");

	private final ModelPart root;
	private final ModelPart turret;

	public IFVModel(ModelPart root) {
		this.root = root;
		this.turret = root.getChild("body").getChild("turret");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// The chassis is the mass of the vehicle: one big hull plus two track
		// runs that reach the ground flush against it — no floating wheels.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-8.0f, -6.0f, -14.0f, 16.0f, 14.0f, 28.0f)
						.uv(0, 44).cuboid(8.0f, 4.0f, -15.0f, 3.0f, 9.0f, 30.0f)
						.uv(0, 44).cuboid(-11.0f, 4.0f, -15.0f, 3.0f, 9.0f, 30.0f),
				ModelTransform.pivot(0.0f, 11.0f, 0.0f));

		// Sloped frontal glacis — leans up and back from the lower front.
		body.addChild("glacis",
				ModelPartBuilder.create()
						.uv(0, 90).cuboid(-8.0f, -14.0f, 0.0f, 16.0f, 14.0f, 2.0f),
				ModelTransform.of(0.0f, 8.0f, -14.0f, -0.42f, 0.0f, 0.0f));

		// Small turret, offset slightly to the rear, with a thin autocannon
		// and a commander's sight — deliberately dwarfed by the chassis.
		ModelPartData turret = body.addChild("turret",
				ModelPartBuilder.create()
						.uv(90, 0).cuboid(-4.0f, -7.0f, -4.0f, 8.0f, 7.0f, 8.0f)
						.uv(90, 44).cuboid(-0.5f, -5.0f, -22.0f, 1.0f, 1.0f, 16.0f),
				ModelTransform.pivot(0.0f, -6.0f, 2.0f));
		turret.addChild("sight",
				ModelPartBuilder.create()
						.uv(90, 64).cuboid(-1.0f, -9.0f, -1.0f, 2.0f, 2.0f, 3.0f),
				ModelTransform.pivot(0.0f, 0.0f, 0.0f));

		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(IFVEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
