package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ReconHelicopterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ReconHelicopterRenderer extends MobEntityRenderer<ReconHelicopterEntity, ReconHelicopterModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/recon_helicopter.png");
	// Tier 3 size bump: fuselage/skid footprint now fills the ~3.0 x 2.6 hitbox,
	// with the main rotor and tail boom intentionally overhanging past it.
	private static final float MODEL_SCALE = 1.65f;

	public ReconHelicopterRenderer(EntityRendererFactory.Context context) {
		super(context, new ReconHelicopterModel(context.getPart(ReconHelicopterModel.LAYER)), 0.7f);
	}

	@Override
	public Identifier getTexture(ReconHelicopterEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(ReconHelicopterEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
