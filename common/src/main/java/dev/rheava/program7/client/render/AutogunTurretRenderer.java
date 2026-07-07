package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AutogunTurretEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class AutogunTurretRenderer extends MobEntityRenderer<AutogunTurretEntity, AutogunTurretModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/autogun_turret.png");

	public AutogunTurretRenderer(EntityRendererFactory.Context context) {
		super(context, new AutogunTurretModel(context.getPart(AutogunTurretModel.LAYER)), 0.45f);
	}

	@Override
	public Identifier getTexture(AutogunTurretEntity entity) {
		return TEXTURE;
	}
}
