package com.nore.stages.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nore.stages.NoreStagesConfig;
import com.nore.stages.compat.ftb.FtbStagesCompat;
import com.nore.stages.compat.ftb.FtbQuestBridge;
import com.nore.stages.compat.norequest.NoreQuestCompat;
import com.nore.stages.network.NoreStagesNetwork;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.service.LootRuleService;
import com.nore.stages.service.SpawnRuleService;
import com.nore.stages.service.StageService;
import com.nore.stages.service.WorldEventGateService;
import com.nore.stages.stage.StageScope;
import com.nore.stages.team.StageSubjectResolver;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class NoreStagesCommand {
    private NoreStagesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("norestages")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("grant")
                        .then(Commands.literal("global")
                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> suggestStages(builder))
                                        .executes(ctx -> grantGlobal(ctx.getSource(), id(ctx, "stage")))))
                        .then(Commands.literal("local")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> suggestStages(builder))
                                                .executes(ctx -> grantLocal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), id(ctx, "stage")))))))
                .then(Commands.literal("list")
                        .then(Commands.literal("global")
                                .executes(ctx -> listGlobal(ctx.getSource())))
                        .then(Commands.literal("local")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> listLocal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("reset")
                        .then(Commands.literal("global")
                                .executes(ctx -> resetGlobal(ctx.getSource())))
                        .then(Commands.literal("local")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetLocal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("revoke")
                        .then(Commands.literal("global")
                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> suggestStages(builder))
                                        .executes(ctx -> revokeGlobal(ctx.getSource(), id(ctx, "stage")))))
                        .then(Commands.literal("local")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> suggestStages(builder))
                                                .executes(ctx -> revokeLocal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), id(ctx, "stage")))))))
                .then(Commands.literal("ftb")
                        .then(Commands.literal("trigger")
                                .then(Commands.literal("quest")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> suggestStages(builder))
                                                        .executes(ctx -> setFtbTrigger(ctx.getSource(), "quest", StringArgumentType.getString(ctx, "id"), id(ctx, "stage"))))))
                                .then(Commands.literal("chapter")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> suggestStages(builder))
                                                        .executes(ctx -> setFtbTrigger(ctx.getSource(), "chapter", StringArgumentType.getString(ctx, "id"), id(ctx, "stage")))))))
                        .then(Commands.literal("gate")
                                .then(Commands.literal("quest")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> suggestStages(builder))
                                                        .executes(ctx -> setFtbGate(ctx.getSource(), "quest", StringArgumentType.getString(ctx, "id"), id(ctx, "stage"), StageScope.LOCAL))
                                                        .then(Commands.argument("scope", StringArgumentType.word())
                                                                .executes(ctx -> setFtbGate(ctx.getSource(), "quest", StringArgumentType.getString(ctx, "id"), id(ctx, "stage"), StageScope.parse(StringArgumentType.getString(ctx, "scope"), StageScope.LOCAL)))))))
                                .then(Commands.literal("chapter")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> suggestStages(builder))
                                                        .executes(ctx -> setFtbGate(ctx.getSource(), "chapter", StringArgumentType.getString(ctx, "id"), id(ctx, "stage"), StageScope.LOCAL))
                                                        .then(Commands.argument("scope", StringArgumentType.word())
                                                                .executes(ctx -> setFtbGate(ctx.getSource(), "chapter", StringArgumentType.getString(ctx, "id"), id(ctx, "stage"), StageScope.parse(StringArgumentType.getString(ctx, "scope"), StageScope.LOCAL)))))))))
                .executes(ctx -> help(ctx.getSource()));
        if (NoreStagesConfig.enableDebugCommands()) {
            root.then(debugCommands());
        }
        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugCommands() {
        return Commands.literal("debug")
                .then(Commands.literal("global")
                        .executes(ctx -> debugGlobal(ctx.getSource())))
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> debugPlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("entity")
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .executes(ctx -> debugEntity(ctx.getSource(), EntityArgument.getEntity(ctx, "entity")))))
                .then(Commands.literal("spawns")
                        .executes(ctx -> debugSpawns(ctx.getSource()))
                        .then(Commands.literal("here")
                                .executes(ctx -> debugSpawnsHere(ctx.getSource())))
                        .then(Commands.literal("stats")
                                .executes(ctx -> debugSpawnStats(ctx.getSource()))
                                .then(Commands.literal("reset")
                                        .executes(ctx -> resetSpawnStats(ctx.getSource())))))
                .then(Commands.literal("loot")
                        .executes(ctx -> debugLoot(ctx.getSource())))
                .then(Commands.literal("events")
                        .executes(ctx -> debugEvents(ctx.getSource())))
                .then(Commands.literal("ranks")
                        .executes(ctx -> debugRanks(ctx.getSource())))
                .then(Commands.literal("quests")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> debugQuests(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("ftb")
                        .executes(ctx -> debugFtb(ctx.getSource())));
    }

    private static int reload(CommandSourceStack source) {
        StageDataRegistry.reload(source.getServer());
        source.getServer().getPlayerList().getPlayers().forEach(player -> {
            StageService.syncPlayerEffects(player);
            NoreStagesNetwork.sendSnapshot(player);
        });
        source.sendSuccess(() -> Component.literal("Reloaded NoreStages data: "
                + StageDataRegistry.stageCount() + " stage(s), "
                + StageDataRegistry.blockRestrictionCount() + " block restriction(s), "
                + StageDataRegistry.itemRestrictionCount() + " item restriction(s), "
                + StageDataRegistry.dimensionRestrictionCount() + " dimension restriction(s), "
                + StageDataRegistry.recipeRestrictionCount() + " recipe restriction(s), "
                + StageDataRegistry.entityRuleTargetCount() + " entity stat target(s), "
                + StageDataRegistry.spawnRuleCount() + " spawn rule(s), "
                + StageDataRegistry.lootRuleTargetCount() + " loot rule target(s), "
                + StageDataRegistry.worldEventGateCount() + " world event gate(s)."), true);
        return 1;
    }

    private static int listGlobal(CommandSourceStack source) {
        var stages = NoreStagesSavedData.get(source.getLevel()).globalStages();
        source.sendSuccess(() -> Component.literal("Global stages: " + stages), false);
        return stages.size();
    }

    private static int listLocal(CommandSourceStack source, ServerPlayer player) {
        UUID subject = StageSubjectResolver.localSubject(player);
        var stages = NoreStagesSavedData.get(source.getLevel()).localStages(subject);
        source.sendSuccess(() -> Component.literal("Local stages for " + player.getGameProfile().getName() + " (" + subject + "): " + stages), false);
        return stages.size();
    }

    private static int grantGlobal(CommandSourceStack source, ResourceLocation stage) {
        if (!knownStage(source, stage)) {
            return 0;
        }
        int result = StageService.grantGlobal(source.getServer(), stage);
        source.sendSuccess(() -> Component.literal("Granted global stage " + stage + " (" + result + ")."), true);
        return result;
    }

    private static int revokeGlobal(CommandSourceStack source, ResourceLocation stage) {
        int result = StageService.revokeGlobal(source.getServer(), stage);
        source.sendSuccess(() -> Component.literal("Revoked global stage " + stage + " (" + result + ")."), true);
        return result;
    }

    private static int resetGlobal(CommandSourceStack source) {
        int result = StageService.resetGlobal(source.getServer());
        source.sendSuccess(() -> Component.literal("Reset global NoreStages progress entries (" + result + ")."), true);
        return result;
    }

    private static int grantLocal(CommandSourceStack source, ServerPlayer player, ResourceLocation stage) {
        if (!knownStage(source, stage)) {
            return 0;
        }
        int result = StageService.grantLocal(player, stage);
        source.sendSuccess(() -> Component.literal("Granted local stage " + stage + " to " + player.getGameProfile().getName() + " (" + result + ")."), true);
        return result;
    }

    private static int revokeLocal(CommandSourceStack source, ServerPlayer player, ResourceLocation stage) {
        int result = StageService.revokeLocal(player, stage);
        source.sendSuccess(() -> Component.literal("Revoked local stage " + stage + " from " + player.getGameProfile().getName() + " (" + result + ")."), true);
        return result;
    }

    private static int resetLocal(CommandSourceStack source, ServerPlayer player) {
        int result = StageService.resetLocal(player);
        source.sendSuccess(() -> Component.literal("Reset local NoreStages progress entries for " + player.getGameProfile().getName() + " (" + result + ")."), true);
        return result;
    }

    private static int debugGlobal(CommandSourceStack source) {
        NoreStagesSavedData data = NoreStagesSavedData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal("Global stages: " + data.globalStages()), false);
        source.sendSuccess(() -> Component.literal("Global facts: " + data.globalFacts()), false);
        source.sendSuccess(() -> Component.literal("Global rank: " + data.globalRank()), false);
        source.sendSuccess(() -> Component.literal("Rank order: " + StageDataRegistry.ranks()), false);
        return 1;
    }

    private static int debugPlayer(CommandSourceStack source, ServerPlayer player) {
        NoreStagesSavedData data = NoreStagesSavedData.get(source.getLevel());
        UUID subject = StageSubjectResolver.localSubject(player);
        source.sendSuccess(() -> Component.literal("Subject for " + player.getGameProfile().getName() + ": " + subject), false);
        source.sendSuccess(() -> Component.literal("NoreTeams team: " + StageSubjectResolver.teamId(player.server, player).map(UUID::toString).orElse("none")), false);
        source.sendSuccess(() -> Component.literal("Local share targets: " + StageSubjectResolver.localShareTargets(player)), false);
        source.sendSuccess(() -> Component.literal("Local stages: " + data.localStages(subject)), false);
        source.sendSuccess(() -> Component.literal("Local facts: " + data.localFacts(subject)), false);
        source.sendSuccess(() -> Component.literal("Local rank: " + data.localRank(subject)), false);
        source.sendSuccess(() -> Component.literal("Effective rank: " + StageService.effectiveRank(player)), false);
        return 1;
    }

    private static int debugEntity(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity is not living: " + entity.getStringUUID()));
            return 0;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        source.sendSuccess(() -> Component.literal("Entity: " + entityId + " (" + living.getStringUUID() + ")"), false);
        sendAttribute(source, living, "max_health", Attributes.MAX_HEALTH);
        sendAttribute(source, living, "attack_damage", Attributes.ATTACK_DAMAGE);
        sendAttribute(source, living, "armor", Attributes.ARMOR);
        sendAttribute(source, living, "armor_toughness", Attributes.ARMOR_TOUGHNESS);
        sendAttribute(source, living, "movement_speed", Attributes.MOVEMENT_SPEED);
        source.sendSuccess(() -> Component.literal("Current health: " + living.getHealth() + "/" + living.getMaxHealth()), false);
        return 1;
    }

    private static int debugSpawns(CommandSourceStack source) {
        var rules = StageDataRegistry.spawnRules();
        BlockPos pos = BlockPos.containing(source.getPosition());
        source.sendSuccess(() -> Component.literal("Spawn rules: " + rules.size() + " (" + SpawnRuleService.activeRuleCount(source.getLevel()) + " active)"), false);
        rules.forEach(rule -> source.sendSuccess(() -> Component.literal(SpawnRuleService.describe(rule, source.getLevel(), pos)), false));
        return rules.size();
    }

    private static int debugSpawnsHere(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        source.sendSuccess(() -> Component.literal("Monster spawn candidates here: " + source.getLevel().dimension().location() + " " + source.getLevel().getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("unknown_biome")), false);
        var candidates = SpawnRuleService.preview(source.getLevel(), pos, MobCategory.MONSTER);
        candidates.stream().limit(20).forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return candidates.size();
    }

    private static int debugSpawnStats(CommandSourceStack source) {
        var stats = SpawnRuleService.stats();
        if (stats.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Spawn stats: none recorded yet."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawn stats:"), false);
        stats.forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return stats.size();
    }

    private static int resetSpawnStats(CommandSourceStack source) {
        SpawnRuleService.resetStats();
        source.sendSuccess(() -> Component.literal("Reset NoreStages spawn stats."), false);
        return 1;
    }

    private static int debugLoot(CommandSourceStack source) {
        var rules = StageDataRegistry.lootRules();
        source.sendSuccess(() -> Component.literal("Loot rules: " + rules.size() + " (" + LootRuleService.activeRuleCount(source.getLevel()) + " active)"), false);
        rules.forEach(rule -> source.sendSuccess(() -> Component.literal(LootRuleService.describe(rule, source.getLevel())), false));
        return rules.size();
    }

    private static int debugEvents(CommandSourceStack source) {
        var rules = StageDataRegistry.worldEventGates();
        source.sendSuccess(() -> Component.literal("World event gates: " + rules.size() + " (" + WorldEventGateService.activeRuleCount(source.getLevel()) + " active)"), false);
        rules.forEach(rule -> source.sendSuccess(() -> Component.literal(WorldEventGateService.describe(rule, source.getLevel())), false));
        return rules.size();
    }

    private static int debugRanks(CommandSourceStack source) {
        var data = StageDataRegistry.rankData();
        var rules = StageDataRegistry.entityRankRules();
        source.sendSuccess(() -> Component.literal("Ranks: " + data.ranks()), false);
        source.sendSuccess(() -> Component.literal("Default rank: " + data.defaultRank()), false);
        source.sendSuccess(() -> Component.literal("Threat thresholds: warning=" + data.warningDifference() + ", deadly=" + data.deadlyDifference()), false);
        source.sendSuccess(() -> Component.literal("Entity rank rules: " + rules.size()), false);
        rules.stream().limit(20).forEach(rule -> source.sendSuccess(() -> Component.literal(rule.entity()
                + " rank=" + rule.rank()
                + " stage=" + (rule.stage() == null ? "always" : rule.stage())
                + " scope=" + rule.scope().name().toLowerCase(java.util.Locale.ROOT)), false));
        if (rules.size() > 20) {
            source.sendSuccess(() -> Component.literal("...and " + (rules.size() - 20) + " more entity rank rule(s)."), false);
        }
        return rules.size();
    }

    private static int debugQuests(CommandSourceStack source, ServerPlayer player) {
        NoreStagesSavedData data = NoreStagesSavedData.get(source.getLevel());
        UUID subject = StageSubjectResolver.localSubject(player);
        source.sendSuccess(() -> Component.literal("Global quest unlocks: " + data.globalQuestUnlocks()), false);
        source.sendSuccess(() -> Component.literal("Local quest unlocks for " + player.getGameProfile().getName() + ": " + data.localQuestUnlocks(subject)), false);
        source.sendSuccess(() -> Component.literal(FtbStagesCompat.debugStatus()), false);
        return data.globalQuestUnlocks().size() + data.localQuestUnlocks(subject).size();
    }

    private static int debugFtb(CommandSourceStack source) {
        NoreStagesSavedData data = NoreStagesSavedData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal("FTB quest triggers: " + data.ftbQuestTriggers()), false);
        source.sendSuccess(() -> Component.literal("Datapack FTB quest triggers: " + StageDataRegistry.ftbQuestTriggers()), false);
        source.sendSuccess(() -> Component.literal("FTB quest gates: " + data.ftbQuestGates()), false);
        source.sendSuccess(() -> Component.literal("Datapack FTB quest gates: " + StageDataRegistry.ftbQuestGates()), false);
        source.sendSuccess(() -> Component.literal("Recent FTB completions: " + FtbQuestBridge.recentCompletions()), false);
        return data.ftbQuestTriggers().size() + data.ftbQuestGates().size() + StageDataRegistry.ftbQuestTriggers().size() + StageDataRegistry.ftbQuestGates().size();
    }

    private static int setFtbTrigger(CommandSourceStack source, String type, String ftbId, ResourceLocation stage) {
        if (!knownStage(source, stage)) {
            return 0;
        }
        String target = FtbQuestBridge.key(type, ftbId);
        NoreStagesSavedData.get(source.getLevel()).setFtbQuestTrigger(target, stage);
        source.sendSuccess(() -> Component.literal("When FTB " + type + " " + ftbId + " completes, grant " + stage + "."), true);
        return 1;
    }

    private static int setFtbGate(CommandSourceStack source, String type, String ftbId, ResourceLocation stage, StageScope scope) {
        if (!knownStage(source, stage)) {
            return 0;
        }
        String target = FtbQuestBridge.key(type, ftbId);
        NoreStagesSavedData.get(source.getLevel()).setFtbQuestGate(target, stage, scope);
        source.getServer().getPlayerList().getPlayers().forEach(NoreStagesNetwork::sendSnapshot);
        String mode = source.getEntity() instanceof ServerPlayer player ? NoreQuestCompat.modeName(player, type, ftbId) : "unknown";
        source.sendSuccess(() -> Component.literal("FTB " + type + " " + ftbId + " requires " + scope.name().toLowerCase(java.util.Locale.ROOT) + " stage " + stage + ". NoreQuest mode=" + mode + "."), true);
        return 1;
    }

    private static void sendAttribute(CommandSourceStack source, LivingEntity living, String name, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        var instance = living.getAttribute(attribute);
        if (instance == null) {
            source.sendSuccess(() -> Component.literal(name + ": unsupported"), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(name + ": base=" + instance.getBaseValue() + ", value=" + instance.getValue()), false);
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("NoreStages: reload, grant|list|reset|revoke global|local, ftb trigger|gate, debug global|player|entity|spawns|loot|events|ranks|quests|ftb"), false);
        return 1;
    }

    private static ResourceLocation id(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name) {
        return ResourceLocationArgument.getId(ctx, name);
    }

    private static boolean knownStage(CommandSourceStack source, ResourceLocation stage) {
        if (StageDataRegistry.stage(stage).isPresent()) {
            return true;
        }
        source.sendFailure(Component.literal("Unknown NoreStages stage " + stage + ". Run /norestages reload after adding datapack stages."));
        return false;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestStages(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        StageDataRegistry.stages().forEach(stage -> builder.suggest(stage.id().toString()));
        return builder.buildFuture();
    }
}
