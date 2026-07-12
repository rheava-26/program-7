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

		// Detailed M109 Paladin: wide tracked hull with full running gear (7
		// road wheels a side, drive sprockets, idlers), applique + stowage, the
		// iconic front-hull travel-lock crutch; a big boxy rear-set turret with
		// a large bustle, commander cupola + .50-cal, smoke launchers, sensors;
		// and a long fume-extractor gun tube on an elevated barrel that
		// traverses with the turret. Generated from the model source (matches
		// the texture) — same detail pass as the IFV and gunboat.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(150, 0).cuboid(-10.0f, -5.0f, -16.0f, 20.0f, 12.0f, 32.0f)
						.uv(57, 79).cuboid(-9.0f, -7.0f, -13.0f, 18.0f, 2.0f, 24.0f)
						.uv(207, 79).cuboid(-11.0f, -2.0f, -11.0f, 1.0f, 7.0f, 11.0f)
						.uv(0, 132).cuboid(10.0f, -2.0f, -11.0f, 1.0f, 7.0f, 11.0f)
						.uv(25, 132).cuboid(-11.0f, -1.0f, 3.0f, 1.0f, 6.0f, 10.0f)
						.uv(48, 132).cuboid(10.0f, -1.0f, 3.0f, 1.0f, 6.0f, 10.0f)
						.uv(51, 162).cuboid(-10.0f, -5.0f, 16.0f, 20.0f, 11.0f, 1.0f)
						.uv(47, 193).cuboid(-8.0f, -3.0f, 16.0f, 3.0f, 3.0f, 3.0f)
						.uv(60, 193).cuboid(5.0f, -3.0f, 16.0f, 3.0f, 3.0f, 3.0f)
						.uv(206, 193).cuboid(-8.0f, -3.0f, -17.0f, 2.0f, 2.0f, 1.0f)
						.uv(213, 193).cuboid(6.0f, -3.0f, -17.0f, 2.0f, 2.0f, 1.0f)
						.uv(190, 132).cuboid(-7.0f, -9.0f, 9.0f, 6.0f, 2.0f, 7.0f)
						.uv(217, 132).cuboid(2.0f, -9.0f, 9.0f, 5.0f, 2.0f, 7.0f)
						.uv(221, 179).cuboid(-1.0f, -12.0f, -14.0f, 2.0f, 7.0f, 2.0f)
						.uv(171, 193).cuboid(-3.0f, -13.0f, -14.0f, 6.0f, 2.0f, 2.0f)
						.uv(0, 0).cuboid(-14.0f, 4.0f, -17.0f, 3.0f, 10.0f, 34.0f)
						.uv(75, 0).cuboid(11.0f, 4.0f, -17.0f, 3.0f, 10.0f, 34.0f)
						.uv(94, 162).cuboid(-15.0f, 8.0f, -15.0f, 4.0f, 5.0f, 4.0f)
						.uv(111, 162).cuboid(11.0f, 8.0f, -15.0f, 4.0f, 5.0f, 4.0f)
						.uv(128, 162).cuboid(-15.0f, 8.0f, -10.0f, 4.0f, 5.0f, 4.0f)
						.uv(145, 162).cuboid(11.0f, 8.0f, -10.0f, 4.0f, 5.0f, 4.0f)
						.uv(162, 162).cuboid(-15.0f, 8.0f, -5.0f, 4.0f, 5.0f, 4.0f)
						.uv(179, 162).cuboid(11.0f, 8.0f, -5.0f, 4.0f, 5.0f, 4.0f)
						.uv(196, 162).cuboid(-15.0f, 8.0f, 0.0f, 4.0f, 5.0f, 4.0f)
						.uv(213, 162).cuboid(11.0f, 8.0f, 0.0f, 4.0f, 5.0f, 4.0f)
						.uv(230, 162).cuboid(-15.0f, 8.0f, 5.0f, 4.0f, 5.0f, 4.0f)
						.uv(0, 179).cuboid(11.0f, 8.0f, 5.0f, 4.0f, 5.0f, 4.0f)
						.uv(17, 179).cuboid(-15.0f, 8.0f, 10.0f, 4.0f, 5.0f, 4.0f)
						.uv(34, 179).cuboid(11.0f, 8.0f, 10.0f, 4.0f, 5.0f, 4.0f)
						.uv(51, 179).cuboid(-15.0f, 8.0f, 14.0f, 4.0f, 5.0f, 4.0f)
						.uv(68, 179).cuboid(11.0f, 8.0f, 14.0f, 4.0f, 5.0f, 4.0f)
						.uv(85, 179).cuboid(-15.0f, 5.0f, -17.0f, 4.0f, 7.0f, 3.0f)
						.uv(100, 179).cuboid(11.0f, 5.0f, -17.0f, 4.0f, 7.0f, 3.0f)
						.uv(115, 179).cuboid(-15.0f, 5.0f, 14.0f, 4.0f, 7.0f, 3.0f)
						.uv(130, 179).cuboid(11.0f, 5.0f, 14.0f, 4.0f, 7.0f, 3.0f),
				ModelTransform.pivot(0.0f, 11.0f, 0.0f));

		body.addChild("glacis",
				ModelPartBuilder.create()
						.uv(145, 132).cuboid(-10.0f, -14.0f, 0.0f, 20.0f, 14.0f, 2.0f),
				ModelTransform.of(0.0f, 9.0f, -16.0f, -0.45f, 0.0f, 0.0f));

		ModelPartData turret = body.addChild("turret",
				ModelPartBuilder.create()
						.uv(142, 79).cuboid(-8.0f, -10.0f, -8.0f, 16.0f, 10.0f, 16.0f)
						.uv(0, 193).cuboid(-8.0f, -9.0f, -10.0f, 16.0f, 6.0f, 2.0f)
						.uv(73, 193).cuboid(-4.0f, -8.0f, -12.0f, 8.0f, 5.0f, 2.0f)
						.uv(104, 132).cuboid(-7.0f, -8.0f, 8.0f, 14.0f, 8.0f, 6.0f)
						.uv(127, 193).cuboid(-7.0f, -9.0f, 12.0f, 14.0f, 6.0f, 1.0f)
						.uv(145, 179).cuboid(-6.0f, -11.0f, -3.0f, 5.0f, 1.0f, 6.0f)
						.uv(187, 179).cuboid(-3.0f, -13.0f, -1.0f, 5.0f, 2.0f, 5.0f)
						.uv(158, 193).cuboid(-2.0f, -14.0f, 0.0f, 3.0f, 1.0f, 3.0f)
						.uv(188, 193).cuboid(-2.0f, -14.0f, -4.0f, 2.0f, 2.0f, 2.0f)
						.uv(208, 179).cuboid(1.0f, -15.0f, -1.0f, 1.0f, 2.0f, 5.0f)
						.uv(94, 193).cuboid(1.0f, -16.0f, -3.0f, 1.0f, 1.0f, 4.0f)
						.uv(197, 193).cuboid(-6.0f, -11.0f, 2.0f, 2.0f, 2.0f, 2.0f)
						.uv(105, 193).cuboid(-9.0f, -8.0f, -2.0f, 2.0f, 3.0f, 3.0f)
						.uv(116, 193).cuboid(7.0f, -8.0f, -2.0f, 2.0f, 3.0f, 3.0f)
						.uv(37, 193).cuboid(-6.0f, -13.0f, 8.0f, 1.0f, 8.0f, 1.0f)
						.uv(42, 193).cuboid(5.0f, -13.0f, 8.0f, 1.0f, 8.0f, 1.0f)
						.uv(0, 162).cuboid(-8.0f, -6.0f, 0.0f, 1.0f, 4.0f, 6.0f)
						.uv(15, 162).cuboid(7.0f, -6.0f, 0.0f, 1.0f, 4.0f, 6.0f),
				ModelTransform.pivot(0.0f, -7.0f, 4.0f));

		turret.addChild("barrel",
				ModelPartBuilder.create()
						.uv(71, 132).cuboid(-3.0f, -3.0f, -8.0f, 6.0f, 6.0f, 10.0f)
						.uv(0, 79).cuboid(-2.0f, -2.0f, -30.0f, 4.0f, 4.0f, 24.0f)
						.uv(30, 162).cuboid(-2.5f, -2.5f, -20.0f, 5.0f, 5.0f, 5.0f)
						.uv(168, 179).cuboid(-2.5f, -2.5f, -33.0f, 5.0f, 5.0f, 4.0f),
				ModelTransform.of(0.0f, -4.0f, -8.0f, -0.4f, 0.0f, 0.0f));

		return TexturedModelData.of(modelData, 256, 208);
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
