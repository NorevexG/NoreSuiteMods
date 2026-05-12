package com.nore.teams.service;

import com.nore.teams.api.InviteView;
import com.nore.teams.api.TeamView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoreTeamService {
    boolean isAvailable(MinecraftServer server);

    Optional<TeamView> getPlayerTeam(MinecraftServer server, UUID playerId);

    Optional<TeamView> getTeamById(MinecraftServer server, UUID partyId);

    boolean areSameTeam(MinecraftServer server, UUID firstPlayer, UUID secondPlayer);

    boolean areAllied(MinecraftServer server, UUID firstPlayer, UUID secondPlayer);

    boolean createParty(ServerPlayer owner);

    boolean disbandOwnedParty(ServerPlayer owner);

    boolean leaveParty(ServerPlayer player);

    boolean invite(ServerPlayer actor, ServerPlayer target);

    boolean acceptInvite(ServerPlayer player, ServerPlayer owner);

    boolean acceptInvite(ServerPlayer player, UUID partyId);

    boolean declineInvite(ServerPlayer player, UUID partyId);

    boolean kick(ServerPlayer actor, ServerPlayer target);

    boolean kick(ServerPlayer actor, UUID targetId);

    boolean addAlly(ServerPlayer actor, ServerPlayer target);

    boolean addAlly(ServerPlayer actor, UUID targetId);

    boolean removeAlly(ServerPlayer actor, ServerPlayer target);

    boolean removeAlly(ServerPlayer actor, UUID targetId);

    boolean setRank(ServerPlayer actor, UUID targetId, String rank);

    boolean transferOwnership(ServerPlayer actor, UUID targetId);

    List<InviteView> getInvites(MinecraftServer server, UUID playerId);

    boolean inviteFake(ServerPlayer actor, String name);

    boolean addFakeMember(ServerPlayer actor, String name);

    boolean removeFakeMember(ServerPlayer actor, String name);

    boolean createFakeParty(MinecraftServer server, String actorName);

    boolean disbandFakeParty(MinecraftServer server, String actorName);

    boolean fakeInvite(MinecraftServer server, String actorName, String targetName);

    boolean fakeJoin(MinecraftServer server, String actorName, String ownerName);

    boolean fakeLeave(MinecraftServer server, String actorName);

    boolean fakeAlly(MinecraftServer server, String actorName, String targetName);

    boolean fakeUnally(MinecraftServer server, String actorName, String targetName);
}
