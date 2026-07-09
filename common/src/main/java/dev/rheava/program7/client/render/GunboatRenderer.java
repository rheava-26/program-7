package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunboatEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GunboatRenderer extends MobEntityRenderer<GunboatEntity, GunboatModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/gunboat.png");
	// Tier 3 size bump: this is the giant of the lineup, stretching to the
	// ~4.5 x 3.0 hitbox with a long hull.
	private static final float MODEL_SCALE = 1.55f;

	public GunboatRenderer(EntityRendererFactory.Context context) {
		super(context, new GunboatModel(context.getPart(GunboatModel.LAYER)), 1.3f);
	}

	@Override
	public Identifier getTexture(GunboatEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(GunboatEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
