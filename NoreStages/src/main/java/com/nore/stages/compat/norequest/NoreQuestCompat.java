package com.nore.stages.compat.norequest;

import com.nore.stages.NoreStages;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class NoreQuestCompat {
    private static Method getMode;
    private static boolean reflectionAttempted;
    private static boolean warningLogged;

    private NoreQuestCompat() {
    }

    public static boolean shouldShareUnlock(ServerPlayer player, String targetType, String questId) {
        return !"SOLO".equals(modeName(player, targetType, questId));
    }

    public static boolean isSoloQuest(ServerPlayer player, String target) {
        if (!target.startsWith("quest:")) {
            return false;
        }
        return "SOLO".equals(modeName(player, "quest", target.substring("quest:".length())));
    }

    public static String modeName(ServerPlayer player, String targetType, String questId) {
        if (!"quest".equals(targetType) || !ModList.get().isLoaded("norequest")) {
            return "unavailable";
        }
        tryResolveReflection();
        if (getMode == null) {
            return "unresolved";
        }
        try {
            Object mode = getMode.invoke(null, player, questId);
            return mode == null ? "unknown" : mode.toString().toUpperCase(java.util.Locale.ROOT);
        } catch (ReflectiveOperationException ex) {
            logWarning("Unable to resolve NoreQuest completion mode; preserving default team sharing.", ex);
            return "error";
        }
    }

    private static void tryResolveReflection() {
        if (reflectionAttempted) {
            return;
        }
        reflectionAttempted = true;
        try {
            Class<?> api = Class.forName("com.nore.quest.api.NoreQuestApi");
            getMode = api.getMethod("getMode", ServerPlayer.class, String.class);
        } catch (ReflectiveOperationException ex) {
            logWarning("NoreQuest is loaded but its API shape could not be resolved.", ex);
        }
    }

    private static void logWarning(String message, Exception ex) {
        if (!warningLogged) {
            warningLogged = true;
            NoreStages.LOGGER.warn(message, ex);
        }
    }
}
