package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AttackDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class AttackDroneRenderer
		extends MobEntityRenderer<AttackDroneEntity, QuadRotorDroneModel<AttackDroneEntity>> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/attack_drone.png");

	public AttackDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new QuadRotorDroneModel<>(context.getPart(QuadRotorDroneModel.ATTACK_LAYER)), 0.4f);
	}

	@Override
	public Identifier getTexture(AttackDroneEntity entity) {
		return TEXTURE;
	}
}
