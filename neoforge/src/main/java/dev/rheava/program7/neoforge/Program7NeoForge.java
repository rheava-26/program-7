package dev.rheava.program7.neoforge;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.rheava.program7.Program7;
import dev.rheava.program7.client.Program7Client;
import net.neoforged.fml.common.Mod;

@Mod(Program7.MOD_ID)
public final class Program7NeoForge {
	public Program7NeoForge() {
		Program7.init();
		EnvExecutor.runInEnv(Env.CLIENT, () -> Program7Client::init);
	}
}
