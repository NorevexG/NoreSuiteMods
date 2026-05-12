package com.nore.teams.api;

import com.nore.teams.service.NoreTeamServices;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NoreTeamsApi {
    private NoreTeamsApi() {
    }

    public static boolean isAvailable(MinecraftServer server) {
        return NoreTeamServices.teams().isAvailable(server);
    }

    public static Optional<TeamView> getPlayerTeam(ServerPlayer player) {
        return NoreTeamServices.teams().getPlayerTeam(player.server, player.getUUID());
    }

    public static Optional<TeamView> getPlayerTeam(MinecraftServer server, UUID playerId) {
        return NoreTeamServices.teams().getPlayerTeam(server, playerId);
    }

    public static Optional<TeamView> getTeamById(MinecraftServer server, UUID partyId) {
        return NoreTeamServices.teams().getTeamById(server, partyId);
    }

    public static boolean areSameTeam(MinecraftServer server, UUID firstPlayer, UUID secondPlayer) {
        return NoreTeamServices.teams().areSameTeam(server, firstPlayer, secondPlayer);
    }

    public static boolean areAllied(MinecraftServer server, UUID firstPlayer, UUID secondPlayer) {
        return NoreTeamServices.teams().areAllied(server, firstPlayer, secondPlayer);
    }

    public static Set<UUID> getTeammates(MinecraftServer server, UUID playerId) {
        LinkedHashSet<UUID> players = new LinkedHashSet<>();
        getPlayerTeam(server, playerId).ifPresent(team -> team.members().forEach(member -> {
            if (!member.playerId().equals(playerId)) {
                players.add(member.playerId());
            }
        }));
        return players;
    }

    public static Set<UUID> getAllies(MinecraftServer server, UUID playerId) {
        LinkedHashSet<UUID> players = new LinkedHashSet<>();
        Optional<TeamView> team = getPlayerTeam(server, playerId);
        if (team.isEmpty()) {
            return players;
        }
        Set<UUID> teamMembers = new LinkedHashSet<>();
        team.get().members().forEach(member -> teamMembers.add(member.playerId()));
        team.get().allies().forEach(ally -> getTeamById(server, ally.partyId())
                .ifPresent(allyTeam -> allyTeam.members().forEach(member -> {
                    if (!member.playerId().equals(playerId) && !teamMembers.contains(member.playerId())) {
                        players.add(member.playerId());
                    }
                })));
        return players;
    }

    public static Set<UUID> getRelatedPlayers(MinecraftServer server, UUID playerId, ShareScope scope) {
        LinkedHashSet<UUID> players = new LinkedHashSet<>();
        players.add(playerId);
        if (scope == ShareScope.TEAM || scope == ShareScope.ALLIED) {
            players.addAll(getTeammates(server, playerId));
        }
        if (scope == ShareScope.ALLIED) {
            players.addAll(getAllies(server, playerId));
        }
        return players;
    }

    public static Set<UUID> getRelatedPlayers(MinecraftServer server, UUID playerId, boolean includeAllies) {
        return getRelatedPlayers(server, playerId, includeAllies ? ShareScope.ALLIED : ShareScope.TEAM);
    }

    public static List<InviteView> getInvites(MinecraftServer server, UUID playerId) {
        return NoreTeamServices.teams().getInvites(server, playerId);
    }
}
