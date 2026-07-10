package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunshipEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders the Tier 4 apex gunship with its own custom texture (gunmetal
 * plating, Program-cyan nose + flank glow, dark ducted-rotor rings). NOTE:
 * the model's first-pass UV layout is cramped — a few housing/rotor faces
 * overlap in the atlas — so a full sculpted model + clean UV unwrap is still
 * owed; the texture is painted to work around the current layout.
 */
public class GunshipRenderer extends MobEntityRenderer<GunshipEntity, GunshipModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/gunship.png");
	// Tier 4 apex: bigger than the Tier 3 heavy drone (1.33) to sell "house-
	// sized", but pulled back from an earlier 1.9 so the airframe's visual
	// footprint stays inside the 4.6-wide hitbox — otherwise the rotor ducts
	// (the "aim for the rotors" weak point) render out past where hits land.
	private static final float MODEL_SCALE = 1.5f;

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
