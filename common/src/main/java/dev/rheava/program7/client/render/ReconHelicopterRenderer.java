package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ReconHelicopterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class ReconHelicopterRenderer extends MobEntityRenderer<ReconHelicopterEntity, ReconHelicopterModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/recon_helicopter.png");

	public ReconHelicopterRenderer(EntityRendererFactory.Context context) {
		super(context, new ReconHelicopterModel(context.getPart(ReconHelicopterModel.LAYER)), 0.7f);
	}

	@Override
	public Identifier getTexture(ReconHelicopterEntity entity) {
		return TEXTURE;
	}
}
