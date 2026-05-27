package com.nore.stages.team;

import com.nore.stages.NoreStages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StageSubjectResolver {
    private static Method getPlayerTeam;
    private static Method partyId;
    private static Method getTeammates;
    private static boolean reflectionAttempted;

    private StageSubjectResolver() {
    }

    public static UUID localSubject(ServerPlayer player) {
        return player.getUUID();
    }

    public static Set<UUID> localShareTargets(ServerPlayer player) {
        Set<UUID> targets = new LinkedHashSet<>();
        targets.add(player.getUUID());
        if (!ModList.get().isLoaded("noreteams")) {
            return targets;
        }
        tryResolveReflection();
        if (getTeammates == null) {
            return targets;
        }
        try {
            Object teammates = getTeammates.invoke(null, player.server, player.getUUID());
            if (teammates instanceof Set<?> set) {
                for (Object teammate : set) {
                    if (teammate instanceof UUID uuid) {
                        targets.add(uuid);
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            NoreStages.LOGGER.warn("Unable to resolve NoreTeams share targets; only applying local stage to triggering player.", ex);
        }
        return targets;
    }

    public static Optional<UUID> teamId(MinecraftServer server, ServerPlayer player) {
        if (!ModList.get().isLoaded("noreteams")) {
            return Optional.empty();
        }
        tryResolveReflection();
        if (getPlayerTeam == null || partyId == null) {
            return Optional.empty();
        }
        try {
            Object optional = getPlayerTeam.invoke(null, player);
            if (optional instanceof Optional<?> team && team.isPresent()) {
                Object id = partyId.invoke(team.get());
                if (id instanceof UUID uuid) {
                    return Optional.of(uuid);
                }
            }
        } catch (ReflectiveOperationException ex) {
            NoreStages.LOGGER.warn("Unable to resolve NoreTeams subject; falling back to player stages.", ex);
        }
        return Optional.empty();
    }

    private static void tryResolveReflection() {
        if (reflectionAttempted) {
            return;
        }
        reflectionAttempted = true;
        try {
            Class<?> api = Class.forName("com.nore.teams.api.NoreTeamsApi");
            Class<?> teamView = Class.forName("com.nore.teams.api.TeamView");
            getPlayerTeam = api.getMethod("getPlayerTeam", ServerPlayer.class);
            getTeammates = api.getMethod("getTeammates", MinecraftServer.class, UUID.class);
            partyId = teamView.getMethod("partyId");
        } catch (ReflectiveOperationException ex) {
            NoreStages.LOGGER.warn("NoreTeams is loaded but its API shape could not be resolved.", ex);
        }
    }
}
