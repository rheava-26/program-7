package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.IFVEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class IFVRenderer extends MobEntityRenderer<IFVEntity, IFVModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/ifv.png");
	// Tier 3 size bump to the ~2.8 x 3.0 hitbox. Uniform for now - the "small
	// turret relative to hull" look is a later geometry pass, not a scale change.
	private static final float MODEL_SCALE = 1.55f;

	public IFVRenderer(EntityRendererFactory.Context context) {
		super(context, new IFVModel(context.getPart(IFVModel.LAYER)), 1.0f);
	}

	@Override
	public Identifier getTexture(IFVEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(IFVEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
