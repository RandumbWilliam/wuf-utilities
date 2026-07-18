package com.wufmc.wufperms.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.wufmc.wufperms.WUFPerms;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SpectatorReturnState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path SAVE_PATH = FabricLoader.getInstance().getConfigDir().resolve("wufperms-spectator-locations.json");

    private static final Map<UUID, SpectatorReturnLocation> LOCATIONS = new HashMap<>();

    private SpectatorReturnState() {}

    public static synchronized boolean hasLocation(UUID playerId) {
        return LOCATIONS.containsKey(playerId);
    }

    public static synchronized Optional<SpectatorReturnLocation> getLocation(UUID playerId) {
        return Optional.ofNullable(LOCATIONS.get(playerId));
    }

    /**
     * Saves only when no location is already present.
     *
     * This prevents a player from replacing their original return position
     * by running /spectate repeatedly.
     */
    public static synchronized boolean saveLocation(UUID playerId, SpectatorReturnLocation location) {
        if (LOCATIONS.containsKey(playerId)) {
            return false;
        }

        LOCATIONS.put(playerId, location);
        save();

        return true;
    }

    public static synchronized void removeLocation(UUID playerId) {
        LOCATIONS.remove(playerId);
        save();
    }

    public static synchronized void load() {
        LOCATIONS.clear();

        if (!Files.exists(SAVE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
            SaveFile saveFile = GSON.fromJson(reader, SaveFile.class);

            if (saveFile == null || saveFile.locations == null) {
                return;
            }

            for (Map.Entry<String, SpectatorReturnLocation> entry : saveFile.locations.entrySet()) {
                try {
                    UUID playerId = UUID.fromString(entry.getKey());
                    SpectatorReturnLocation location = entry.getValue();

                    if (location != null) {
                        LOCATIONS.put(playerId, location);
                    }
                } catch (IllegalArgumentException exception) {
                    WUFPerms.LOGGER.warn(
                            "Ignoring invalid player UUID in {}: {}",
                            SAVE_PATH,
                            entry.getKey()
                    );
                }
            }
        } catch (IOException | JsonSyntaxException exception) {
            WUFPerms.LOGGER.error(
                    "Could not load spectator return locations from {}.",
                    SAVE_PATH,
                    exception
            );
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(SAVE_PATH.getParent());

            Path temporaryPath = SAVE_PATH.resolveSibling(SAVE_PATH.getFileName() + ".tmp");

            Map<String, SpectatorReturnLocation> serializedLocations = new HashMap<>();

            for (Map.Entry<UUID, SpectatorReturnLocation> entry : LOCATIONS.entrySet()) {
                serializedLocations.put(entry.getKey().toString(), entry.getValue());
            }

            SaveFile saveFile = new SaveFile();
            saveFile.locations = serializedLocations;

            try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
                GSON.toJson(saveFile, writer);
            }

            try {
                Files.move(
                        temporaryPath,
                        SAVE_PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicMoveException) {
                /*
                 * Some filesystems do not support atomic moves.
                 */
                Files.move(
                        temporaryPath,
                        SAVE_PATH,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            WUFPerms.LOGGER.error(
                    "Could not save spectator return locations to {}.",
                    SAVE_PATH,
                    exception
            );
        }
    }

    private static final class SaveFile {
        private Map<String, SpectatorReturnLocation> locations = new HashMap<>();
    }
}