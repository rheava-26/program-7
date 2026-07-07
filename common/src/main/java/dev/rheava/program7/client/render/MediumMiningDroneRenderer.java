package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MediumMiningDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MediumMiningDroneRenderer extends MobEntityRenderer<MediumMiningDroneEntity, MediumMiningDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/medium_mining_drone.png");

	public MediumMiningDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new MediumMiningDroneModel(context.getPart(MediumMiningDroneModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(MediumMiningDroneEntity entity) {
		return TEXTURE;
	}
}
