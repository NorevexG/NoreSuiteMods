package com.nore.teams.service;

import com.nore.teams.NoreTeams;
import com.nore.teams.api.AllyView;
import com.nore.teams.api.InviteView;
import com.nore.teams.api.MemberView;
import com.nore.teams.api.TeamView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class OpenPacReflectiveTeamService implements NoreTeamService {
    private static final String OPENPAC_SERVER_API = "xaero.pac.common.server.api.OpenPACServerAPI";

    @Override
    public boolean isAvailable(MinecraftServer server) {
        return manager(server).isPresent();
    }

    @Override
    public Optional<TeamView> getPlayerTeam(MinecraftServer server, UUID playerId) {
        return manager(server)
                .flatMap(manager -> Reflect.call(manager, "getPartyByMember", playerId))
                .flatMap(party -> toTeamView(server, party));
    }

    @Override
    public Optional<TeamView> getTeamById(MinecraftServer server, UUID partyId) {
        return manager(server)
                .flatMap(manager -> Reflect.call(manager, "getPartyById", partyId))
                .flatMap(party -> toTeamView(server, party));
    }

    @Override
    public boolean areSameTeam(MinecraftServer server, UUID firstPlayer, UUID secondPlayer) {
        Optional<TeamView> team = getPlayerTeam(server, firstPlayer);
        return team.isPresent() && team.get().contains(secondPlayer);
    }

    @Override
    public boolean areAllied(MinecraftServer server, UUID firstPlayer, UUID secondPlayer) {
        Optional<TeamView> firstTeam = getPlayerTeam(server, firstPlayer);
        Optional<TeamView> secondTeam = getPlayerTeam(server, secondPlayer);
        if (firstTeam.isEmpty() || secondTeam.isEmpty()) {
            return false;
        }
        UUID secondPartyId = secondTeam.get().partyId();
        return firstTeam.get().allies().stream().anyMatch(ally -> ally.partyId().equals(secondPartyId));
    }

    @Override
    public boolean createParty(ServerPlayer owner) {
        return manager(owner.server)
                .flatMap(manager -> Reflect.call(manager, "createPartyForOwner", owner))
                .isPresent();
    }

    @Override
    public boolean disbandOwnedParty(ServerPlayer owner) {
        Optional<Object> manager = manager(owner.server);
        Optional<Object> party = manager.flatMap(value -> Reflect.call(value, "getPartyByOwner", owner.getUUID()));
        party.ifPresent(value -> manager.ifPresent(m -> Reflect.call(m, "removeParty", value)));
        return party.isPresent();
    }

    @Override
    public boolean leaveParty(ServerPlayer player) {
        Optional<Object> party = memberParty(player);
        Optional<UUID> ownerId = party.flatMap(this::ownerId);
        if (party.isEmpty() || ownerId.isEmpty() || ownerId.get().equals(player.getUUID())) {
            return false;
        }
        Reflect.call(party.get(), "removeMember", player.getUUID());
        return true;
    }

    @Override
    public boolean invite(ServerPlayer actor, ServerPlayer target) {
        return actorParty(actor)
                .flatMap(party -> Reflect.call(party, "invitePlayer", target.getUUID(), target.getGameProfile().getName()))
                .isPresent();
    }

    @Override
    public boolean acceptInvite(ServerPlayer player, ServerPlayer owner) {
        Optional<Object> party = manager(player.server).flatMap(manager -> Reflect.call(manager, "getPartyByOwner", owner.getUUID()));
        if (party.isEmpty() || !asBoolean(Reflect.call(party.get(), "isInvited", player.getUUID()).orElse(false))) {
            return false;
        }
        Optional<Object> member = Reflect.call(party.get(), "addMember", player.getUUID(), null, player.getGameProfile().getName());
        Reflect.call(party.get(), "uninvitePlayer", player.getUUID());
        return member.isPresent();
    }

    @Override
    public boolean acceptInvite(ServerPlayer player, UUID partyId) {
        Optional<Object> party = manager(player.server).flatMap(manager -> Reflect.call(manager, "getPartyById", partyId));
        if (party.isEmpty() || !asBoolean(Reflect.call(party.get(), "isInvited", player.getUUID()).orElse(false))) {
            return false;
        }
        Optional<Object> member = Reflect.call(party.get(), "addMember", player.getUUID(), null, player.getGameProfile().getName());
        Reflect.call(party.get(), "uninvitePlayer", player.getUUID());
        return member.isPresent();
    }

    @Override
    public boolean declineInvite(ServerPlayer player, UUID partyId) {
        Optional<Object> party = manager(player.server).flatMap(manager -> Reflect.call(manager, "getPartyById", partyId));
        if (party.isEmpty() || !asBoolean(Reflect.call(party.get(), "isInvited", player.getUUID()).orElse(false))) {
            return false;
        }
        Reflect.call(party.get(), "uninvitePlayer", player.getUUID());
        return true;
    }

    @Override
    public boolean kick(ServerPlayer actor, ServerPlayer target) {
        return kick(actor, target.getUUID());
    }

    @Override
    public boolean kick(ServerPlayer actor, UUID targetId) {
        return actorParty(actor)
                .flatMap(party -> Reflect.call(party, "removeMember", targetId))
                .isPresent();
    }

    @Override
    public boolean addAlly(ServerPlayer actor, ServerPlayer target) {
        return addAlly(actor, target.getUUID());
    }

    @Override
    public boolean addAlly(ServerPlayer actor, UUID targetId) {
        Optional<Object> actorParty = actorParty(actor);
        Optional<Object> targetParty = manager(actor.server).flatMap(manager -> Reflect.call(manager, "getPartyByMember", targetId));
        Optional<UUID> targetPartyId = targetParty.flatMap(this::partyId);
        if (actorParty.isEmpty() || targetParty.isEmpty() || targetPartyId.isEmpty()) {
            return false;
        }
        Reflect.call(actorParty.get(), "addAllyParty", targetPartyId.get());
        return true;
    }

    @Override
    public boolean removeAlly(ServerPlayer actor, ServerPlayer target) {
        return removeAlly(actor, target.getUUID());
    }

    @Override
    public boolean removeAlly(ServerPlayer actor, UUID targetId) {
        Optional<Object> actorParty = actorParty(actor);
        Optional<Object> targetParty = manager(actor.server).flatMap(manager -> Reflect.call(manager, "getPartyByMember", targetId));
        Optional<UUID> targetPartyId = targetParty.flatMap(this::partyId);
        if (actorParty.isEmpty() || targetParty.isEmpty() || targetPartyId.isEmpty()) {
            return false;
        }
        Reflect.call(actorParty.get(), "removeAllyParty", targetPartyId.get());
        return true;
    }

    @Override
    public boolean setRank(ServerPlayer actor, UUID targetId, String rank) {
        Optional<Object> party = actorParty(actor);
        if (party.isEmpty() || targetId.equals(actor.getUUID())) {
            return false;
        }
        Optional<Object> member = memberById(party.get(), targetId);
        if (member.isEmpty() || asBoolean(Reflect.call(member.get(), "isOwner").orElse(false))) {
            return false;
        }
        try {
            Class<?> rankClass = Class.forName("xaero.pac.common.parties.party.member.PartyMemberRank");
            Object rankValue = Enum.valueOf((Class<? extends Enum>) rankClass.asSubclass(Enum.class), rank.toUpperCase());
            return asBoolean(Reflect.call(party.get(), "setRankTyped", member.get(), rankValue).orElse(false));
        } catch (IllegalArgumentException | ReflectiveOperationException ex) {
            return false;
        }
    }

    @Override
    public boolean transferOwnership(ServerPlayer actor, UUID targetId) {
        Optional<Object> party = actorParty(actor);
        if (party.isEmpty() || targetId.equals(actor.getUUID())) {
            return false;
        }
        Optional<Object> member = memberById(party.get(), targetId);
        if (member.isEmpty() || asBoolean(Reflect.call(member.get(), "isOwner").orElse(false))) {
            return false;
        }
        String username = stringFrom(member.get(), "getPlayerUsername", "getUsername", "getName").orElse(null);
        return username != null && asBoolean(Reflect.call(party.get(), "changeOwner", targetId, username).orElse(false));
    }

    @Override
    public List<InviteView> getInvites(MinecraftServer server, UUID playerId) {
        Optional<Object> manager = manager(server);
        if (manager.isEmpty()) {
            return List.of();
        }
        List<InviteView> invites = new ArrayList<>();
        streamFrom(Reflect.call(manager.get(), "getAllStream").orElse(null)).forEach(party -> {
            if (!asBoolean(Reflect.call(party, "isInvited", playerId).orElse(false))) {
                return;
            }
            Optional<UUID> id = partyId(party);
            if (id.isEmpty()) {
                return;
            }
            UUID ownerId = ownerId(party).orElse(new UUID(0L, 0L));
            String partyName = Reflect.call(party, "getDefaultName").map(Object::toString).orElse(id.get().toString());
            String ownerName = Reflect.call(party, "getOwner")
                    .flatMap(owner -> stringFrom(owner, "getUsername", "getPlayerUsername", "getName"))
                    .orElse("Unknown");
            invites.add(new InviteView(id.get(), partyName, ownerId, ownerName));
        });
        return List.copyOf(invites);
    }

    @Override
    public boolean inviteFake(ServerPlayer actor, String name) {
        Optional<Object> party = actorParty(actor);
        Optional<String> username = DebugFakePlayers.cleanName(name);
        if (party.isEmpty() || username.isEmpty()) {
            return false;
        }
        UUID id = DebugFakePlayers.add(username.get()).map(DebugFakePlayers.Entry::id).orElse(null);
        return id != null && Reflect.call(party.get(), "invitePlayer", id, username.get()).isPresent();
    }

    @Override
    public boolean addFakeMember(ServerPlayer actor, String name) {
        Optional<Object> party = actorParty(actor);
        Optional<String> username = DebugFakePlayers.cleanName(name);
        if (party.isEmpty() || username.isEmpty()) {
            return false;
        }
        UUID id = DebugFakePlayers.add(username.get()).map(DebugFakePlayers.Entry::id).orElse(null);
        return id != null && Reflect.call(party.get(), "addMember", id, null, username.get()).isPresent();
    }

    @Override
    public boolean removeFakeMember(ServerPlayer actor, String name) {
        Optional<Object> party = memberParty(actor);
        Optional<DebugFakePlayers.Entry> fake = DebugFakePlayers.get(name);
        if (party.isEmpty() || fake.isEmpty()) {
            return false;
        }
        Reflect.call(party.get(), "removeMember", fake.get().id());
        return true;
    }

    @Override
    public boolean createFakeParty(MinecraftServer server, String actorName) {
        Optional<DebugFakePlayers.Entry> fake = DebugFakePlayers.add(actorName);
        Optional<Object> manager = manager(server);
        if (fake.isEmpty() || manager.isEmpty()
                || Reflect.call(manager.get(), "getPartyByOwner", fake.get().id()).isPresent()
                || Reflect.call(manager.get(), "getPartyByMember", fake.get().id()).isPresent()) {
            return false;
        }
        try {
            Class<?> memberClass = Class.forName("xaero.pac.common.parties.party.member.PartyMember");
            Object owner = memberClass.getConstructor(UUID.class, boolean.class).newInstance(fake.get().id(), true);
            Class<?> rankClass = Class.forName("xaero.pac.common.parties.party.member.PartyMemberRank");
            Object admin = Enum.valueOf((Class<? extends Enum>) rankClass.asSubclass(Enum.class), "ADMIN");
            Reflect.call(owner, "setRank", admin);
            Reflect.call(owner, "setUsername", fake.get().name());

            Class<?> builderClass = Class.forName("xaero.pac.common.server.parties.party.ServerParty$Builder");
            Object builder = Reflect.call(builderClass, "begin").orElse(null);
            if (builder == null) {
                return false;
            }
            UUID partyId;
            do {
                partyId = UUID.randomUUID();
            } while (Reflect.call(manager.get(), "getPartyById", partyId).isPresent());
            Reflect.call(builder, "setManagedBy", manager.get());
            Reflect.call(builder, "setOwner", owner);
            Reflect.call(builder, "setId", partyId);
            Object party = Reflect.call(builder, "build").orElse(null);
            if (party == null) {
                return false;
            }
            Reflect.call(manager.get(), "addParty", party);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            NoreTeams.LOGGER.warn("Could not create fake OPAC party for {}", actorName, ex);
            return false;
        }
    }

    @Override
    public boolean disbandFakeParty(MinecraftServer server, String actorName) {
        Optional<DebugFakePlayers.Entry> fake = DebugFakePlayers.get(actorName);
        Optional<Object> manager = manager(server);
        Optional<Object> party = manager.flatMap(value -> fake.flatMap(entry -> Reflect.call(value, "getPartyByOwner", entry.id())));
        party.ifPresent(value -> manager.ifPresent(m -> Reflect.call(m, "removeParty", value)));
        return party.isPresent();
    }

    @Override
    public boolean fakeInvite(MinecraftServer server, String actorName, String targetName) {
        Optional<Object> party = fakeActorParty(server, actorName);
        Optional<PlayerRef> target = resolvePlayer(server, targetName);
        if (party.isEmpty() || target.isEmpty()) {
            return false;
        }
        return Reflect.call(party.get(), "invitePlayer", target.get().id(), target.get().name()).isPresent();
    }

    @Override
    public boolean fakeJoin(MinecraftServer server, String actorName, String ownerName) {
        Optional<DebugFakePlayers.Entry> actor = DebugFakePlayers.get(actorName);
        Optional<PlayerRef> owner = resolvePlayer(server, ownerName);
        Optional<Object> manager = manager(server);
        Optional<Object> party = manager.flatMap(value -> owner.flatMap(ownerRef -> Reflect.call(value, "getPartyByOwner", ownerRef.id())));
        if (actor.isEmpty() || party.isEmpty() || !asBoolean(Reflect.call(party.get(), "isInvited", actor.get().id()).orElse(false))) {
            return false;
        }
        Optional<Object> member = Reflect.call(party.get(), "addMember", actor.get().id(), null, actor.get().name());
        Reflect.call(party.get(), "uninvitePlayer", actor.get().id());
        return member.isPresent();
    }

    @Override
    public boolean fakeLeave(MinecraftServer server, String actorName) {
        Optional<DebugFakePlayers.Entry> actor = DebugFakePlayers.get(actorName);
        Optional<Object> party = fakeMemberParty(server, actorName);
        Optional<UUID> ownerId = party.flatMap(this::ownerId);
        if (actor.isEmpty() || party.isEmpty() || ownerId.isEmpty() || ownerId.get().equals(actor.get().id())) {
            return false;
        }
        Reflect.call(party.get(), "removeMember", actor.get().id());
        return true;
    }

    @Override
    public boolean fakeAlly(MinecraftServer server, String actorName, String targetName) {
        Optional<Object> actorParty = fakeMemberParty(server, actorName);
        Optional<PlayerRef> target = resolvePlayer(server, targetName);
        Optional<Object> targetParty = manager(server).flatMap(manager -> target.flatMap(targetRef -> Reflect.call(manager, "getPartyByMember", targetRef.id())));
        Optional<UUID> targetPartyId = targetParty.flatMap(this::partyId);
        if (actorParty.isEmpty() || targetPartyId.isEmpty()) {
            return false;
        }
        Reflect.call(actorParty.get(), "addAllyParty", targetPartyId.get());
        return true;
    }

    @Override
    public boolean fakeUnally(MinecraftServer server, String actorName, String targetName) {
        Optional<Object> actorParty = fakeMemberParty(server, actorName);
        Optional<PlayerRef> target = resolvePlayer(server, targetName);
        Optional<Object> targetParty = manager(server).flatMap(manager -> target.flatMap(targetRef -> Reflect.call(manager, "getPartyByMember", targetRef.id())));
        Optional<UUID> targetPartyId = targetParty.flatMap(this::partyId);
        if (actorParty.isEmpty() || targetPartyId.isEmpty()) {
            return false;
        }
        Reflect.call(actorParty.get(), "removeAllyParty", targetPartyId.get());
        return true;
    }

    private Optional<Object> actorParty(ServerPlayer actor) {
        return manager(actor.server).flatMap(manager -> Reflect.call(manager, "getPartyByOwner", actor.getUUID()));
    }

    private Optional<Object> memberParty(ServerPlayer player) {
        return manager(player.server).flatMap(manager -> Reflect.call(manager, "getPartyByMember", player.getUUID()));
    }

    private Optional<Object> fakeActorParty(MinecraftServer server, String actorName) {
        return DebugFakePlayers.get(actorName)
                .flatMap(actor -> manager(server).flatMap(manager -> Reflect.call(manager, "getPartyByOwner", actor.id())));
    }

    private Optional<Object> fakeMemberParty(MinecraftServer server, String actorName) {
        return DebugFakePlayers.get(actorName)
                .flatMap(actor -> manager(server).flatMap(manager -> Reflect.call(manager, "getPartyByMember", actor.id())));
    }

    private Optional<Object> manager(MinecraftServer server) {
        try {
            Class<?> apiClass = Class.forName(OPENPAC_SERVER_API);
            Method get = apiClass.getMethod("get", MinecraftServer.class);
            Object api = get.invoke(null, server);
            return Reflect.call(api, "getPartyManager");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<TeamView> toTeamView(MinecraftServer server, Object party) {
        Optional<UUID> id = partyId(party);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        String name = Reflect.call(party, "getDefaultName").map(Object::toString).orElse(id.get().toString());
        List<MemberView> members = members(server, party);
        List<AllyView> allies = allies(party);
        return Optional.of(new TeamView(id.get(), name, members, allies));
    }

    private List<MemberView> members(MinecraftServer server, Object party) {
        List<MemberView> members = new ArrayList<>();
        streamFrom(Reflect.call(party, "getMemberInfoStream").orElse(null)).forEach(member -> {
            UUID id = uuidFrom(member).orElse(null);
            if (id == null) {
                return;
            }
            String username = stringFrom(member, "getPlayerUsername", "getUsername", "getName").orElseGet(() -> {
                ServerPlayer online = server.getPlayerList().getPlayer(id);
                return online == null ? id.toString() : online.getGameProfile().getName();
            });
            String rank = Reflect.call(member, "getRank").map(Object::toString).orElse("member");
            boolean owner = asBoolean(Reflect.call(member, "isOwner").orElse(false));
            members.add(new MemberView(id, username, rank, owner));
        });
        return List.copyOf(members);
    }

    private Optional<Object> memberById(Object party, UUID playerId) {
        return streamFrom(Reflect.call(party, "getMemberInfoStream").orElse(null))
                .filter(member -> uuidFrom(member).map(playerId::equals).orElse(false))
                .map(member -> (Object) member)
                .findFirst();
    }

    private List<AllyView> allies(Object party) {
        List<AllyView> allies = new ArrayList<>();
        streamFrom(Reflect.call(party, "getAllyPartiesStream").orElse(null)).forEach(ally -> {
            Optional<UUID> id = uuidFrom(ally);
            id.ifPresent(uuid -> allies.add(new AllyView(uuid, stringFrom(ally, "getName", "getDefaultName").orElse(uuid.toString()))));
        });
        return List.copyOf(allies);
    }

    private Stream<?> streamFrom(Object object) {
        return object instanceof Stream<?> stream ? stream : Stream.empty();
    }

    private Optional<UUID> partyId(Object party) {
        return uuidFrom(party);
    }

    private Optional<UUID> ownerId(Object party) {
        return Reflect.call(party, "getOwner").flatMap(this::uuidFrom);
    }

    private Optional<UUID> uuidFrom(Object object) {
        for (String method : List.of("getId", "getPartyId", "getPartyID", "getPlayerId", "getPlayerUUID", "getUUID")) {
            Optional<Object> value = Reflect.call(object, method);
            if (value.orElse(null) instanceof UUID uuid) {
                return Optional.of(uuid);
            }
        }
        return Optional.empty();
    }

    private Optional<String> stringFrom(Object object, String... methods) {
        for (String method : methods) {
            Optional<Object> value = Reflect.call(object, method);
            if (value.isPresent()) {
                return Optional.of(value.get().toString());
            }
        }
        return Optional.empty();
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean b && b;
    }

    private Optional<PlayerRef> resolvePlayer(MinecraftServer server, String name) {
        String trimmed = name == null ? "" : name.trim();
        ServerPlayer online = server.getPlayerList().getPlayerByName(trimmed);
        if (online != null) {
            return Optional.of(new PlayerRef(online.getUUID(), online.getGameProfile().getName()));
        }
        return DebugFakePlayers.get(trimmed).map(entry -> new PlayerRef(entry.id(), entry.name()));
    }

    private record PlayerRef(UUID id, String name) {
    }
}
