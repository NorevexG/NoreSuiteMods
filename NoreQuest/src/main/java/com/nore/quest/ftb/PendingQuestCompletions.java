package com.nore.quest.ftb;

import com.nore.quest.NoreQuest;
import com.nore.quest.api.QuestCompletionMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public final class PendingQuestCompletions {
    private PendingQuestCompletions() {
    }

    public static void record(MinecraftServer server, UUID playerId, String questId, QuestCompletionMode mode) {
        Properties properties = load(server);
        String key = playerId.toString();
        Set<String> entries = decodeEntries(properties.getProperty(key, ""));
        entries.add(mode.name() + "\t" + Base64.getEncoder().encodeToString(questId.getBytes(StandardCharsets.UTF_8)));
        properties.setProperty(key, String.join(";", entries));
        save(server, properties);
    }

    public static void apply(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Properties properties = load(player.server);
        String key = player.getUUID().toString();
        Set<String> entries = decodeEntries(properties.getProperty(key, ""));
        if (entries.isEmpty()) {
            return;
        }
        properties.remove(key);
        save(player.server, properties);
        for (String entry : entries) {
            String[] parts = entry.split("\t", 2);
            if (parts.length != 2) {
                continue;
            }
            String questId = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            QuestShareService.runFtbComplete(player.server, player, questId);
        }
    }

    private static Set<String> decodeEntries(String value) {
        Set<String> entries = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return entries;
        }
        for (String entry : value.split(";")) {
            if (!entry.isBlank()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Properties load(MinecraftServer server) {
        Path file = file(server);
        Properties properties = new Properties();
        if (!Files.exists(file)) {
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException ex) {
            NoreQuest.LOGGER.warn("Could not load pending quest completions", ex);
        }
        return properties;
    }

    private static void save(MinecraftServer server, Properties properties) {
        Path file = file(server);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Nore Quest pending offline completions");
            }
        } catch (IOException ex) {
            NoreQuest.LOGGER.warn("Could not save pending quest completions", ex);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("norequest_pending.properties");
    }
}
