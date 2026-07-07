package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AntiAirTurretEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class AntiAirTurretRenderer extends MobEntityRenderer<AntiAirTurretEntity, AntiAirTurretModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/anti_air_turret.png");

	public AntiAirTurretRenderer(EntityRendererFactory.Context context) {
		super(context, new AntiAirTurretModel(context.getPart(AntiAirTurretModel.LAYER)), 0.45f);
	}

	@Override
	public Identifier getTexture(AntiAirTurretEntity entity) {
		return TEXTURE;
	}
}
