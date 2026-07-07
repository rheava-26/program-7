package dev.rheava.program7.fabric.client;

import dev.rheava.program7.client.Program7Client;
import net.fabricmc.api.ClientModInitializer;

public final class Program7FabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Program7Client.init();
	}
}
