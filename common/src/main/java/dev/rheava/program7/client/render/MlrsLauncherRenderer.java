package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MlrsLauncherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Reuses the howitzer's texture and scale as a placeholder (see {@link
 * MlrsLauncherModel}) — a distinct rocket-launcher paint job is a later art
 * pass.
 */
public class MlrsLauncherRenderer extends MobEntityRenderer<MlrsLauncherEntity, MlrsLauncherModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/howitzer.png");
	private static final float MODEL_SCALE = 2.16f;

	public MlrsLauncherRenderer(EntityRendererFactory.Context context) {
		super(context, new MlrsLauncherModel(context.getPart(MlrsLauncherModel.LAYER)), 1.7f);
	}

	@Override
	public Identifier getTexture(MlrsLauncherEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(MlrsLauncherEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
