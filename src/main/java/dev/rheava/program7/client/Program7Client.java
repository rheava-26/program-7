package dev.rheava.program7.client;

import dev.rheava.program7.client.render.AttackDroneRenderer;
import dev.rheava.program7.client.render.DropPodRenderer;
import dev.rheava.program7.client.render.QuadRotorDroneModel;
import dev.rheava.program7.client.render.SurveyorDroneRenderer;
import dev.rheava.program7.registry.P7Entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class Program7Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(QuadRotorDroneModel.SURVEYOR_LAYER,
				QuadRotorDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(QuadRotorDroneModel.ATTACK_LAYER,
				QuadRotorDroneModel::getTexturedModelData);

		EntityRendererRegistry.register(P7Entities.SURVEYOR_DRONE, SurveyorDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.ATTACK_DRONE, AttackDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.DROP_POD, DropPodRenderer::new);
	}
}
