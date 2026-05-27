package com.nore.stages.stage;

import java.util.Locale;

public enum StageScope {
    GLOBAL,
    LOCAL;

    public static StageScope parse(String value, StageScope fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "global" -> GLOBAL;
            case "local", "team", "player" -> LOCAL;
            default -> fallback;
        };
    }
}
