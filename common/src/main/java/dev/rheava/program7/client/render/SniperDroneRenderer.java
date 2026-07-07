package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.SniperDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class SniperDroneRenderer extends MobEntityRenderer<SniperDroneEntity, SniperDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/sniper_drone.png");

	public SniperDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new SniperDroneModel(context.getPart(SniperDroneModel.LAYER)), 0.35f);
	}

	@Override
	public Identifier getTexture(SniperDroneEntity entity) {
		return TEXTURE;
	}
}
