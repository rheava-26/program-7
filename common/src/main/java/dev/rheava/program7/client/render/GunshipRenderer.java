package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunshipEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders the Tier 4 apex gunship. Reuses the heavy attack drone's texture
 * as a placeholder — per the manager's brief, a real sculpted texture is a
 * later art pass; this pass only needs the airframe on screen and readable
 * at scale, not painted correctly.
 */
public class GunshipRenderer extends MobEntityRenderer<GunshipEntity, GunshipModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/heavy_attack_drone.png");
	// Tier 4 apex: noticeably bigger on screen than the Tier 3 heavy attack
	// drone's 1.33 scale to sell "house-sized" per UNITS.md.
	private static final float MODEL_SCALE = 1.9f;

	public GunshipRenderer(EntityRendererFactory.Context context) {
		super(context, new GunshipModel(context.getPart(GunshipModel.LAYER)), 2.2f);
	}

	@Override
	public Identifier getTexture(GunshipEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(GunshipEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
