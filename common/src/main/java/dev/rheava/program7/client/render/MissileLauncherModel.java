package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MissileLauncherEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * Shares the howitzer's tracked-chassis geometry as a placeholder, the same
 * choice {@link MlrsLauncherModel} makes and for the same reason — a
 * dedicated missile-canister silhouette is a later art pass, not required
 * to stand the unit up.
 */
public class MissileLauncherModel extends SinglePartEntityModel<MissileLauncherEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("missile_launcher"), "main");

	private final ModelPart root;
	private final ModelPart turret;

	public MissileLauncherModel(ModelPart root) {
		this.root = root;
		this.turret = root.getChild("body").getChild("turret");
	}

	public static TexturedModelData getTexturedModelData() {
		return HowitzerModel.getTexturedModelData();
	}

	@Override
	public void setAngles(MissileLauncherEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
