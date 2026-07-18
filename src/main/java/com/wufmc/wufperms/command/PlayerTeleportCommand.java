package com.wufmc.wufperms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wufmc.wufperms.config.WUFPermsConfig;
import com.wufmc.wufperms.event.PlayerDamageTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.Set;

public final class PlayerTeleportCommand {
    private PlayerTeleportCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ptp")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (ServerPlayer player : context.getSource()
                                    .getServer()
                                    .getPlayerList()
                                    .getPlayers()) {
                                builder.suggest(player.getGameProfile().name());
                            }

                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String targetName = StringArgumentType.getString(context, "player");

                            return execute(context.getSource(), targetName);
                        })
            )
        );
    }

    private static int execute(CommandSourceStack source, String targetName) {
        ServerPlayer sender;

        try {
            sender = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Only a player can use this command."));

            return 0;
        }

        ServerPlayer target = source.getServer()
                .getPlayerList()
                .getPlayerByName(targetName);

        if (target == null) {
            source.sendFailure(Component.literal("That player is not currently online."));

            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You cannot teleport to yourself."));

            return 0;
        }

        if (sender.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            source.sendFailure(Component.literal("You cannot use /ptp while spectating."));

            return 0;
        }

        if (!PlayerDamageTracker.mayTeleport(sender)) {
            long remainingSeconds = PlayerDamageTracker.getRemainingCooldownSeconds(sender);

            source.sendFailure(Component.literal(
                        "You took damage recently. "
                                + "You can teleport in "
                                + formatTime(remainingSeconds)
                                + "."
                    )
            );

            return 0;
        }

        if (hasNearbyHostileMob(sender)) {
            source.sendFailure(Component.literal("You cannot teleport while hostile mobs " + "are nearby."));

            return 0;
        }

        if (WUFPermsConfig.CHECK_DESTINATION_FOR_HOSTILES
                && hasNearbyHostileMob(target)) {
            source.sendFailure(
                    Component.literal(
                            "You cannot teleport because hostile mobs "
                                    + "are near the destination."
                    )
            );

            return 0;
        }

        boolean teleported = sender.teleportTo(
                target.level(),
                target.getX(),
                target.getY(),
                target.getZ(),
                Set.<Relative>of(),
                target.getYRot(),
                target.getXRot(),
                true
        );

        if (!teleported) {
            source.sendFailure(
                    Component.literal("The teleport failed.")
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Teleported to "
                                + target.getGameProfile().name()
                                + "."
                ),
                false
        );

        return 1;
    }

    private static boolean hasNearbyHostileMob(ServerPlayer player) {
        ServerLevel level = player.level();

        AABB searchArea =
                player.getBoundingBox().inflate(
                        WUFPermsConfig.HOSTILE_RANGE_HORIZONTAL,
                        WUFPermsConfig.HOSTILE_RANGE_VERTICAL,
                        WUFPermsConfig.HOSTILE_RANGE_HORIZONTAL
                );

        return !level.getEntitiesOfClass(
                Monster.class,
                searchArea,
                monster ->
                        monster.isAlive()
                                && !monster.isSpectator()
        ).isEmpty();
    }

    private static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        if (minutes <= 0L) {
            return seconds + " second" + (seconds == 1L ? "" : "s");
        }

        if (seconds == 0L) {
            return minutes + " minute" + (minutes == 1L ? "" : "s");
        }

        return minutes
                + " minute"
                + (minutes == 1L ? "" : "s")
                + " and "
                + seconds
                + " second"
                + (seconds == 1L ? "" : "s");
    }
}