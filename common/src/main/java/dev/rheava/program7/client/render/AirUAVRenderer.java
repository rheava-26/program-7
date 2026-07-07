package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AirUAVEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class AirUAVRenderer extends MobEntityRenderer<AirUAVEntity, AirUAVModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/air_uav.png");

	public AirUAVRenderer(EntityRendererFactory.Context context) {
		super(context, new AirUAVModel(context.getPart(AirUAVModel.LAYER)), 0.4f);
	}

	@Override
	public Identifier getTexture(AirUAVEntity entity) {
		return TEXTURE;
	}
}
