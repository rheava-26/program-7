package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GunboatEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GunboatRenderer extends MobEntityRenderer<GunboatEntity, GunboatModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/gunboat.png");

	public GunboatRenderer(EntityRendererFactory.Context context) {
		super(context, new GunboatModel(context.getPart(GunboatModel.LAYER)), 1.3f);
	}

	@Override
	public Identifier getTexture(GunboatEntity entity) {
		return TEXTURE;
	}
}
