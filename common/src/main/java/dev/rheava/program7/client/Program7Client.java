package dev.rheava.program7.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.EntityModelLayerRegistry;
import dev.rheava.program7.client.render.AttackDroneRenderer;
import dev.rheava.program7.client.render.DropPodRenderer;
import dev.rheava.program7.client.render.HarvesterDroneModel;
import dev.rheava.program7.client.render.HarvesterDroneRenderer;
import dev.rheava.program7.client.render.QuadRotorDroneModel;
import dev.rheava.program7.client.render.SurveyorDroneRenderer;
import dev.rheava.program7.network.InterferencePayload;
import dev.rheava.program7.registry.P7Entities;

/**
 * Loader-agnostic client setup; called by the Fabric and NeoForge client
 * entrypoints.
 */
public final class Program7Client {
	public static void init() {
		EntityModelLayerRegistry.register(QuadRotorDroneModel.SURVEYOR_LAYER,
				QuadRotorDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(QuadRotorDroneModel.ATTACK_LAYER,
				QuadRotorDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(HarvesterDroneModel.LAYER,
				HarvesterDroneModel::getTexturedModelData);

		EntityRendererRegistry.register(P7Entities.SURVEYOR_DRONE, SurveyorDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.ATTACK_DRONE, AttackDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.HARVESTER_DRONE, HarvesterDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.DROP_POD, DropPodRenderer::new);

		NetworkManager.registerReceiver(NetworkManager.Side.S2C,
				InterferencePayload.ID, InterferencePayload.CODEC,
				(payload, context) -> InterferenceOverlay.onPacket(payload.intensity(), payload.threatYaw()));
		ClientTickEvent.CLIENT_POST.register(InterferenceOverlay::clientTick);
		ClientGuiEvent.RENDER_HUD.register(InterferenceOverlay::render);
	}

	private Program7Client() {
	}
}
