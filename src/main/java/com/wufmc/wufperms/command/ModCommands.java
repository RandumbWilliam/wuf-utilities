package com.wufmc.wufperms.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    PlayerTeleportCommand.register(dispatcher);
                    PlayerModeCommand.register(dispatcher);
                }
        );
    }
}