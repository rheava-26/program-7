package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunshipEntity;
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
 * Placeholder blockout for the Tier 4 apex gunship. Silhouette per
 * {@code UNITS.md}'s Tier 4 entry: a fat Osprey-derived fuselage, but instead
 * of wings, two big metal-shrouded (ducted) rotors on short stub pylons,
 * plus a helicopter-style tail rotor at the rear, plus a belly-mounted
 * autocannon turret. This is a first-pass blockout (bigger cuboids, same
 * construction pattern as {@link HeavyAttackDroneModel}) so the unit reads
 * as the apex and doesn't crash — a real sculpted model is a later art pass.
 */
public class GunshipModel extends SinglePartEntityModel<GunshipEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("gunship"), "main");

	// The heaviest airframe in the roster (mass 8.0 in InertialFlightMoveControl,
	// half the yaw-rate cap of the heavy attack drone) — it still needs to read
	// as banking, so the bank gain is pushed up even further than the heavy
	// drone's, while the max bank angle stays lower: it leans hard into a turn
	// but never snaps, per AIR_DOCTRINE.md's "weighty, never twitchy."
	private static final float BANK_GAIN = 2.4f;
	private static final float MAX_BANK_DEGREES = 18.0f;
	private static final float CLIMB_PITCH_GAIN = 30.0f;
	private static final float MAX_CLIMB_PITCH_DEGREES = 10.0f;

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart leftRotor;
	private final ModelPart rightRotor;
	private final ModelPart tailRotor;

	public GunshipModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.leftRotor = this.body.getChild("left_pylon").getChild("left_housing").getChild("left_rotor");
		this.rightRotor = this.body.getChild("right_pylon").getChild("right_housing").getChild("right_rotor");
		this.tailRotor = this.body.getChild("tail_boom").getChild("tail_housing").getChild("tail_rotor");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// Fat central fuselage (Osprey-derived hull), a nose sensor cluster,
		// and a ventral spine — all one part, same pattern as the heavy
		// attack drone's multi-cuboid body.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-17.0f, -7.0f, -20.0f, 34.0f, 14.0f, 40.0f)
						.uv(0, 70).cuboid(-6.0f, -3.0f, -26.0f, 12.0f, 6.0f, 6.0f)
						.uv(30, 70).cuboid(-2.0f, -9.0f, -12.0f, 4.0f, 2.0f, 16.0f),
				ModelTransform.pivot(0.0f, 10.0f, 0.0f));

		// Belly-mounted autocannon turret + barrel, hung underneath, forward.
		body.addChild("belly_turret",
				ModelPartBuilder.create()
						.uv(60, 70).cuboid(-3.0f, -2.0f, -5.0f, 6.0f, 4.0f, 10.0f)
						.uv(60, 84).cuboid(-1.0f, -1.0f, -9.0f, 2.0f, 2.0f, 4.0f),
				ModelTransform.pivot(0.0f, 7.0f, -10.0f));

		addDuctedRotor(body, "left_pylon", "left_housing", "left_rotor", -17.0f, -14.0f);
		addDuctedRotor(body, "right_pylon", "right_housing", "right_rotor", 17.0f, 14.0f);

		// Tail boom + helicopter-style tail rotor housing at the rear.
		ModelPartData tailBoom = body.addChild("tail_boom",
				ModelPartBuilder.create()
						.uv(0, 96).cuboid(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 16.0f),
				ModelTransform.pivot(0.0f, 0.0f, 20.0f));
		ModelPartData tailHousing = tailBoom.addChild("tail_housing",
				ModelPartBuilder.create()
						.uv(40, 96).cuboid(-4.0f, -4.0f, -2.0f, 8.0f, 8.0f, 4.0f),
				ModelTransform.pivot(0.0f, -2.0f, 16.0f));
		tailHousing.addChild("tail_rotor",
				ModelPartBuilder.create().uv(70, 96).cuboid(-6.0f, -0.5f, -0.5f, 12.0f, 1.0f, 1.0f),
				ModelTransform.pivot(0.0f, 0.0f, 2.0f));

		return TexturedModelData.of(modelData, 128, 128);
	}

	/**
	 * One "big metal-shrouded (ducted) rotor on a short stub pylon" — a stub
	 * strut off the fuselage side carrying an oversized ring housing with a
	 * spinning disc inside it. {@code pylonPivotX} is where the stub attaches
	 * to the fuselage; {@code pylonSpanX} is how far it (and the housing) sit
	 * out from that attachment point, signed to point away from the body.
	 */
	private static void addDuctedRotor(ModelPartData body, String pylonName, String housingName,
			String rotorName, float pylonPivotX, float pylonSpanX) {
		ModelPartData pylon = body.addChild(pylonName,
				ModelPartBuilder.create()
						.uv(0, 60).cuboid(Math.min(0.0f, pylonSpanX), -3.0f, -6.0f,
								Math.abs(pylonSpanX), 6.0f, 12.0f),
				ModelTransform.pivot(pylonPivotX, -1.0f, -2.0f));
		ModelPartData housing = pylon.addChild(housingName,
				ModelPartBuilder.create()
						.uv(90, 0).cuboid(-9.0f, -9.0f, -9.0f, 18.0f, 18.0f, 18.0f),
				ModelTransform.pivot(pylonSpanX, 0.0f, 0.0f));
		housing.addChild(rotorName,
				ModelPartBuilder.create().uv(90, 27).cuboid(-8.0f, -1.0f, -8.0f, 16.0f, 2.0f, 16.0f),
				ModelTransform.pivot(0.0f, 0.0f, 0.0f));
	}

	@Override
	public void setAngles(GunshipEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float spin = animationProgress * 2.2f;
		this.leftRotor.yaw = spin;
		this.rightRotor.yaw = -spin;
		this.tailRotor.yaw = spin * 1.6f;

		float turnRate = MathHelper.wrapDegrees(entity.getYaw() - entity.prevYaw);
		float bankDegrees = MathHelper.clamp(turnRate * BANK_GAIN, -MAX_BANK_DEGREES, MAX_BANK_DEGREES);
		float climbDegrees = MathHelper.clamp((float) -entity.getVelocity().y * CLIMB_PITCH_GAIN,
				-MAX_CLIMB_PITCH_DEGREES, MAX_CLIMB_PITCH_DEGREES);

		this.body.roll = bankDegrees * (float) (Math.PI / 180.0);
		this.body.pitch = climbDegrees * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
