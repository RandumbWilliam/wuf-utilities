package com.wufmc.wufperms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.wufmc.wufperms.state.SpectatorReturnLocation;
import com.wufmc.wufperms.state.SpectatorReturnState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;

public final class PlayerModeCommand {
    private PlayerModeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spectate")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> enterSpectator(context.getSource()))
        );

        dispatcher.register(Commands.literal("survival")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> returnToSurvival(context.getSource()))
        );
    }

    private static int enterSpectator(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Only a player can use this command."));

            return 0;
        }

        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            source.sendFailure(Component.literal("You are already in spectator mode."));

            return 0;
        }

        if (SpectatorReturnState.hasLocation(player.getUUID())) {
            source.sendFailure(
                    Component.literal(
                            "You already have a saved spectator "
                                    + "return location. Use /survival first."
                    )
            );

            return 0;
        }

        SpectatorReturnLocation location = SpectatorReturnLocation.fromPlayer(player);

        boolean saved = SpectatorReturnState.saveLocation(player.getUUID(), location);

        if (!saved) {
            source.sendFailure(Component.literal("Your return location could not be saved."));

            return 0;
        }

        boolean changed = player.setGameMode(GameType.SPECTATOR);

        if (!changed) {
            /*
             * Do not leave behind a return location when changing mode fails.
             */
            SpectatorReturnState.removeLocation(player.getUUID());

            source.sendFailure(Component.literal("Could not enter spectator mode.")
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Entered spectator mode. "
                                + "Use /survival to return."
                ),
                false
        );

        return 1;
    }

    private static int returnToSurvival(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Only a player can use this command."));

            return 0;
        }

        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            source.sendFailure(
                    Component.literal(
                            "You must be in spectator mode "
                                    + "to use /survival."
                    )
            );

            return 0;
        }

        Optional<SpectatorReturnLocation> optionalLocation = SpectatorReturnState.getLocation(player.getUUID());

        if (optionalLocation.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You do not have a saved return location."
                    )
            );

            return 0;
        }

        SpectatorReturnLocation location = optionalLocation.get();

        Identifier dimensionId;

        try {
            dimensionId = Identifier.parse(location.dimension());
        } catch (IdentifierException exception) {
            source.sendFailure(Component.literal("Your saved dimension ID is invalid."));

            return 0;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

        ServerLevel destination =
                source.getServer().getLevel(dimensionKey);

        if (destination == null) {
            source.sendFailure(Component.literal("Your saved dimension is unavailable."));

            return 0;
        }

        boolean teleported = player.teleportTo(
                destination,
                location.x(),
                location.y(),
                location.z(),
                Set.<Relative>of(),
                location.yaw(),
                location.pitch(),
                true
        );

        if (!teleported) {
            source.sendFailure(Component.literal("Could not return to your saved location."));

            return 0;
        }

        boolean changed = player.setGameMode(GameType.SURVIVAL);

        if (!changed) {
            source.sendFailure(
                    Component.literal(
                            "You returned to your location, "
                                    + "but survival mode could not be set."
                    )
            );

            return 0;
        }

        SpectatorReturnState.removeLocation(player.getUUID());

        source.sendSuccess(
                () -> Component.literal(
                        "Returned to your saved location "
                                + "in survival mode."
                ),
                false
        );

        return 1;
    }
}