package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class LogisticsDroneRenderer extends MobEntityRenderer<LogisticsDroneEntity, LogisticsDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/logistics_drone.png");

	public LogisticsDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new LogisticsDroneModel(context.getPart(LogisticsDroneModel.LAYER)), 0.4f);
	}

	@Override
	public Identifier getTexture(LogisticsDroneEntity entity) {
		return TEXTURE;
	}
}
