package com.wufmc.wufperms.config;

public final class WUFPermsConfig {
    private WUFPermsConfig() {}

    /**
     * Five minutes expressed in milliseconds.
     */
    public static final long TELEPORT_DAMAGE_COOLDOWN_MILLIS = 5L * 60L * 1000L;

    /**
     * Horizontal hostile-mob detection range.
     */
    public static final double HOSTILE_RANGE_HORIZONTAL = 8.0D;

    /**
     * Vertical hostile-mob detection range.
     */
    public static final double HOSTILE_RANGE_VERTICAL = 5.0D;

    /**
     * Check hostile mobs around the teleport destination.
     */
    public static final boolean CHECK_DESTINATION_FOR_HOSTILES = true;
}
