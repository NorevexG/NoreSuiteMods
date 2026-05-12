package com.nore.teams.network;

import com.nore.teams.NoreTeams;
import com.nore.teams.api.InviteView;
import com.nore.teams.api.MemberView;
import com.nore.teams.api.NoreTeamsApi;
import com.nore.teams.api.TeamView;
import com.nore.teams.client.NoreTeamsClientState;
import com.nore.teams.service.DebugFakePlayers;
import com.nore.teams.service.NoreTeamServices;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = NoreTeams.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NoreTeamsNetwork {
    private NoreTeamsNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SnapshotRequest.TYPE, SnapshotRequest.STREAM_CODEC, NoreTeamsNetwork::handleSnapshotRequest);
        registrar.playToServer(UiActionRequest.TYPE, UiActionRequest.STREAM_CODEC, NoreTeamsNetwork::handleUiActionRequest);
        registrar.playToClient(SnapshotPayload.TYPE, SnapshotPayload.STREAM_CODEC, NoreTeamsNetwork::handleSnapshotPayload);
    }

    public static void requestSnapshot() {
        PacketDistributor.sendToServer(SnapshotRequest.INSTANCE);
    }

    public static void sendUiAction(String action, String value) {
        PacketDistributor.sendToServer(new UiActionRequest(action, value == null ? "" : value));
    }

    private static void handleSnapshotRequest(SnapshotRequest request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, buildSnapshot(player));
        }
    }

    private static void handleSnapshotPayload(SnapshotPayload payload, IPayloadContext context) {
        NoreTeamsClientState.setSnapshot(payload);
    }

    private static void handleUiActionRequest(UiActionRequest request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        boolean ok = switch (request.action()) {
            case "create" -> NoreTeamServices.teams().createParty(player);
            case "disband" -> NoreTeamServices.teams().disbandOwnedParty(player);
            case "leave" -> NoreTeamServices.teams().leaveParty(player);
            case "invite" -> inviteByName(player, request.value());
            case "join" -> parseUuid(request.value()).map(partyId -> NoreTeamServices.teams().acceptInvite(player, partyId)).orElse(false);
            case "decline" -> parseUuid(request.value()).map(partyId -> NoreTeamServices.teams().declineInvite(player, partyId)).orElse(false);
            case "kick" -> parseUuid(request.value()).map(target -> NoreTeamServices.teams().kick(player, target)).orElse(false);
            case "ally" -> {
                if (NoreTeamsApi.getPlayerTeam(player).isEmpty()) {
                    NoreTeamServices.teams().createParty(player);
                }
                yield parseUuid(request.value()).map(target -> NoreTeamServices.teams().addAlly(player, target)).orElse(false);
            }
            case "unally" -> parseUuid(request.value()).map(target -> NoreTeamServices.teams().removeAlly(player, target)).orElse(false);
            case "promote" -> parseUuid(request.value()).flatMap(target -> nextRank(player, target, true)
                    .map(rank -> NoreTeamServices.teams().setRank(player, target, rank))).orElse(false);
            case "demote" -> parseUuid(request.value()).flatMap(target -> nextRank(player, target, false)
                    .map(rank -> NoreTeamServices.teams().setRank(player, target, rank))).orElse(false);
            case "transfer" -> parseUuid(request.value()).map(target -> NoreTeamServices.teams().transferOwnership(player, target)).orElse(false);
            default -> false;
        };
        if (!ok) {
            player.sendSystemMessage(Component.literal("Could not complete that team action."));
        }
        refreshAll(player.server);
    }

    private static boolean inviteByName(ServerPlayer actor, String name) {
        ServerPlayer target = actor.server.getPlayerList().getPlayerByName(name);
        boolean invited = target != null && NoreTeamServices.teams().invite(actor, target);
        if (invited) {
            target.sendSystemMessage(Component.literal(actor.getGameProfile().getName() + " invited you to a team."));
        }
        return invited;
    }

    private static Optional<String> nextRank(ServerPlayer actor, UUID targetId, boolean promote) {
        return NoreTeamsApi.getPlayerTeam(actor)
                .flatMap(team -> team.members().stream()
                        .filter(member -> member.playerId().equals(targetId) && !member.owner())
                        .findFirst())
                .flatMap(member -> switch (member.rank().toUpperCase()) {
                    case "MEMBER" -> promote ? Optional.of("MODERATOR") : Optional.empty();
                    case "MODERATOR" -> Optional.of(promote ? "ADMIN" : "MEMBER");
                    case "ADMIN" -> promote ? Optional.empty() : Optional.of("MODERATOR");
                    default -> Optional.empty();
                });
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static void refreshAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, buildSnapshot(player)));
    }

    private static SnapshotPayload buildSnapshot(ServerPlayer player) {
        MinecraftServer server = player.server;
        UUID playerId = player.getUUID();
        Optional<TeamView> team = NoreTeamsApi.getPlayerTeam(server, playerId);
        Set<UUID> teammates = NoreTeamsApi.getTeammates(server, playerId);
        Set<UUID> allies = NoreTeamsApi.getAllies(server, playerId);
        List<PlayerEntry> players = new ArrayList<>(server.getPlayerList().getPlayers().stream()
                .map(serverPlayer -> new PlayerEntry(
                        serverPlayer.getUUID(),
                        serverPlayer.getGameProfile().getName(),
                        relation(playerId, serverPlayer.getUUID(), teammates, allies),
                        true
                ))
                .toList());
        Set<UUID> listedPlayers = new LinkedHashSet<>();
        players.forEach(entry -> listedPlayers.add(entry.id()));
        DebugFakePlayers.entries().forEach(fake -> {
            if (listedPlayers.add(fake.id())) {
                players.add(new PlayerEntry(fake.id(), fake.name(), relation(playerId, fake.id(), teammates, allies), false));
            }
        });
        players.sort(Comparator.comparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER));
        List<MemberEntry> members = team
                .map(view -> view.members().stream()
                        .map(member -> new MemberEntry(member.playerId(), member.username(), member.rank(), member.owner()))
                        .sorted(Comparator.comparing(MemberEntry::name, String.CASE_INSENSITIVE_ORDER))
                        .toList())
                .orElse(List.of());
        List<MemberEntry> allyMembers = new ArrayList<>();
        team.ifPresent(view -> {
            Set<UUID> seen = new LinkedHashSet<>();
            view.members().forEach(member -> seen.add(member.playerId()));
            view.allies().forEach(ally -> NoreTeamsApi.getTeamById(server, ally.partyId()).ifPresent(allyTeam -> allyTeam.members().forEach(member -> {
                if (seen.add(member.playerId())) {
                    allyMembers.add(new MemberEntry(member.playerId(), member.username(), allyTeam.name(), false));
                }
            })));
        });
        allyMembers.sort(Comparator.comparing(MemberEntry::name, String.CASE_INSENSITIVE_ORDER));
        List<InviteEntry> invites = NoreTeamsApi.getInvites(server, playerId).stream()
                .map(invite -> new InviteEntry(invite.partyId(), invite.partyName(), invite.ownerId(), invite.ownerName()))
                .sorted(Comparator.comparing(InviteEntry::ownerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        boolean owner = team.map(value -> value.members().stream()
                .anyMatch(member -> member.playerId().equals(playerId) && member.owner()))
                .orElse(false);
        return new SnapshotPayload(team.map(TeamView::name).orElse(""), team.isPresent(), owner, players, members, allyMembers, invites);
    }

    private static Relation relation(UUID self, UUID other, Set<UUID> teammates, Set<UUID> allies) {
        if (self.equals(other)) {
            return Relation.SELF;
        }
        if (teammates.contains(other)) {
            return Relation.TEAMMATE;
        }
        if (allies.contains(other)) {
            return Relation.ALLY;
        }
        return Relation.OTHER;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NoreTeams.MODID, path);
    }

    public record SnapshotRequest() implements CustomPacketPayload {
        public static final SnapshotRequest INSTANCE = new SnapshotRequest();
        public static final Type<SnapshotRequest> TYPE = new Type<>(id("snapshot_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UiActionRequest(String action, String value) implements CustomPacketPayload {
        public static final Type<UiActionRequest> TYPE = new Type<>(id("ui_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UiActionRequest> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> payload.write(buf),
                UiActionRequest::read
        );

        private static UiActionRequest read(FriendlyByteBuf buf) {
            return new UiActionRequest(buf.readUtf(), buf.readUtf());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(action);
            buf.writeUtf(value);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SnapshotPayload(
            String teamName,
            boolean inTeam,
            boolean owner,
            List<PlayerEntry> players,
            List<MemberEntry> teamMembers,
            List<MemberEntry> allies,
            List<InviteEntry> invites
    ) implements CustomPacketPayload {
        public static final Type<SnapshotPayload> TYPE = new Type<>(id("snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> payload.write(buf),
                SnapshotPayload::read
        );

        private static SnapshotPayload read(FriendlyByteBuf buf) {
            String teamName = buf.readUtf();
            boolean inTeam = buf.readBoolean();
            boolean owner = buf.readBoolean();
            List<PlayerEntry> players = buf.readList(PlayerEntry::read);
            List<MemberEntry> teamMembers = buf.readList(MemberEntry::read);
            List<MemberEntry> allies = buf.readList(MemberEntry::read);
            List<InviteEntry> invites = buf.readList(InviteEntry::read);
            return new SnapshotPayload(teamName, inTeam, owner, players, teamMembers, allies, invites);
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(teamName);
            buf.writeBoolean(inTeam);
            buf.writeBoolean(owner);
            buf.writeCollection(players, PlayerEntry::write);
            buf.writeCollection(teamMembers, MemberEntry::write);
            buf.writeCollection(allies, MemberEntry::write);
            buf.writeCollection(invites, InviteEntry::write);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public enum Relation {
        SELF,
        TEAMMATE,
        ALLY,
        OTHER
    }

    public record PlayerEntry(UUID id, String name, Relation relation, boolean online) {
        private static PlayerEntry read(FriendlyByteBuf buf) {
            return new PlayerEntry(buf.readUUID(), buf.readUtf(), buf.readEnum(Relation.class), buf.readBoolean());
        }

        private static void write(FriendlyByteBuf buf, PlayerEntry entry) {
            buf.writeUUID(entry.id);
            buf.writeUtf(entry.name);
            buf.writeEnum(entry.relation);
            buf.writeBoolean(entry.online);
        }
    }

    public record MemberEntry(UUID id, String name, String role, boolean owner) {
        private static MemberEntry read(FriendlyByteBuf buf) {
            return new MemberEntry(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
        }

        private static void write(FriendlyByteBuf buf, MemberEntry entry) {
            buf.writeUUID(entry.id);
            buf.writeUtf(entry.name);
            buf.writeUtf(entry.role);
            buf.writeBoolean(entry.owner);
        }
    }

    public record InviteEntry(UUID partyId, String partyName, UUID ownerId, String ownerName) {
        private static InviteEntry read(FriendlyByteBuf buf) {
            return new InviteEntry(buf.readUUID(), buf.readUtf(), buf.readUUID(), buf.readUtf());
        }

        private static void write(FriendlyByteBuf buf, InviteEntry entry) {
            buf.writeUUID(entry.partyId);
            buf.writeUtf(entry.partyName);
            buf.writeUUID(entry.ownerId);
            buf.writeUtf(entry.ownerName);
        }
    }
}
