package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.TransportDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class TransportDroneRenderer extends MobEntityRenderer<TransportDroneEntity, TransportDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/transport_drone.png");

	public TransportDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new TransportDroneModel(context.getPart(TransportDroneModel.LAYER)), 0.45f);
	}

	@Override
	public Identifier getTexture(TransportDroneEntity entity) {
		return TEXTURE;
	}
}
