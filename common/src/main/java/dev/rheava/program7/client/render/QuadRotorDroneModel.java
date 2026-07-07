package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * The small-drone chassis, FPV-proportioned: a low, flat, layered hull with
 * angled armor cheeks (rolled panels faking curvature), four diagonal motor
 * arms, and oversized rotor discs nearly the size of the hull. Deliberately
 * reads as "machine", not "creature". Each unit gets its own texture +
 * model layer.
 */
public class QuadRotorDroneModel<T extends MobEntity> extends SinglePartEntityModel<T> {
	public static final EntityModelLayer SURVEYOR_LAYER = new EntityModelLayer(Program7.id("surveyor_drone"), "main");
	public static final EntityModelLayer ATTACK_LAYER = new EntityModelLayer(Program7.id("attack_drone"), "main");

	private static final String[] ARM_NAMES = {"arm_fl", "arm_fr", "arm_bl", "arm_br"};

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart[] rotors = new ModelPart[4];

	public QuadRotorDroneModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		for (int i = 0; i < ARM_NAMES.length; i++) {
			this.rotors[i] = this.body.getChild(ARM_NAMES[i]).getChild("rotor");
		}
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create()
						// low main slab
						.uv(0, 0).cuboid(-5.0f, -1.5f, -4.0f, 10.0f, 3.0f, 8.0f)
						// raised spine layer (bevel illusion)
						.uv(0, 12).cuboid(-4.0f, -3.5f, -3.0f, 8.0f, 2.0f, 6.0f)
						// forward sensor housing with the red bar on its face
						.uv(0, 21).cuboid(-3.0f, -1.0f, -6.0f, 6.0f, 2.0f, 2.0f)
						// belly pack (battery/payload)
						.uv(0, 26).cuboid(-3.0f, 1.5f, -3.0f, 6.0f, 2.0f, 6.0f),
				ModelTransform.pivot(0.0f, 18.0f, 0.0f));

		// Rolled armor cheeks — the "curved" silhouette.
		body.addChild("panel_left",
				ModelPartBuilder.create().uv(29, 12).cuboid(-1.0f, -1.5f, -3.5f, 1.0f, 3.0f, 7.0f),
				ModelTransform.of(-5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.30f));
		body.addChild("panel_right",
				ModelPartBuilder.create().uv(29, 12).cuboid(0.0f, -1.5f, -3.5f, 1.0f, 3.0f, 7.0f),
				ModelTransform.of(5.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.30f));

		addArm(body, "arm_fl", -3.5f, -2.5f, (float) (-Math.PI / 4.0));
		addArm(body, "arm_fr", 3.5f, -2.5f, (float) (Math.PI / 4.0));
		addArm(body, "arm_bl", -3.5f, 2.5f, (float) (-Math.PI * 3.0 / 4.0));
		addArm(body, "arm_br", 3.5f, 2.5f, (float) (Math.PI * 3.0 / 4.0));

		return TexturedModelData.of(modelData, 64, 64);
	}

	private static void addArm(ModelPartData body, String name, float x, float z, float yaw) {
		ModelPartData arm = body.addChild(name,
				ModelPartBuilder.create()
						.uv(37, 0).cuboid(-0.5f, -0.5f, -5.0f, 1.0f, 1.0f, 5.0f)
						.uv(37, 7).cuboid(-1.0f, -1.5f, -6.0f, 2.0f, 2.0f, 2.0f),
				ModelTransform.of(x, -1.5f, z, 0.0f, yaw, 0.0f));
		// Head-sized rotor disc riding on the motor pod.
		arm.addChild("rotor",
				ModelPartBuilder.create().uv(0, 35).cuboid(-3.5f, -0.5f, -3.5f, 7.0f, 1.0f, 7.0f),
				ModelTransform.pivot(0.0f, -2.0f, -5.0f));
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		float spin = animationProgress * 2.8f;
		for (int i = 0; i < this.rotors.length; i++) {
			this.rotors[i].yaw = (i % 2 == 0) ? spin : -spin;
		}

		// Idle hover bob plus a slight nose-down lean toward whatever it watches.
		this.body.pivotY = 18.0f + MathHelper.sin(animationProgress * 0.18f) * 0.5f;
		this.body.pitch = headPitch * ((float) Math.PI / 180.0f) * 0.3f;
		this.body.yaw = headYaw * ((float) Math.PI / 180.0f) * 0.2f;
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
