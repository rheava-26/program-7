package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.IFVEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class IFVRenderer extends MobEntityRenderer<IFVEntity, IFVModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/ifv.png");

	public IFVRenderer(EntityRendererFactory.Context context) {
		super(context, new IFVModel(context.getPart(IFVModel.LAYER)), 1.0f);
	}

	@Override
	public Identifier getTexture(IFVEntity entity) {
		return TEXTURE;
	}
}
