package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HowitzerEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The Tier 3 self-propelled howitzer: the M109 Paladin blockout. Where the
 * {@link dev.rheava.program7.client.render.IFVModel IFV} is a dominant hull
 * with a small tracking turret, this reads turret-forward — a wide tracked
 * hull carrying a large boxy fighting compartment, with a long gun barrel
 * projecting up and forward from the turret front to a muzzle brake at the
 * tip. The tube is the point of the whole vehicle.
 */
public class HowitzerModel extends SinglePartEntityModel<HowitzerEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("howitzer"), "main");

	private final ModelPart root;
	private final ModelPart turret;

	public HowitzerModel(ModelPart root) {
		this.root = root;
		this.turret = root.getChild("body").getChild("turret");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// Wide boxy hull riding on two continuous side tracks that run the
		// hull's full length, flush to the sides — same "no floating wheels"
		// chassis language as the IFV.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-9.0f, -5.0f, -13.0f, 18.0f, 10.0f, 26.0f)
						.uv(0, 40).cuboid(9.0f, 3.0f, -14.0f, 3.0f, 9.0f, 28.0f)
						.uv(0, 40).cuboid(-12.0f, 3.0f, -14.0f, 3.0f, 9.0f, 28.0f),
				ModelTransform.pivot(0.0f, 11.0f, 0.0f));

		// Large boxy turret/fighting compartment, centered on the hull and set
		// slightly to the rear — the M109's silhouette, dwarfing the IFV's
		// modest tracking turret.
		ModelPartData turret = body.addChild("turret",
				ModelPartBuilder.create()
						.uv(0, 80).cuboid(-7.0f, -9.0f, -7.0f, 14.0f, 9.0f, 15.0f),
				ModelTransform.pivot(0.0f, -5.0f, 3.0f));

		// Long gun barrel projecting up and forward from the turret front,
		// angled skyward for the heavy arcing indirect fire this unit throws.
		ModelPartData barrel = turret.addChild("barrel",
				ModelPartBuilder.create()
						.uv(0, 106).cuboid(-1.25f, -1.25f, -24.0f, 2.5f, 2.5f, 24.0f),
				ModelTransform.of(0.0f, -3.0f, -6.0f, -0.5f, 0.0f, 0.0f));

		// Muzzle brake at the tip.
		barrel.addChild("muzzleBrake",
				ModelPartBuilder.create()
						.uv(58, 106).cuboid(-1.75f, -1.75f, -1.75f, 3.5f, 3.5f, 3.5f),
				ModelTransform.pivot(0.0f, 0.0f, -24.0f));

		return TexturedModelData.of(modelData, 128, 144);
	}

	@Override
	public void setAngles(HowitzerEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		// Turret (and the barrel/muzzle brake riding on it) traverses onto the
		// target; elevation stays fixed for this pass, same simplification as
		// the IFV's autocannon.
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
