package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MlrsLauncherEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * The MLRS launcher shares the howitzer's tracked-chassis geometry
 * ({@link HowitzerModel#getTexturedModelData()}) as a placeholder — both are
 * tracked artillery hulls, and a dedicated rocket-rack silhouette (angled
 * launch tubes instead of a gun barrel) is a later art pass, not required to
 * stand the unit up. Only the entity type binding differs from {@link
 * HowitzerModel}; the baked geometry itself is identical.
 */
public class MlrsLauncherModel extends SinglePartEntityModel<MlrsLauncherEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("mlrs_launcher"), "main");

	private final ModelPart root;
	private final ModelPart turret;

	public MlrsLauncherModel(ModelPart root) {
		this.root = root;
		this.turret = root.getChild("body").getChild("turret");
	}

	public static TexturedModelData getTexturedModelData() {
		return HowitzerModel.getTexturedModelData();
	}

	@Override
	public void setAngles(MlrsLauncherEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch) {
		this.turret.yaw = headYaw * (float) (Math.PI / 180.0);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}
}
