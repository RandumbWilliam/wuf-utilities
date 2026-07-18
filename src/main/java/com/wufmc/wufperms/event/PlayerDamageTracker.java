package com.wufmc.wufperms.event;

import com.wufmc.wufperms.config.WUFPermsConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDamageTracker {
    private static final Map<UUID, Long> LAST_DAMAGE_TIMES = new ConcurrentHashMap<>();

    private PlayerDamageTracker() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, damageSource, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer player && damageTaken > 0.0f) {
                recordDamage(player);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                recordDamage(player);
            }
        });
    }

    public static void recordDamage(ServerPlayer player) {
        LAST_DAMAGE_TIMES.put(player.getUUID(), System.currentTimeMillis());
    }

    public static boolean mayTeleport(ServerPlayer player) {
        Long lastDamageTime = LAST_DAMAGE_TIMES.get(player.getUUID());

        if (lastDamageTime == null) {
            return true;
        }

        long elapsed = System.currentTimeMillis() - lastDamageTime;

        return elapsed >=  WUFPermsConfig.TELEPORT_DAMAGE_COOLDOWN_MILLIS;
    }

    public static long getRemainingCooldownSeconds(ServerPlayer player) {
        Long lastDamageTime = LAST_DAMAGE_TIMES.get(player.getUUID());

        if (lastDamageTime == null) {
            return 0L;
        }

        long elapsed = System.currentTimeMillis() - lastDamageTime;

        long remainingMillis = WUFPermsConfig.TELEPORT_DAMAGE_COOLDOWN_MILLIS - elapsed;

        if (remainingMillis <= 0L) {
            return 0L;
        }

        return (remainingMillis + 999L) / 1000L;
    }

    public static void clear(UUID playerId) {
        LAST_DAMAGE_TIMES.remove(playerId);
    }
}
