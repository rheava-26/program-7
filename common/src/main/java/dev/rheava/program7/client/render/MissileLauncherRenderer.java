package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MissileLauncherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Reuses the howitzer's texture and scale as a placeholder (see {@link MissileLauncherModel}). */
public class MissileLauncherRenderer extends MobEntityRenderer<MissileLauncherEntity, MissileLauncherModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/howitzer.png");
	private static final float MODEL_SCALE = 2.16f;

	public MissileLauncherRenderer(EntityRendererFactory.Context context) {
		super(context, new MissileLauncherModel(context.getPart(MissileLauncherModel.LAYER)), 1.7f);
	}

	@Override
	public Identifier getTexture(MissileLauncherEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(MissileLauncherEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
