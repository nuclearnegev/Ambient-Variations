package com.ambientvariations;

import com.ambientvariations.command.DebugCommand;
import com.ambientvariations.config.AVConfig;
import com.ambientvariations.director.Director;
import com.ambientvariations.director.EventRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbientVariations implements ModInitializer {
	public static final String MOD_ID = "ambientvariations";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AVConfig.load();
		Settings.register();
		EventRegistry.registerDefaults();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> DebugCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTING.register(server -> Director.reset());
		ServerTickEvents.END_SERVER_TICK.register(Director::onServerTick);
	}
}
