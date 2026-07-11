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

		// Detailed chassis: hull + top deck + sloped glacis; running gear (two
		// tracks with five road wheels each, drive sprockets and idlers); side
		// applique armour; standoff cage/slat armour on the front and both
		// sides (open at the rear); headlights, exhausts, tow hooks and a rear
		// stowage box. The turret carries a mantlet, rear bustle rack, commander
		// cupola with a cyan optic, smoke launchers, a coax MG, twin antennas,
		// and a thermal-sleeved autocannon with a muzzle brake. Geometry + UVs
		// generated from the model source so they match the texture exactly.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(134, 0).cuboid(-8.0f, -4.0f, -14.0f, 16.0f, 12.0f, 28.0f)
						.uv(0, 122).cuboid(-7.0f, -6.0f, -11.0f, 14.0f, 2.0f, 21.0f)
						.uv(71, 122).cuboid(-9.0f, -2.0f, -9.0f, 1.0f, 7.0f, 9.0f)
						.uv(92, 122).cuboid(8.0f, -2.0f, -9.0f, 1.0f, 7.0f, 9.0f)
						.uv(146, 122).cuboid(-9.0f, -1.0f, 3.0f, 1.0f, 6.0f, 8.0f)
						.uv(165, 122).cuboid(8.0f, -1.0f, 3.0f, 1.0f, 6.0f, 8.0f)
						.uv(42, 167).cuboid(-8.0f, -4.0f, 14.0f, 16.0f, 11.0f, 1.0f)
						.uv(187, 184).cuboid(-7.0f, -3.0f, 14.0f, 2.0f, 2.0f, 3.0f)
						.uv(198, 184).cuboid(5.0f, -3.0f, 14.0f, 2.0f, 2.0f, 3.0f)
						.uv(36, 197).cuboid(-7.0f, -3.0f, -15.0f, 2.0f, 2.0f, 1.0f)
						.uv(43, 197).cuboid(5.0f, -3.0f, -15.0f, 2.0f, 2.0f, 1.0f)
						.uv(50, 197).cuboid(-6.0f, 2.0f, -15.0f, 1.0f, 1.0f, 1.0f)
						.uv(55, 197).cuboid(5.0f, 2.0f, -15.0f, 1.0f, 1.0f, 1.0f)
						.uv(19, 167).cuboid(-6.0f, -8.0f, 8.0f, 5.0f, 2.0f, 6.0f)
						.uv(0, 0).cuboid(-12.0f, 4.0f, -15.0f, 3.0f, 9.0f, 30.0f)
						.uv(67, 0).cuboid(9.0f, 4.0f, -15.0f, 3.0f, 9.0f, 30.0f)
						.uv(100, 167).cuboid(-13.0f, 8.0f, -13.0f, 4.0f, 4.0f, 4.0f)
						.uv(117, 167).cuboid(9.0f, 8.0f, -13.0f, 4.0f, 4.0f, 4.0f)
						.uv(134, 167).cuboid(-13.0f, 8.0f, -6.0f, 4.0f, 4.0f, 4.0f)
						.uv(151, 167).cuboid(9.0f, 8.0f, -6.0f, 4.0f, 4.0f, 4.0f)
						.uv(168, 167).cuboid(-13.0f, 8.0f, 0.0f, 4.0f, 4.0f, 4.0f)
						.uv(185, 167).cuboid(9.0f, 8.0f, 0.0f, 4.0f, 4.0f, 4.0f)
						.uv(202, 167).cuboid(-13.0f, 8.0f, 6.0f, 4.0f, 4.0f, 4.0f)
						.uv(219, 167).cuboid(9.0f, 8.0f, 6.0f, 4.0f, 4.0f, 4.0f)
						.uv(236, 167).cuboid(-13.0f, 8.0f, 12.0f, 4.0f, 4.0f, 4.0f)
						.uv(0, 184).cuboid(9.0f, 8.0f, 12.0f, 4.0f, 4.0f, 4.0f)
						.uv(17, 184).cuboid(-13.0f, 6.0f, -15.0f, 4.0f, 6.0f, 3.0f)
						.uv(32, 184).cuboid(9.0f, 6.0f, -15.0f, 4.0f, 6.0f, 3.0f)
						.uv(47, 184).cuboid(-13.0f, 6.0f, 12.0f, 4.0f, 6.0f, 3.0f)
						.uv(62, 184).cuboid(9.0f, 6.0f, 12.0f, 4.0f, 6.0f, 3.0f)
						.uv(0, 70).cuboid(-11.0f, -4.0f, -13.0f, 1.0f, 1.0f, 25.0f)
						.uv(53, 70).cuboid(-11.0f, 3.0f, -13.0f, 1.0f, 1.0f, 25.0f)
						.uv(77, 184).cuboid(-11.0f, -4.0f, -12.0f, 1.0f, 8.0f, 1.0f)
						.uv(82, 184).cuboid(-11.0f, -4.0f, -5.0f, 1.0f, 8.0f, 1.0f)
						.uv(87, 184).cuboid(-11.0f, -4.0f, 2.0f, 1.0f, 8.0f, 1.0f)
						.uv(92, 184).cuboid(-11.0f, -4.0f, 10.0f, 1.0f, 8.0f, 1.0f)
						.uv(106, 70).cuboid(10.0f, -4.0f, -13.0f, 1.0f, 1.0f, 25.0f)
						.uv(159, 70).cuboid(10.0f, 3.0f, -13.0f, 1.0f, 1.0f, 25.0f)
						.uv(97, 184).cuboid(10.0f, -4.0f, -12.0f, 1.0f, 8.0f, 1.0f)
						.uv(102, 184).cuboid(10.0f, -4.0f, -5.0f, 1.0f, 8.0f, 1.0f)
						.uv(107, 184).cuboid(10.0f, -4.0f, 2.0f, 1.0f, 8.0f, 1.0f)
						.uv(112, 184).cuboid(10.0f, -4.0f, 10.0f, 1.0f, 8.0f, 1.0f)
						.uv(60, 197).cuboid(-7.0f, -3.0f, -17.0f, 14.0f, 1.0f, 1.0f)
						.uv(91, 197).cuboid(-7.0f, 3.0f, -17.0f, 14.0f, 1.0f, 1.0f)
						.uv(134, 184).cuboid(-6.0f, -3.0f, -17.0f, 1.0f, 7.0f, 1.0f)
						.uv(139, 184).cuboid(-2.0f, -3.0f, -17.0f, 1.0f, 7.0f, 1.0f)
						.uv(144, 184).cuboid(2.0f, -3.0f, -17.0f, 1.0f, 7.0f, 1.0f)
						.uv(149, 184).cuboid(6.0f, -3.0f, -17.0f, 1.0f, 7.0f, 1.0f),
				ModelTransform.pivot(0.0f, 11.0f, 0.0f));

		ModelPartData glacis = body.addChild("glacis",
				ModelPartBuilder.create()
						.uv(184, 122).cuboid(-8.0f, -14.0f, 0.0f, 16.0f, 14.0f, 2.0f),
				ModelTransform.of(0.0f, 8.0f, -14.0f, -0.42f, 0.0f, 0.0f));

		ModelPartData turret = body.addChild("turret",
				ModelPartBuilder.create()
						.uv(113, 122).cuboid(-4.0f, -7.0f, -4.0f, 8.0f, 7.0f, 8.0f)
						.uv(209, 184).cuboid(-4.0f, -6.0f, -6.0f, 8.0f, 4.0f, 2.0f)
						.uv(230, 184).cuboid(-3.0f, -6.0f, -8.0f, 6.0f, 4.0f, 2.0f)
						.uv(77, 167).cuboid(-3.0f, -5.0f, 4.0f, 6.0f, 3.0f, 5.0f)
						.uv(117, 184).cuboid(-2.0f, -9.0f, -1.0f, 4.0f, 2.0f, 4.0f)
						.uv(5, 197).cuboid(-1.0f, -10.0f, 0.0f, 3.0f, 1.0f, 3.0f)
						.uv(18, 197).cuboid(-1.0f, -10.0f, -4.0f, 2.0f, 2.0f, 2.0f)
						.uv(154, 184).cuboid(-5.0f, -5.0f, -4.0f, 2.0f, 3.0f, 3.0f)
						.uv(165, 184).cuboid(3.0f, -5.0f, -4.0f, 2.0f, 3.0f, 3.0f)
						.uv(176, 184).cuboid(2.0f, -4.0f, -7.0f, 1.0f, 1.0f, 4.0f)
						.uv(247, 184).cuboid(-3.0f, -12.0f, 4.0f, 1.0f, 6.0f, 1.0f)
						.uv(0, 197).cuboid(3.0f, -12.0f, 4.0f, 1.0f, 6.0f, 1.0f)
						.uv(0, 167).cuboid(-1.0f, -5.0f, -15.0f, 2.0f, 2.0f, 7.0f)
						.uv(221, 122).cuboid(0.0f, -4.0f, -22.0f, 1.0f, 1.0f, 8.0f)
						.uv(27, 197).cuboid(-1.0f, -5.0f, -23.0f, 2.0f, 2.0f, 2.0f),
				ModelTransform.pivot(0.0f, -6.0f, 2.0f));

		return TexturedModelData.of(modelData, 256, 208);
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
