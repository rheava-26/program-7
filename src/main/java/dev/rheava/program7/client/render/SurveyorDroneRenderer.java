package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class SurveyorDroneRenderer extends MobEntityRenderer<SurveyorDroneEntity, SurveyorDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/surveyor_drone.png");

	public SurveyorDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new SurveyorDroneModel(context.getPart(SurveyorDroneModel.LAYER)), 0.4f);
	}

	@Override
	public Identifier getTexture(SurveyorDroneEntity entity) {
		return TEXTURE;
	}
}
