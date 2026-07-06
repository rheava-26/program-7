package dev.rheava.program7.client;

import dev.rheava.program7.client.render.SurveyorDroneModel;
import dev.rheava.program7.client.render.SurveyorDroneRenderer;
import dev.rheava.program7.registry.P7Entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class Program7Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(SurveyorDroneModel.LAYER,
				SurveyorDroneModel::getTexturedModelData);
		EntityRendererRegistry.register(P7Entities.SURVEYOR_DRONE, SurveyorDroneRenderer::new);
	}
}
