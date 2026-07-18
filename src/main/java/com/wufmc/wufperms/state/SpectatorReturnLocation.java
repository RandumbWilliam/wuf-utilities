package com.wufmc.wufperms.state;

import net.minecraft.server.level.ServerPlayer;

public record SpectatorReturnLocation(
        String dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public static SpectatorReturnLocation fromPlayer(ServerPlayer player) {
        String dimensionId = player.level()
                .dimension()
                .identifier()
                .toString();

        return new SpectatorReturnLocation(
                dimensionId,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }
}
