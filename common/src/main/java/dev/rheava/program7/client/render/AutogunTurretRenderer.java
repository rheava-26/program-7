package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AutogunTurretEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AutogunTurretRenderer extends MobEntityRenderer<AutogunTurretEntity, AutogunTurretModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/autogun_turret.png");

	// Model scale for the Tier 1 fixed-defense size pass — see P7Entities'
	// matching autogun_turret hitbox bump in the shared registry file.
	private static final float MODEL_SCALE = 1.35f;

	public AutogunTurretRenderer(EntityRendererFactory.Context context) {
		super(context, new AutogunTurretModel(context.getPart(AutogunTurretModel.LAYER)), 0.45f);
		// Static emplacement: vanilla's small circular blob shadow reads wrong
		// under a wide gun mount, so drop it to effectively nothing.
		this.shadowRadius = 0.0f;
	}

	@Override
	public Identifier getTexture(AutogunTurretEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(AutogunTurretEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
