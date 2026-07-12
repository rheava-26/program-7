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

		// Detailed hull: flared two-tier hull with a chine, raked bow, raised
		// forecastle, fantail stern, gunwale + spray rails, deck railings;
		// superstructure with a windowed bridge, funnel, lattice radar mast;
		// aft VLS + CIWS, life rafts, deck crates; and port/starboard secondary
		// point-defense turrets. Generated from the model source (matches the
		// texture). The bow is a separate raked part; the bow gun turret tracks.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(111, 114).cuboid(-7.0f, 0.0f, -24.0f, 14.0f, 4.0f, 52.0f)
						.uv(0, 0).cuboid(-9.0f, -5.0f, -26.0f, 18.0f, 5.0f, 54.0f)
						.uv(0, 225).cuboid(-9.0f, -6.0f, -24.0f, 18.0f, 2.0f, 50.0f)
						.uv(148, 430).cuboid(-7.0f, -8.0f, -26.0f, 14.0f, 2.0f, 12.0f)
						.uv(0, 542).cuboid(-8.0f, -5.0f, 28.0f, 16.0f, 6.0f, 3.0f)
						.uv(152, 542).cuboid(-6.0f, -5.0f, 31.0f, 12.0f, 5.0f, 2.0f)
						.uv(145, 0).cuboid(-10.0f, -6.0f, -24.0f, 1.0f, 2.0f, 54.0f)
						.uv(0, 114).cuboid(9.0f, -6.0f, -24.0f, 1.0f, 2.0f, 54.0f)
						.uv(137, 225).cuboid(-10.0f, -1.0f, -24.0f, 1.0f, 1.0f, 50.0f)
						.uv(0, 328).cuboid(9.0f, -1.0f, -24.0f, 1.0f, 1.0f, 50.0f)
						.uv(103, 328).cuboid(-9.0f, -9.0f, -20.0f, 1.0f, 1.0f, 44.0f)
						.uv(32, 555).cuboid(-9.0f, -9.0f, -18.0f, 1.0f, 3.0f, 1.0f)
						.uv(37, 555).cuboid(-9.0f, -9.0f, -10.0f, 1.0f, 3.0f, 1.0f)
						.uv(42, 555).cuboid(-9.0f, -9.0f, -2.0f, 1.0f, 3.0f, 1.0f)
						.uv(47, 555).cuboid(-9.0f, -9.0f, 6.0f, 1.0f, 3.0f, 1.0f)
						.uv(52, 555).cuboid(-9.0f, -9.0f, 14.0f, 1.0f, 3.0f, 1.0f)
						.uv(57, 555).cuboid(-9.0f, -9.0f, 22.0f, 1.0f, 3.0f, 1.0f)
						.uv(0, 430).cuboid(8.0f, -9.0f, -20.0f, 1.0f, 1.0f, 44.0f)
						.uv(62, 555).cuboid(8.0f, -9.0f, -18.0f, 1.0f, 3.0f, 1.0f)
						.uv(67, 555).cuboid(8.0f, -9.0f, -10.0f, 1.0f, 3.0f, 1.0f)
						.uv(72, 555).cuboid(8.0f, -9.0f, -2.0f, 1.0f, 3.0f, 1.0f)
						.uv(77, 555).cuboid(8.0f, -9.0f, 6.0f, 1.0f, 3.0f, 1.0f)
						.uv(82, 555).cuboid(8.0f, -9.0f, 14.0f, 1.0f, 3.0f, 1.0f)
						.uv(87, 555).cuboid(8.0f, -9.0f, 22.0f, 1.0f, 3.0f, 1.0f)
						.uv(91, 430).cuboid(-6.0f, -12.0f, -6.0f, 12.0f, 6.0f, 16.0f)
						.uv(0, 555).cuboid(-6.0f, -11.0f, -7.0f, 12.0f, 4.0f, 1.0f)
						.uv(79, 520).cuboid(-4.0f, -15.0f, -4.0f, 8.0f, 3.0f, 8.0f)
						.uv(168, 520).cuboid(-8.0f, -11.0f, -4.0f, 2.0f, 2.0f, 6.0f)
						.uv(185, 520).cuboid(6.0f, -11.0f, -4.0f, 2.0f, 2.0f, 6.0f)
						.uv(145, 520).cuboid(-3.0f, -17.0f, 6.0f, 6.0f, 6.0f, 5.0f)
						.uv(39, 542).cuboid(-4.0f, -18.0f, 6.0f, 8.0f, 1.0f, 5.0f)
						.uv(203, 542).cuboid(-2.0f, -19.0f, 7.0f, 4.0f, 1.0f, 3.0f)
						.uv(96, 542).cuboid(-1.0f, -21.0f, -4.0f, 2.0f, 6.0f, 2.0f)
						.uv(130, 555).cuboid(-4.0f, -19.0f, -4.0f, 8.0f, 1.0f, 1.0f)
						.uv(92, 555).cuboid(-3.0f, -23.0f, -5.0f, 6.0f, 3.0f, 1.0f)
						.uv(27, 555).cuboid(-1.0f, -25.0f, -3.0f, 1.0f, 4.0f, 1.0f)
						.uv(149, 555).cuboid(-1.0f, -22.0f, -5.0f, 1.0f, 1.0f, 1.0f)
						.uv(112, 520).cuboid(-4.0f, -8.0f, 16.0f, 8.0f, 3.0f, 8.0f)
						.uv(105, 542).cuboid(-2.0f, -9.0f, 14.0f, 4.0f, 2.0f, 4.0f)
						.uv(218, 542).cuboid(-1.0f, -9.0f, 11.0f, 1.0f, 1.0f, 3.0f)
						.uv(122, 542).cuboid(-8.0f, -7.0f, 24.0f, 3.0f, 2.0f, 4.0f)
						.uv(137, 542).cuboid(5.0f, -7.0f, 24.0f, 3.0f, 2.0f, 4.0f)
						.uv(181, 542).cuboid(-9.0f, -7.0f, -6.0f, 2.0f, 2.0f, 3.0f)
						.uv(192, 542).cuboid(7.0f, -7.0f, -6.0f, 2.0f, 2.0f, 3.0f)
						.uv(66, 542).cuboid(-9.0f, -8.0f, 0.0f, 3.0f, 3.0f, 4.0f)
						.uv(154, 555).cuboid(-12.0f, -7.0f, 1.0f, 3.0f, 1.0f, 1.0f)
						.uv(163, 555).cuboid(-12.0f, -7.0f, 3.0f, 3.0f, 1.0f, 1.0f)
						.uv(172, 555).cuboid(-8.0f, -9.0f, 1.0f, 1.0f, 1.0f, 1.0f)
						.uv(81, 542).cuboid(6.0f, -8.0f, 0.0f, 3.0f, 3.0f, 4.0f)
						.uv(177, 555).cuboid(9.0f, -7.0f, 1.0f, 3.0f, 1.0f, 1.0f)
						.uv(186, 555).cuboid(9.0f, -7.0f, 3.0f, 3.0f, 1.0f, 1.0f)
						.uv(195, 555).cuboid(7.0f, -9.0f, 1.0f, 1.0f, 1.0f, 1.0f),
				ModelTransform.pivot(0.0f, 20.0f, 0.0f));

		body.addChild("bow",
				ModelPartBuilder.create()
						.uv(201, 430).cuboid(-6.0f, -5.0f, -8.0f, 12.0f, 6.0f, 8.0f)
						.uv(202, 520).cuboid(-3.0f, -5.0f, -12.0f, 6.0f, 5.0f, 4.0f),
				ModelTransform.of(0.0f, -2.0f, -24.0f, 0.34f, 0.0f, 0.0f));

		body.addChild("turret",
				ModelPartBuilder.create()
						.uv(46, 520).cuboid(-4.0f, -4.0f, -4.0f, 8.0f, 4.0f, 8.0f)
						.uv(227, 542).cuboid(-3.0f, -4.0f, -6.0f, 6.0f, 3.0f, 2.0f)
						.uv(0, 520).cuboid(-2.0f, -3.0f, -14.0f, 1.0f, 1.0f, 10.0f)
						.uv(23, 520).cuboid(1.0f, -3.0f, -14.0f, 1.0f, 1.0f, 10.0f)
						.uv(116, 555).cuboid(-2.0f, -4.0f, -15.0f, 2.0f, 2.0f, 1.0f)
						.uv(123, 555).cuboid(1.0f, -4.0f, -15.0f, 2.0f, 2.0f, 1.0f)
						.uv(107, 555).cuboid(-1.0f, -5.0f, -3.0f, 2.0f, 1.0f, 2.0f),
				ModelTransform.pivot(0.0f, -7.0f, -15.0f));

		return TexturedModelData.of(modelData, 256, 568);
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
