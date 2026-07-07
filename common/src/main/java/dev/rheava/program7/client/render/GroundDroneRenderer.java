package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.GroundDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GroundDroneRenderer extends MobEntityRenderer<GroundDroneEntity, GroundDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/ground_drone.png");

	public GroundDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new GroundDroneModel(context.getPart(GroundDroneModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(GroundDroneEntity entity) {
		return TEXTURE;
	}
}
