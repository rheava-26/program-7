package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.WheeledHaulerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class WheeledHaulerRenderer extends MobEntityRenderer<WheeledHaulerEntity, WheeledHaulerModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/wheeled_hauler.png");

	public WheeledHaulerRenderer(EntityRendererFactory.Context context) {
		super(context, new WheeledHaulerModel(context.getPart(WheeledHaulerModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(WheeledHaulerEntity entity) {
		return TEXTURE;
	}
}
