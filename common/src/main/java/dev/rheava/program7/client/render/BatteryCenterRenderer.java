package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.BatteryCenterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BatteryCenterRenderer extends MobEntityRenderer<BatteryCenterEntity, BatteryCenterModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/battery_center.png");
	// Tier 3 size bump to the ~2.4 x 2.6 hitbox.
	private static final float MODEL_SCALE = 1.4f;

	public BatteryCenterRenderer(EntityRendererFactory.Context context) {
		super(context, new BatteryCenterModel(context.getPart(BatteryCenterModel.LAYER)), 0.9f);
	}

	@Override
	public Identifier getTexture(BatteryCenterEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(BatteryCenterEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
