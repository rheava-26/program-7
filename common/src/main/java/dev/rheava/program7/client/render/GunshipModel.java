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
 * The Tier 4 apex gunship. Silhouette per {@code UNITS.md}'s Tier 4 entry: a
 * fat Osprey-derived fuselage, but instead of wings, two big metal-shrouded
 * (ducted) rotors on short stub pylons, plus a helicopter-style tail rotor at
 * the rear, plus a belly-mounted autocannon turret. Sculpted pass: the ducts
 * are open rings (four rims round a hollow centre) with a spinnable
 * crossed-blade rotor inside each, the fuselage tapers from a fat rear hull
 * through a stepped-in cockpit to a nose sensor, and the tail boom tapers into
 * a crossed tail rotor — all packed onto a clean 256x128 UV atlas.
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

	// Tiltrotor: the ducted pylons swing from vertical-lift (hover) toward
	// forward-thrust as the airframe builds horizontal speed — the V-22
	// transition. Full tilt by roughly cruise speed; capped short of flat so it
	// reads as "leaning into forward flight", not a fixed-wing.
	private static final double FULL_TILT_SPEED = 0.28;
	private static final float MAX_TILT_DEGREES = 72.0f;

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart leftPylon;
	private final ModelPart rightPylon;
	private final ModelPart leftRotor;
	private final ModelPart rightRotor;
	private final ModelPart tailRotor;

	public GunshipModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.leftPylon = this.body.getChild("left_pylon");
		this.rightPylon = this.body.getChild("right_pylon");
		this.leftRotor = this.leftPylon.getChild("left_rotor");
		this.rightRotor = this.rightPylon.getChild("right_rotor");
		this.tailRotor = this.body.getChild("tail_boom").getChild("tail_boom2")
				.getChild("tail_housing").getChild("tail_rotor");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// Tapered fuselage: fat rear hull (where the ducts + tail mount) ->
		// stepped-in cockpit -> small nose sensor, plus a dorsal spine.
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-14.0f, -6.0f, -4.0f, 28.0f, 12.0f, 24.0f)
						.uv(105, 0).cuboid(-10.0f, -5.0f, -20.0f, 20.0f, 10.0f, 16.0f)
						.uv(160, 61).cuboid(-5.0f, -4.0f, -26.0f, 10.0f, 7.0f, 6.0f)
						.uv(178, 0).cuboid(-3.0f, -9.0f, -6.0f, 6.0f, 3.0f, 14.0f),
				ModelTransform.pivot(0.0f, 10.0f, 0.0f));

		// Belly-mounted autocannon turret + barrel, hung underneath, forward.
		body.addChild("belly_turret",
				ModelPartBuilder.create()
						.uv(84, 61).cuboid(-3.0f, -2.0f, -5.0f, 6.0f, 4.0f, 10.0f)
						.uv(87, 105).cuboid(-1.0f, -1.0f, -9.0f, 2.0f, 2.0f, 4.0f),
				ModelTransform.pivot(0.0f, 6.0f, -8.0f));

		// Left ducted fan: stub strut + open ring (4 rims) on the tilting pylon,
		// with a spinnable crossed-blade rotor sub-part inside the ring.
		ModelPartData leftPylon = body.addChild("left_pylon",
				ModelPartBuilder.create()
						.uv(193, 61).cuboid(-8.0f, -2.0f, -3.0f, 8.0f, 4.0f, 6.0f)
						.uv(0, 88).cuboid(-11.0f, -11.0f, -3.0f, 22.0f, 4.0f, 6.0f)
						.uv(57, 88).cuboid(-11.0f, 7.0f, -3.0f, 22.0f, 4.0f, 6.0f)
						.uv(0, 61).cuboid(-11.0f, -7.0f, -3.0f, 4.0f, 14.0f, 6.0f)
						.uv(21, 61).cuboid(7.0f, -7.0f, -3.0f, 4.0f, 14.0f, 6.0f),
				ModelTransform.pivot(-6.0f, 0.0f, 2.0f));
		leftPylon.addChild("left_rotor",
				ModelPartBuilder.create()
						.uv(100, 105).cuboid(-9.0f, -1.0f, -1.0f, 18.0f, 2.0f, 2.0f)
						.uv(117, 61).cuboid(-1.0f, -9.0f, -1.0f, 2.0f, 18.0f, 2.0f),
				ModelTransform.pivot(0.0f, 0.0f, 0.0f));

		// Right ducted fan, mirrored.
		ModelPartData rightPylon = body.addChild("right_pylon",
				ModelPartBuilder.create()
						.uv(114, 88).cuboid(0.0f, -2.0f, -3.0f, 8.0f, 4.0f, 6.0f)
						.uv(143, 88).cuboid(-11.0f, -11.0f, -3.0f, 22.0f, 4.0f, 6.0f)
						.uv(0, 105).cuboid(-11.0f, 7.0f, -3.0f, 22.0f, 4.0f, 6.0f)
						.uv(42, 61).cuboid(-11.0f, -7.0f, -3.0f, 4.0f, 14.0f, 6.0f)
						.uv(63, 61).cuboid(7.0f, -7.0f, -3.0f, 4.0f, 14.0f, 6.0f),
				ModelTransform.pivot(6.0f, 0.0f, 2.0f));
		rightPylon.addChild("right_rotor",
				ModelPartBuilder.create()
						.uv(141, 105).cuboid(-9.0f, -1.0f, -1.0f, 18.0f, 2.0f, 2.0f)
						.uv(126, 61).cuboid(-1.0f, -9.0f, -1.0f, 2.0f, 18.0f, 2.0f),
				ModelTransform.pivot(0.0f, 0.0f, 0.0f));

		// Tapering tail boom -> housing -> spinnable crossed tail rotor.
		ModelPartData tailBoom = body.addChild("tail_boom",
				ModelPartBuilder.create()
						.uv(219, 0).cuboid(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 12.0f),
				ModelTransform.pivot(0.0f, -2.0f, 20.0f));
		ModelPartData tailBoom2 = tailBoom.addChild("tail_boom2",
				ModelPartBuilder.create()
						.uv(135, 61).cuboid(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 8.0f),
				ModelTransform.pivot(0.0f, 0.0f, 12.0f));
		ModelPartData tailHousing = tailBoom2.addChild("tail_housing",
				ModelPartBuilder.create()
						.uv(57, 105).cuboid(-4.0f, -4.0f, -2.0f, 8.0f, 8.0f, 4.0f),
				ModelTransform.pivot(0.0f, 0.0f, 8.0f));
		tailHousing.addChild("tail_rotor",
				ModelPartBuilder.create()
						.uv(182, 105).cuboid(-6.0f, -0.5f, -0.5f, 12.0f, 1.0f, 1.0f)
						.uv(82, 105).cuboid(-0.5f, -6.0f, -0.5f, 1.0f, 12.0f, 1.0f),
				ModelTransform.pivot(0.0f, 0.0f, 2.0f));

		return TexturedModelData.of(modelData, 256, 128);
	}

	@Override
	public void setAngles(GunshipEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float spin = animationProgress * 2.2f;
		this.leftRotor.yaw = spin;
		this.rightRotor.yaw = -spin;
		this.tailRotor.yaw = spin * 1.6f;

		// Tiltrotor transition: the ducted pylons swing from vertical-lift (hover)
		// toward forward-thrust as horizontal airspeed builds — the V-22 pivot.
		// (Sign is in-game-tunable: flip to -tilt if the ducts lean the wrong way.)
		double vx = entity.getVelocity().x;
		double vz = entity.getVelocity().z;
		double horizSpeed = Math.sqrt(vx * vx + vz * vz);
		float tilt = MathHelper.clamp((float) (horizSpeed / FULL_TILT_SPEED), 0.0f, 1.0f)
				* MAX_TILT_DEGREES * (float) (Math.PI / 180.0);
		this.leftPylon.pitch = tilt;
		this.rightPylon.pitch = tilt;

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
