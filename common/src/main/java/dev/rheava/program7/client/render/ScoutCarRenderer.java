package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ScoutCarEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class ScoutCarRenderer extends MobEntityRenderer<ScoutCarEntity, ScoutCarModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/scout_car.png");

	public ScoutCarRenderer(EntityRendererFactory.Context context) {
		super(context, new ScoutCarModel(context.getPart(ScoutCarModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(ScoutCarEntity entity) {
		return TEXTURE;
	}
}
