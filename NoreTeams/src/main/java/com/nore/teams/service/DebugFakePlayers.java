package com.nore.teams.service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DebugFakePlayers {
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private DebugFakePlayers() {
    }

    public static Optional<Entry> add(String name) {
        return cleanName(name).map(clean -> {
            String key = clean.toLowerCase(Locale.ROOT);
            Entry entry = new Entry(fakeId(clean), clean);
            ENTRIES.put(key, entry);
            return entry;
        });
    }

    public static Optional<Entry> get(String name) {
        return cleanName(name).flatMap(clean -> Optional.ofNullable(ENTRIES.get(clean.toLowerCase(Locale.ROOT))));
    }

    public static Optional<Entry> get(UUID id) {
        return ENTRIES.values().stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    public static Collection<Entry> entries() {
        return java.util.List.copyOf(ENTRIES.values());
    }

    public static Optional<String> cleanName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 16 || !trimmed.matches("[A-Za-z0-9_]+")) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    private static UUID fakeId(String name) {
        return UUID.nameUUIDFromBytes(("noreteams:fake:" + name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    public record Entry(UUID id, String name) {
    }
}
