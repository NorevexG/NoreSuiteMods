package com.nore.stages.compat.ftb;

import com.nore.stages.NoreStages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class FtbStagesCompat {
    private static Method stageHelperGetProvider;
    private static Method stageProviderAdd;
    private static Method ftbTeamsApi;
    private static Method ftbTeamsGetManager;
    private static Method ftbTeamsGetTeamForPlayer;
    private static Method addTeamStage;
    private static Method checkStageTasks;
    private static Object stageHelperInstance;
    private static boolean reflectionAttempted;
    private static boolean warningLogged;

    private FtbStagesCompat() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("ftbquests")
                && ModList.get().isLoaded("ftblibrary")
                && ModList.get().isLoaded("ftbteams");
    }

    public static void grantQuestStage(ServerPlayer player, ResourceLocation quest) {
        if (!isAvailable()) {
            return;
        }
        tryResolveReflection();
        if (stageHelperGetProvider == null || stageProviderAdd == null) {
            return;
        }
        String stage = quest.toString();
        try {
            Object provider = stageHelperGetProvider.invoke(stageHelperInstance);
            stageProviderAdd.invoke(provider, player, stage);
            grantTeamStage(player, stage);
            checkStageTasks(player);
        } catch (ReflectiveOperationException ex) {
            logWarning("Unable to grant FTB stage " + stage + " for NoreStages quest unlock.", ex);
        }
    }

    public static String debugStatus() {
        if (!isAvailable()) {
            return "FTB quest stage bridge: unavailable";
        }
        tryResolveReflection();
        return "FTB quest stage bridge: " + (stageHelperGetProvider != null && stageProviderAdd != null ? "ready" : "unresolved");
    }

    private static void grantTeamStage(ServerPlayer player, String stage) throws ReflectiveOperationException {
        if (ftbTeamsApi == null || ftbTeamsGetManager == null || ftbTeamsGetTeamForPlayer == null || addTeamStage == null) {
            return;
        }
        Object api = ftbTeamsApi.invoke(null);
        Object manager = ftbTeamsGetManager.invoke(api);
        Object optional = ftbTeamsGetTeamForPlayer.invoke(manager, player);
        if (optional instanceof Optional<?> team && team.isPresent()) {
            addTeamStage.invoke(null, team.get(), stage);
        }
    }

    private static void checkStageTasks(ServerPlayer player) throws ReflectiveOperationException {
        if (checkStageTasks != null) {
            checkStageTasks.invoke(null, player);
        }
    }

    private static void tryResolveReflection() {
        if (reflectionAttempted) {
            return;
        }
        reflectionAttempted = true;
        try {
            Class<?> stageHelper = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageHelper");
            Class<?> stageProvider = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageProvider");
            Field instance = stageHelper.getField("INSTANCE");
            stageHelperInstance = instance.get(null);
            stageHelperGetProvider = stageHelper.getMethod("getProvider");
            stageProviderAdd = stageProvider.getMethod("add", ServerPlayer.class, String.class);

            Class<?> api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Class<?> teamManager = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            Class<?> team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
            Class<?> teamStagesHelper = Class.forName("dev.ftb.mods.ftbteams.api.TeamStagesHelper");
            ftbTeamsApi = api.getMethod("api");
            ftbTeamsGetManager = apiInterface.getMethod("getManager");
            ftbTeamsGetTeamForPlayer = teamManager.getMethod("getTeamForPlayer", ServerPlayer.class);
            addTeamStage = teamStagesHelper.getMethod("addTeamStage", team, String.class);

            Class<?> stageTask = Class.forName("dev.ftb.mods.ftbquests.quest.task.StageTask");
            checkStageTasks = stageTask.getMethod("checkStages", ServerPlayer.class);
        } catch (ReflectiveOperationException ex) {
            logWarning("FTB Quests is loaded, but NoreStages could not resolve its stage bridge API.", ex);
        }
    }

    private static void logWarning(String message, Exception ex) {
        if (!warningLogged) {
            warningLogged = true;
            NoreStages.LOGGER.warn(message, ex);
        }
    }
}
