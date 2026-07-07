package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.BatteryCenterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class BatteryCenterRenderer extends MobEntityRenderer<BatteryCenterEntity, BatteryCenterModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/battery_center.png");

	public BatteryCenterRenderer(EntityRendererFactory.Context context) {
		super(context, new BatteryCenterModel(context.getPart(BatteryCenterModel.LAYER)), 0.9f);
	}

	@Override
	public Identifier getTexture(BatteryCenterEntity entity) {
		return TEXTURE;
	}
}
