package com.nore.quest.ftb;

import com.nore.quest.NoreQuest;
import com.nore.quest.api.QuestCompletionMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class QuestModeStore {
    private static final Map<Path, Properties> CACHE = new HashMap<>();

    private QuestModeStore() {
    }

    public static QuestCompletionMode get(MinecraftServer server, String questId) {
        return QuestCompletionMode.parse(properties(server).getProperty(questId, QuestCompletionMode.SOLO.name()));
    }

    public static boolean contains(MinecraftServer server, String questId) {
        return properties(server).containsKey(questId);
    }

    public static void set(MinecraftServer server, String questId, QuestCompletionMode mode) {
        Properties properties = properties(server);
        properties.setProperty(questId, mode.name());
        save(server, properties);
    }

    private static Properties properties(MinecraftServer server) {
        Path path = file(server);
        return CACHE.computeIfAbsent(path, ignored -> {
            Properties properties = new Properties();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    properties.load(reader);
                } catch (IOException ex) {
                    NoreQuest.LOGGER.warn("Could not load Nore Quest mode store", ex);
                }
            }
            return properties;
        });
    }

    private static void save(MinecraftServer server, Properties properties) {
        Path path = file(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "Nore Quest completion modes");
            }
        } catch (IOException ex) {
            NoreQuest.LOGGER.warn("Could not save Nore Quest mode store", ex);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("norequest_modes.properties");
    }
}
