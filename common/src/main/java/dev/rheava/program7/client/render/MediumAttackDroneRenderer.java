package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MediumAttackDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MediumAttackDroneRenderer extends MobEntityRenderer<MediumAttackDroneEntity, MediumAttackDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/medium_attack_drone.png");

	public MediumAttackDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new MediumAttackDroneModel(context.getPart(MediumAttackDroneModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(MediumAttackDroneEntity entity) {
		return TEXTURE;
	}
}
