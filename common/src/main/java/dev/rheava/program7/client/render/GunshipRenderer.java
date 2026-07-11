package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunshipEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders the Tier 4 apex gunship with its own custom texture (gunmetal
 * plating, Program-cyan nose sensor + belly-turret eye, dark open ducted-rotor
 * rings), painted to the sculpted model's clean 256x128 box-UV atlas.
 */
public class GunshipRenderer extends MobEntityRenderer<GunshipEntity, GunshipModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/gunship.png");
	// Tier 4 apex: pushed up to sell "house-sized" per the "everything's too
	// small" pass. The sculpted airframe's widest point (the outer duct rims,
	// ~17px = ~1.06 blocks) still stays inside the 4.6-wide hitbox at this
	// scale (~4.0 blocks), so the rotor ducts — the "aim for the rotors" weak
	// point — render where hits actually land.
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
