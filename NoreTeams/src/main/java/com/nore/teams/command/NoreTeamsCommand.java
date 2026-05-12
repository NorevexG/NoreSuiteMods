package com.nore.teams.command;

import com.mojang.brigadier.CommandDispatcher;
import com.nore.teams.NoreTeamsConfig;
import com.nore.teams.api.AllyView;
import com.nore.teams.api.MemberView;
import com.nore.teams.api.NoreTeamsApi;
import com.nore.teams.api.TeamView;
import com.nore.teams.service.DebugFakePlayers;
import com.nore.teams.service.NoreTeamServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NoreTeamsCommand {
    private NoreTeamsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("noreteams")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("same")
                        .then(Commands.literal("team")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> sameTeam(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("allied")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> sameAllied(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2) && NoreTeamsConfig.enableDebugCommands())
                        .then(Commands.literal("listplayers")
                                .executes(ctx -> debugListPlayers(ctx.getSource())))
                        .then(Commands.literal("listfake")
                                .executes(ctx -> debugListFake(ctx.getSource())))
                        .then(Commands.literal("listteams")
                                .executes(ctx -> debugListTeams(ctx.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> debugInspect(ctx.getSource(), EntityArgument.getPlayer(ctx, "player").getUUID(), EntityArgument.getPlayer(ctx, "player").getGameProfile().getName()))))
                        .then(Commands.literal("inspectid")
                                .then(Commands.argument("player", UuidArgument.uuid())
                                        .executes(ctx -> debugInspect(ctx.getSource(), UuidArgument.getUuid(ctx, "player"), UuidArgument.getUuid(ctx, "player").toString()))))
                        .then(Commands.literal("inspectfake")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> debugInspectFake(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("invitefake")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> inviteFake(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("addfake")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> addFake(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("removefake")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> removeFake(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("fake")
                                .then(Commands.argument("actor", StringArgumentType.word())
                                        .then(Commands.literal("create")
                                                .executes(ctx -> fakeCreate(ctx.getSource(), StringArgumentType.getString(ctx, "actor"))))
                                        .then(Commands.literal("disband")
                                                .executes(ctx -> fakeDisband(ctx.getSource(), StringArgumentType.getString(ctx, "actor"))))
                                        .then(Commands.literal("leave")
                                                .executes(ctx -> fakeLeave(ctx.getSource(), StringArgumentType.getString(ctx, "actor"))))
                                        .then(Commands.literal("invite")
                                                .then(Commands.argument("target", StringArgumentType.word())
                                                        .executes(ctx -> fakeInvite(ctx.getSource(), StringArgumentType.getString(ctx, "actor"), StringArgumentType.getString(ctx, "target")))))
                                        .then(Commands.literal("join")
                                                .then(Commands.argument("owner", StringArgumentType.word())
                                                        .executes(ctx -> fakeJoin(ctx.getSource(), StringArgumentType.getString(ctx, "actor"), StringArgumentType.getString(ctx, "owner")))))
                                        .then(Commands.literal("ally")
                                                .then(Commands.argument("target", StringArgumentType.word())
                                                        .executes(ctx -> fakeAlly(ctx.getSource(), StringArgumentType.getString(ctx, "actor"), StringArgumentType.getString(ctx, "target")))))
                                        .then(Commands.literal("unally")
                                                .then(Commands.argument("target", StringArgumentType.word())
                                                        .executes(ctx -> fakeUnally(ctx.getSource(), StringArgumentType.getString(ctx, "actor"), StringArgumentType.getString(ctx, "target"))))))))
                .executes(ctx -> help(ctx.getSource())));
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!NoreTeamsApi.isAvailable(player.server)) {
            source.sendFailure(Component.literal("Open Parties and Claims is not available."));
            return 0;
        }
        Optional<TeamView> team = NoreTeamsApi.getPlayerTeam(player);
        if (team.isEmpty()) {
            source.sendSuccess(() -> Component.literal("You are not in a team."), false);
            return 1;
        }
        TeamView view = team.get();
        source.sendSuccess(() -> Component.literal("Team: " + view.name()), false);
        source.sendSuccess(() -> Component.literal("Team members: " + joinMembers(view)), false);
        source.sendSuccess(() -> Component.literal("Allied players: " + joinAlliedPlayers(player.server, view, player.getUUID())), false);
        return 1;
    }

    private static int sameTeam(ServerPlayer actor, ServerPlayer target) {
        boolean same = NoreTeamsApi.areSameTeam(actor.server, actor.getUUID(), target.getUUID());
        actor.sendSystemMessage(Component.literal(same ? "You are on the same team." : "You are not on the same team."));
        return same ? 1 : 0;
    }

    private static int sameAllied(ServerPlayer actor, ServerPlayer target) {
        boolean related = NoreTeamsApi.areSameTeam(actor.server, actor.getUUID(), target.getUUID())
                || NoreTeamsApi.areAllied(actor.server, actor.getUUID(), target.getUUID());
        actor.sendSystemMessage(Component.literal(related ? "You are on the same team or allied." : "You are not on the same team or allied."));
        return related ? 1 : 0;
    }

    private static int debugListPlayers(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Online players:"), false);
        source.getServer().getPlayerList().getPlayers().stream()
                .sorted(Comparator.comparing(player -> player.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(player -> source.sendSuccess(() -> Component.literal("- " + player.getGameProfile().getName() + " (" + player.getUUID() + ")"), false));
        if (DebugFakePlayers.entries().isEmpty()) {
            source.sendSuccess(() -> Component.literal("Fake players: none"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Fake players:"), false);
            DebugFakePlayers.entries().stream()
                    .sorted(Comparator.comparing(DebugFakePlayers.Entry::name, String.CASE_INSENSITIVE_ORDER))
                    .forEach(fake -> source.sendSuccess(() -> Component.literal("- " + fake.name() + " (" + fake.id() + ")"), false));
        }
        return 1;
    }

    private static int debugListFake(CommandSourceStack source) {
        if (DebugFakePlayers.entries().isEmpty()) {
            source.sendSuccess(() -> Component.literal("No fake players are registered."), false);
            return 1;
        }
        DebugFakePlayers.entries().stream()
                .sorted(Comparator.comparing(DebugFakePlayers.Entry::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(fake -> source.sendSuccess(() -> Component.literal(fake.name() + " (" + fake.id() + ")"), false));
        return 1;
    }

    private static int debugListTeams(CommandSourceStack source) {
        if (!NoreTeamsApi.isAvailable(source.getServer())) {
            source.sendFailure(Component.literal("Open Parties and Claims is not available."));
            return 0;
        }
        Map<UUID, TeamView> teams = new LinkedHashMap<>();
        source.getServer().getPlayerList().getPlayers().forEach(player ->
                NoreTeamsApi.getPlayerTeam(source.getServer(), player.getUUID()).ifPresent(team -> teams.putIfAbsent(team.partyId(), team)));
        DebugFakePlayers.entries().forEach(fake ->
                NoreTeamsApi.getPlayerTeam(source.getServer(), fake.id()).ifPresent(team -> teams.putIfAbsent(team.partyId(), team)));
        if (teams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No teams found for online or fake players."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Teams found from online/fake players: " + teams.size()), false);
        teams.values().stream()
                .sorted(Comparator.comparing(TeamView::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(team -> sendTeamSummary(source, team));
        return 1;
    }

    private static int debugInspect(CommandSourceStack source, UUID playerId, String label) {
        if (!NoreTeamsApi.isAvailable(source.getServer())) {
            source.sendFailure(Component.literal("Open Parties and Claims is not available."));
            return 0;
        }
        Optional<TeamView> team = NoreTeamsApi.getPlayerTeam(source.getServer(), playerId);
        if (team.isEmpty()) {
            source.sendSuccess(() -> Component.literal(label + " is not in a team. UUID: " + playerId), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(label + " is in:"), false);
        sendTeamDetails(source, team.get());
        return 1;
    }

    private static int debugInspectFake(CommandSourceStack source, String name) {
        Optional<DebugFakePlayers.Entry> fake = DebugFakePlayers.get(name);
        if (fake.isEmpty()) {
            source.sendFailure(Component.literal("No fake player named " + name + " is registered."));
            return 0;
        }
        return debugInspect(source, fake.get().id(), fake.get().name());
    }

    private static int inviteFake(ServerPlayer actor, String name) {
        boolean invited = NoreTeamServices.teams().inviteFake(actor, name);
        message(actor, invited, "Invited fake player " + name + ".", "Could not invite that fake player. Own a party and use a valid name.");
        return invited ? 1 : 0;
    }

    private static int addFake(ServerPlayer actor, String name) {
        boolean added = NoreTeamServices.teams().addFakeMember(actor, name);
        message(actor, added, "Added fake player " + name + " to your team.", "Could not add that fake player. Own a party and use a valid name.");
        return added ? 1 : 0;
    }

    private static int removeFake(ServerPlayer actor, String name) {
        boolean removed = NoreTeamServices.teams().removeFakeMember(actor, name);
        message(actor, removed, "Removed fake player " + name + ".", "Could not remove that fake player.");
        return removed ? 1 : 0;
    }

    private static int fakeCreate(CommandSourceStack source, String actor) {
        boolean ok = NoreTeamServices.teams().createFakeParty(source.getServer(), actor);
        source.sendSuccess(() -> Component.literal(ok ? actor + " created a fake team." : "Could not create a fake team for " + actor + "."), false);
        return ok ? 1 : 0;
    }

    private static int fakeDisband(CommandSourceStack source, String actor) {
        boolean ok = NoreTeamServices.teams().disbandFakeParty(source.getServer(), actor);
        source.sendSuccess(() -> Component.literal(ok ? actor + " disbanded their fake team." : "Could not disband a fake team for " + actor + "."), false);
        return ok ? 1 : 0;
    }

    private static int fakeLeave(CommandSourceStack source, String actor) {
        boolean ok = NoreTeamServices.teams().fakeLeave(source.getServer(), actor);
        source.sendSuccess(() -> Component.literal(ok ? actor + " left their team." : "Could not make " + actor + " leave."), false);
        return ok ? 1 : 0;
    }

    private static int fakeInvite(CommandSourceStack source, String actor, String target) {
        boolean ok = NoreTeamServices.teams().fakeInvite(source.getServer(), actor, target);
        source.sendSuccess(() -> Component.literal(ok ? actor + " invited " + target + "." : "Could not make " + actor + " invite " + target + "."), false);
        return ok ? 1 : 0;
    }

    private static int fakeJoin(CommandSourceStack source, String actor, String owner) {
        boolean ok = NoreTeamServices.teams().fakeJoin(source.getServer(), actor, owner);
        source.sendSuccess(() -> Component.literal(ok ? actor + " joined " + owner + "'s team." : "Could not make " + actor + " join " + owner + "'s team."), false);
        return ok ? 1 : 0;
    }

    private static int fakeAlly(CommandSourceStack source, String actor, String target) {
        boolean ok = NoreTeamServices.teams().fakeAlly(source.getServer(), actor, target);
        source.sendSuccess(() -> Component.literal(ok ? actor + " allied " + target + "." : "Could not make " + actor + " ally " + target + "."), false);
        return ok ? 1 : 0;
    }

    private static int fakeUnally(CommandSourceStack source, String actor, String target) {
        boolean ok = NoreTeamServices.teams().fakeUnally(source.getServer(), actor, target);
        source.sendSuccess(() -> Component.literal(ok ? actor + " unallied " + target + "." : "Could not make " + actor + " unally " + target + "."), false);
        return ok ? 1 : 0;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Teams: /noreteams status, same team <player>, same allied <player>"), false);
        if (NoreTeamsConfig.enableDebugCommands()) {
            source.sendSuccess(() -> Component.literal("Debug inspect: /noreteams debug listplayers, listfake, listteams, inspect <player>, inspectid <uuid>, inspectfake <name>"), false);
            source.sendSuccess(() -> Component.literal("Debug: /noreteams debug addfake <name>, invitefake <name>, removefake <name>"), false);
            source.sendSuccess(() -> Component.literal("Debug fake actor: /noreteams debug fake <name> create|disband|leave|invite <target>|join <owner>|ally <target>|unally <target>"), false);
        }
        return 1;
    }

    private static void message(ServerPlayer player, boolean ok, String success, String failure) {
        player.sendSystemMessage(Component.literal(ok ? success : failure));
    }

    private static String joinMembers(TeamView team) {
        if (team.members().isEmpty()) {
            return "none";
        }
        return String.join(", ", team.members().stream()
                .map(MemberView::username)
                .toList());
    }

    private static String joinAllies(TeamView team) {
        if (team.allies().isEmpty()) {
            return "none";
        }
        return String.join(", ", team.allies().stream()
                .map(AllyView::name)
                .toList());
    }

    private static String joinAlliedPlayers(MinecraftServer server, TeamView team, UUID self) {
        if (team.allies().isEmpty()) {
            return "none";
        }
        Set<UUID> ownTeamMembers = new LinkedHashSet<>();
        team.members().forEach(member -> ownTeamMembers.add(member.playerId()));
        Set<String> names = new LinkedHashSet<>();
        team.allies().forEach(ally -> {
            Optional<TeamView> allyTeam = NoreTeamsApi.getTeamById(server, ally.partyId());
            if (allyTeam.isPresent()) {
                allyTeam.get().members().stream()
                        .filter(member -> !member.playerId().equals(self))
                        .filter(member -> !ownTeamMembers.contains(member.playerId()))
                        .sorted(Comparator.comparing(MemberView::username, String.CASE_INSENSITIVE_ORDER))
                        .forEach(member -> names.add(member.username()));
            } else {
                names.add(ally.name());
            }
        });
        return names.isEmpty() ? "none" : String.join(", ", names);
    }

    private static void sendTeamSummary(CommandSourceStack source, TeamView team) {
        source.sendSuccess(() -> Component.literal("- " + team.name() + " (" + team.partyId() + ") members=" + team.members().size() + " allies=" + team.allies().size()), false);
    }

    private static void sendTeamDetails(CommandSourceStack source, TeamView team) {
        source.sendSuccess(() -> Component.literal("Team: " + team.name() + " (" + team.partyId() + ")"), false);
        source.sendSuccess(() -> Component.literal("Members:"), false);
        team.members().stream()
                .sorted(Comparator.comparing(MemberView::username, String.CASE_INSENSITIVE_ORDER))
                .forEach(member -> source.sendSuccess(() -> Component.literal("- " + member.username() + " (" + member.playerId() + ") rank=" + member.rank() + (member.owner() ? " owner" : "")), false));
        source.sendSuccess(() -> Component.literal("Allies: " + joinAllies(team)), false);
    }
}
