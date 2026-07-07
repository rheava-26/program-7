package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class HarvesterDroneRenderer extends MobEntityRenderer<HarvesterDroneEntity, HarvesterDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/harvester_drone.png");

	public HarvesterDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new HarvesterDroneModel(context.getPart(HarvesterDroneModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(HarvesterDroneEntity entity) {
		return TEXTURE;
	}
}
