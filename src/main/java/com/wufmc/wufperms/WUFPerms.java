package com.wufmc.wufperms;

import com.wufmc.wufperms.command.ModCommands;
import com.wufmc.wufperms.event.PlayerDamageTracker;
import com.wufmc.wufperms.state.SpectatorReturnState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WUFPerms implements ModInitializer {
	public static final String MOD_ID = "wufperms";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Loading WUF Perms.");

		ModCommands.register();
		PlayerDamageTracker.register();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			SpectatorReturnState.load();
			LOGGER.info("Loaded spectator return locations.");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SpectatorReturnState.save();
			LOGGER.info("Saved spectator return locations.");
		});
	}
}
