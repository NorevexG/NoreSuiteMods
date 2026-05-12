package com.nore.quest.api;

import java.util.Locale;

public enum QuestCompletionMode {
    SOLO(false),
    TEAM(false),
    ALLIED(true);

    private final boolean includeAllies;

    QuestCompletionMode(boolean includeAllies) {
        this.includeAllies = includeAllies;
    }

    public boolean includeAllies() {
        return includeAllies;
    }

    public static QuestCompletionMode parse(String value) {
        if (value == null || value.isBlank()) {
            return SOLO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return SOLO;
        }
    }
}
