package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HeavyAttackDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class HeavyAttackDroneRenderer extends MobEntityRenderer<HeavyAttackDroneEntity, HeavyAttackDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/heavy_attack_drone.png");

	public HeavyAttackDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new HeavyAttackDroneModel(context.getPart(HeavyAttackDroneModel.LAYER)), 0.9f);
	}

	@Override
	public Identifier getTexture(HeavyAttackDroneEntity entity) {
		return TEXTURE;
	}
}
