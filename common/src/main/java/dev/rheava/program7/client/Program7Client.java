package dev.rheava.program7.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.rheava.program7.client.render.AirUAVModel;
import dev.rheava.program7.client.render.AirUAVRenderer;
import dev.rheava.program7.client.render.AntiAirTurretModel;
import dev.rheava.program7.client.render.AntiAirTurretRenderer;
import dev.rheava.program7.client.render.ArmorPiercingArrowRenderer;
import dev.rheava.program7.client.render.AttackDroneRenderer;
import dev.rheava.program7.client.render.AutogunTurretModel;
import dev.rheava.program7.client.render.AutogunTurretRenderer;
import dev.rheava.program7.client.render.BatteryCenterModel;
import dev.rheava.program7.client.render.BatteryCenterRenderer;
import dev.rheava.program7.client.render.BombModel;
import dev.rheava.program7.client.render.BombRenderer;
import dev.rheava.program7.client.render.DropPodRenderer;
import dev.rheava.program7.client.render.GlowStickModel;
import dev.rheava.program7.client.render.GlowStickRenderer;
import dev.rheava.program7.client.render.GuidedMissileModel;
import dev.rheava.program7.client.render.GuidedMissileRenderer;
import dev.rheava.program7.client.render.GunboatModel;
import dev.rheava.program7.client.render.GunboatRenderer;
import dev.rheava.program7.client.render.GunshipModel;
import dev.rheava.program7.client.render.GunshipRenderer;
import dev.rheava.program7.client.render.HowitzerModel;
import dev.rheava.program7.client.render.HowitzerRenderer;
import dev.rheava.program7.client.render.HowitzerShellModel;
import dev.rheava.program7.client.render.HowitzerShellRenderer;
import dev.rheava.program7.client.render.GroundDroneModel;
import dev.rheava.program7.client.render.GroundDroneRenderer;
import dev.rheava.program7.client.render.HarvesterDroneModel;
import dev.rheava.program7.client.render.HarvesterDroneRenderer;
import dev.rheava.program7.client.render.HeavyAttackDroneModel;
import dev.rheava.program7.client.render.HeavyAttackDroneRenderer;
import dev.rheava.program7.client.render.IFVModel;
import dev.rheava.program7.client.render.IFVRenderer;
import dev.rheava.program7.client.render.LogisticsDroneModel;
import dev.rheava.program7.client.render.LogisticsDroneRenderer;
import dev.rheava.program7.client.render.MediumAttackDroneModel;
import dev.rheava.program7.client.render.MediumAttackDroneRenderer;
import dev.rheava.program7.client.render.MediumMiningDroneModel;
import dev.rheava.program7.client.render.MediumMiningDroneRenderer;
import dev.rheava.program7.client.render.MissileLauncherModel;
import dev.rheava.program7.client.render.MissileLauncherRenderer;
import dev.rheava.program7.client.render.MlrsLauncherModel;
import dev.rheava.program7.client.render.MlrsLauncherRenderer;
import dev.rheava.program7.client.render.MlrsRocketModel;
import dev.rheava.program7.client.render.MlrsRocketRenderer;
import dev.rheava.program7.client.render.MortarEmplacementModel;
import dev.rheava.program7.client.render.MortarEmplacementRenderer;
import dev.rheava.program7.client.render.MortarShellModel;
import dev.rheava.program7.client.render.MortarShellRenderer;
import dev.rheava.program7.client.render.QuadRotorDroneModel;
import dev.rheava.program7.client.render.ReconHelicopterModel;
import dev.rheava.program7.client.render.ReconHelicopterRenderer;
import dev.rheava.program7.client.render.ScoutCarModel;
import dev.rheava.program7.client.render.ScoutCarRenderer;
import dev.rheava.program7.client.render.SniperDroneModel;
import dev.rheava.program7.client.render.SniperDroneRenderer;
import dev.rheava.program7.client.render.SurveyorDroneRenderer;
import dev.rheava.program7.client.render.TransportDroneModel;
import dev.rheava.program7.client.render.TransportDroneRenderer;
import dev.rheava.program7.client.render.WheeledHaulerModel;
import dev.rheava.program7.client.render.WheeledHaulerRenderer;
import dev.rheava.program7.network.DatapadSnapshotPayload;
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
		EntityModelLayerRegistry.register(GroundDroneModel.LAYER,
				GroundDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(AutogunTurretModel.LAYER,
				AutogunTurretModel::getTexturedModelData);
		EntityModelLayerRegistry.register(LogisticsDroneModel.LAYER,
				LogisticsDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(WheeledHaulerModel.LAYER,
				WheeledHaulerModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MediumAttackDroneModel.LAYER,
				MediumAttackDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(SniperDroneModel.LAYER,
				SniperDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MortarEmplacementModel.LAYER,
				MortarEmplacementModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MortarShellModel.LAYER,
				MortarShellModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MediumMiningDroneModel.LAYER,
				MediumMiningDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(TransportDroneModel.LAYER,
				TransportDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(AntiAirTurretModel.LAYER,
				AntiAirTurretModel::getTexturedModelData);
		EntityModelLayerRegistry.register(ScoutCarModel.LAYER,
				ScoutCarModel::getTexturedModelData);
		EntityModelLayerRegistry.register(AirUAVModel.LAYER,
				AirUAVModel::getTexturedModelData);
		EntityModelLayerRegistry.register(HeavyAttackDroneModel.LAYER,
				HeavyAttackDroneModel::getTexturedModelData);
		EntityModelLayerRegistry.register(IFVModel.LAYER,
				IFVModel::getTexturedModelData);
		EntityModelLayerRegistry.register(GunboatModel.LAYER,
				GunboatModel::getTexturedModelData);
		EntityModelLayerRegistry.register(ReconHelicopterModel.LAYER,
				ReconHelicopterModel::getTexturedModelData);
		EntityModelLayerRegistry.register(BatteryCenterModel.LAYER,
				BatteryCenterModel::getTexturedModelData);
		EntityModelLayerRegistry.register(GunshipModel.LAYER,
				GunshipModel::getTexturedModelData);
		EntityModelLayerRegistry.register(HowitzerModel.LAYER,
				HowitzerModel::getTexturedModelData);
		EntityModelLayerRegistry.register(HowitzerShellModel.LAYER,
				HowitzerShellModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MlrsLauncherModel.LAYER,
				MlrsLauncherModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MlrsRocketModel.LAYER,
				MlrsRocketModel::getTexturedModelData);
		EntityModelLayerRegistry.register(MissileLauncherModel.LAYER,
				MissileLauncherModel::getTexturedModelData);
		EntityModelLayerRegistry.register(GuidedMissileModel.LAYER,
				GuidedMissileModel::getTexturedModelData);
		EntityModelLayerRegistry.register(BombModel.LAYER,
				BombModel::getTexturedModelData);
		EntityModelLayerRegistry.register(GlowStickModel.LAYER,
				GlowStickModel::getTexturedModelData);

		EntityRendererRegistry.register(P7Entities.SURVEYOR_DRONE, SurveyorDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.ATTACK_DRONE, AttackDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.HARVESTER_DRONE, HarvesterDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.DROP_POD, DropPodRenderer::new);
		EntityRendererRegistry.register(P7Entities.GROUND_DRONE, GroundDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.AUTOGUN_TURRET, AutogunTurretRenderer::new);
		EntityRendererRegistry.register(P7Entities.LOGISTICS_DRONE, LogisticsDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.WHEELED_HAULER, WheeledHaulerRenderer::new);
		EntityRendererRegistry.register(P7Entities.MEDIUM_ATTACK_DRONE, MediumAttackDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.SNIPER_DRONE, SniperDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.MORTAR_EMPLACEMENT, MortarEmplacementRenderer::new);
		EntityRendererRegistry.register(P7Entities.MORTAR_SHELL, MortarShellRenderer::new);
		EntityRendererRegistry.register(P7Entities.MEDIUM_MINING_DRONE, MediumMiningDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.TRANSPORT_DRONE, TransportDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.ANTI_AIR_TURRET, AntiAirTurretRenderer::new);
		EntityRendererRegistry.register(P7Entities.SCOUT_CAR, ScoutCarRenderer::new);
		EntityRendererRegistry.register(P7Entities.AIR_UAV, AirUAVRenderer::new);
		EntityRendererRegistry.register(P7Entities.HEAVY_ATTACK_DRONE, HeavyAttackDroneRenderer::new);
		EntityRendererRegistry.register(P7Entities.IFV, IFVRenderer::new);
		EntityRendererRegistry.register(P7Entities.GUNBOAT, GunboatRenderer::new);
		EntityRendererRegistry.register(P7Entities.RECON_HELICOPTER, ReconHelicopterRenderer::new);
		EntityRendererRegistry.register(P7Entities.BATTERY_CENTER, BatteryCenterRenderer::new);
		EntityRendererRegistry.register(P7Entities.GUNSHIP, GunshipRenderer::new);
		EntityRendererRegistry.register(P7Entities.HOWITZER, HowitzerRenderer::new);
		EntityRendererRegistry.register(P7Entities.HOWITZER_SHELL, HowitzerShellRenderer::new);
		EntityRendererRegistry.register(P7Entities.MLRS_LAUNCHER, MlrsLauncherRenderer::new);
		EntityRendererRegistry.register(P7Entities.MLRS_ROCKET, MlrsRocketRenderer::new);
		EntityRendererRegistry.register(P7Entities.MISSILE_LAUNCHER, MissileLauncherRenderer::new);
		EntityRendererRegistry.register(P7Entities.GUIDED_MISSILE, GuidedMissileRenderer::new);
		EntityRendererRegistry.register(P7Entities.BOMB, BombRenderer::new);
		EntityRendererRegistry.register(P7Entities.GLOW_STICK, GlowStickRenderer::new);
		EntityRendererRegistry.register(P7Entities.ARMOR_PIERCING_BOLT, ArmorPiercingArrowRenderer::new);

		NetworkManager.registerReceiver(NetworkManager.Side.S2C,
				InterferencePayload.ID, InterferencePayload.CODEC,
				(payload, context) -> InterferenceOverlay.onPacket(payload.intensity(), payload.threatYaw()));
		NetworkManager.registerReceiver(NetworkManager.Side.S2C,
				DatapadSnapshotPayload.ID, DatapadSnapshotPayload.CODEC,
				(payload, context) -> context.queue(() -> DatapadScreen.open(payload)));
		// The C2S refresh type is registered on both sides by the receiver
		// registration in InterferenceManager (common init), so the open screen
		// can send it without a separate client-side type registration here.
		ClientTickEvent.CLIENT_POST.register(InterferenceOverlay::clientTick);
		ClientGuiEvent.RENDER_HUD.register(InterferenceOverlay::render);
		ClientGuiEvent.RENDER_HUD.register(ChargeLaserHud::render);
	}

	private Program7Client() {
	}
}
