package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AntiAirTurretEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AntiAirTurretRenderer extends MobEntityRenderer<AntiAirTurretEntity, AntiAirTurretModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/anti_air_turret.png");
	// Model scale for the Tier 2 fixed-defense size pass — see P7Entities'
	// matching anti_air_turret hitbox bump in the shared registry file.
	private static final float MODEL_SCALE = 1.35f;

	public AntiAirTurretRenderer(EntityRendererFactory.Context context) {
		super(context, new AntiAirTurretModel(context.getPart(AntiAirTurretModel.LAYER)), 0.45f);
		// Static emplacement: vanilla's small circular blob shadow reads wrong
		// under a wide flak mount, so drop it to effectively nothing.
		this.shadowRadius = 0.0f;
	}

	@Override
	public Identifier getTexture(AntiAirTurretEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(AntiAirTurretEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
