package com.nore.stages.service;

import com.nore.stages.NoreStages;
import com.nore.stages.compat.ftb.FtbStagesCompat;
import com.nore.stages.network.NoreStagesNetwork;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.stage.StageCondition;
import com.nore.stages.stage.StageDefinition;
import com.nore.stages.stage.StageEffect;
import com.nore.stages.stage.StageScope;
import com.nore.stages.stage.TriggerFact;
import com.nore.stages.team.StageSubjectResolver;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;

import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StageService {
    private StageService() {
    }

    public static boolean hasStage(ServerPlayer player, ResourceLocation stage, StageScope scope) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        if (scope == StageScope.GLOBAL) {
            return data.hasGlobalStage(stage);
        }
        return data.hasLocalStage(StageSubjectResolver.localSubject(player), stage);
    }

    public static int grantGlobal(MinecraftServer server, ResourceLocation stage) {
        NoreStagesSavedData data = NoreStagesSavedData.get(server.overworld());
        if (!data.grantGlobalStage(stage)) {
            return 0;
        }
        StageDataRegistry.stage(stage).ifPresent(definition -> applyGlobalStage(server, definition));
        syncAllOnline(server);
        return 1;
    }

    public static int revokeGlobal(MinecraftServer server, ResourceLocation stage) {
        int result = NoreStagesSavedData.get(server.overworld()).revokeGlobalStage(stage) ? 1 : 0;
        if (result > 0) {
            syncAllOnline(server);
        }
        return result;
    }

    public static int resetGlobal(MinecraftServer server) {
        int result = NoreStagesSavedData.get(server.overworld()).resetGlobalProgress();
        if (result > 0) {
            syncAllOnline(server);
        }
        return result;
    }

    public static int grantLocal(ServerPlayer player, ResourceLocation stage) {
        return grantLocal(player, stage, true);
    }

    public static int grantLocal(ServerPlayer player, ResourceLocation stage, boolean share) {
        int changed = grantLocalDirect(player, stage);
        if (changed > 0 && share) {
            shareLocalStage(player, stage);
        }
        return changed;
    }

    public static int grantConfigured(ServerPlayer player, ResourceLocation stage) {
        return grantConfigured(player, stage, true);
    }

    public static int grantConfigured(ServerPlayer player, ResourceLocation stage, boolean shareLocal) {
        Optional<StageDefinition> definition = StageDataRegistry.stage(stage);
        if (definition.isPresent() && definition.get().scope() == StageScope.GLOBAL) {
            return grantGlobal(player.server, stage);
        }
        return grantLocal(player, stage, shareLocal);
    }

    public static int revokeLocal(ServerPlayer player, ResourceLocation stage) {
        UUID subject = StageSubjectResolver.localSubject(player);
        int result = NoreStagesSavedData.get(player.level()).revokeLocalStage(subject, stage) ? 1 : 0;
        if (result > 0) {
            syncOnlineSubject(player.server, subject);
        }
        return result;
    }

    public static int resetLocal(ServerPlayer player) {
        UUID subject = StageSubjectResolver.localSubject(player);
        int result = NoreStagesSavedData.get(player.level()).resetLocalProgress(subject, player.getUUID());
        if (result > 0) {
            syncOnlineSubject(player.server, subject);
        }
        return result;
    }

    public static void recordFact(ServerPlayer player, String fact) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        UUID subject = StageSubjectResolver.localSubject(player);
        boolean localChanged = data.recordLocalFact(subject, fact);
        boolean globalChanged = data.recordGlobalFact(fact);
        if (localChanged || globalChanged) {
            evaluate(player.server, player, subject);
        }
    }

    public static void pollNumericTriggers(ServerPlayer player) {
        for (StageDefinition definition : StageDataRegistry.stages()) {
            for (StageCondition.NumericTrigger trigger : definition.condition().numericTriggers()) {
                if (numericTriggerMet(player, trigger)) {
                    recordFact(player, trigger.fact());
                }
            }
        }
    }

    public static void syncGlobalEffects(ServerPlayer player) {
        removeKnownAttributeModifiers(player);
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        for (ResourceLocation stage : data.globalStages()) {
            Optional<StageDefinition> definition = StageDataRegistry.stage(stage);
            if (definition.isPresent()) {
                applyGlobalRankEffects(player.server, definition.get());
                applyRepeatablePlayerEffects(player.server, definition.get(), player);
                if (!data.hasAppliedGlobalStage(player.getUUID(), stage)) {
                    applyOneTimePlayerEffects(player.server, definition.get(), player);
                    data.markAppliedGlobalStage(player.getUUID(), stage);
                }
            }
        }
    }

    public static void syncLocalEffects(ServerPlayer player) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        UUID subject = StageSubjectResolver.localSubject(player);
        for (ResourceLocation stage : data.localStages(subject)) {
            Optional<StageDefinition> definition = StageDataRegistry.stage(stage);
            if (definition.isPresent()) {
                applyRepeatablePlayerEffects(player.server, definition.get(), player);
                if (!data.hasAppliedLocalStage(player.getUUID(), stage)) {
                    applyOneTimePlayerEffects(player.server, definition.get(), player);
                    data.markAppliedLocalStage(player.getUUID(), stage);
                }
            }
        }
    }

    public static void syncPlayerEffects(ServerPlayer player) {
        syncGlobalEffects(player);
        syncLocalEffects(player);
    }

    public static boolean canCraft(ServerPlayer player, ResourceLocation recipe) {
        for (var restriction : StageDataRegistry.recipeRestrictions(recipe)) {
            if (!hasStage(player, restriction.stage(), restriction.scope())) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasQuestUnlocked(ServerPlayer player, ResourceLocation quest) {
        return NoreStagesSavedData.get(player.level()).hasQuestUnlocked(StageSubjectResolver.localSubject(player), quest);
    }

    public static boolean hasDirectLocalStage(ServerPlayer player, ResourceLocation stage) {
        return NoreStagesSavedData.get(player.level()).hasDirectLocalStage(StageSubjectResolver.localSubject(player), stage);
    }

    private static void evaluate(MinecraftServer server, ServerPlayer player, UUID subject) {
        NoreStagesSavedData data = NoreStagesSavedData.get(server.overworld());
        Collection<StageDefinition> definitions = StageDataRegistry.stages();
        for (StageDefinition definition : definitions) {
            if (definition.scope() == StageScope.GLOBAL) {
                if (!data.hasGlobalStage(definition.id()) && definition.condition().matches(activeGlobalFacts(data))) {
                    grantGlobal(server, definition.id());
                }
            } else if (!data.hasLocalStage(subject, definition.id()) && definition.condition().matches(activeLocalFacts(data, subject))) {
                grantLocal(player, definition.id());
            }
        }
    }

    private static int grantLocalDirect(ServerPlayer player, ResourceLocation stage) {
        return grantLocalDirect(player, stage, true);
    }

    private static int grantLocalDirect(ServerPlayer player, ResourceLocation stage, boolean direct) {
        UUID subject = StageSubjectResolver.localSubject(player);
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        if (!data.grantLocalStage(subject, stage, direct)) {
            return 0;
        }
        syncOnlineSubject(player.server, subject);
        return 1;
    }

    private static void shareLocalStage(ServerPlayer source, ResourceLocation stage) {
        Optional<StageDefinition> definition = StageDataRegistry.stage(stage);
        if (definition.isEmpty() || definition.get().scope() != StageScope.LOCAL) {
            return;
        }
        Set<UUID> targets = StageSubjectResolver.localShareTargets(source);
        for (ServerPlayer player : source.server.getPlayerList().getPlayers()) {
            if (!targets.contains(player.getUUID()) || player.getUUID().equals(source.getUUID())) {
                continue;
            }
            if (canReceiveSharedStage(player, definition.get())) {
                grantLocalDirect(player, stage, false);
            }
        }
    }

    private static boolean canReceiveSharedStage(ServerPlayer player, StageDefinition definition) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        UUID subject = StageSubjectResolver.localSubject(player);
        if (data.hasLocalStage(subject, definition.id())) {
            return false;
        }
        for (ResourceLocation requiredStage : definition.condition().requiredStages()) {
            if (!data.hasLocalStage(subject, requiredStage)) {
                return false;
            }
        }
        return true;
    }

    private static void applyGlobalStage(MinecraftServer server, StageDefinition definition) {
        applyServerEffects(server, definition);
    }

    private static void applyEffects(MinecraftServer server, StageDefinition definition, ServerPlayer player) {
        applyServerEffects(server, definition);
        applyRepeatablePlayerEffects(server, definition, player);
        applyOneTimePlayerEffects(server, definition, player);
    }

    private static void applyServerEffects(MinecraftServer server, StageDefinition definition) {
        for (StageEffect effect : definition.effects()) {
            if (effect.type() == StageEffect.Type.SERVER_COMMAND && !effect.command().isBlank()) {
                runCommand(server.createCommandSourceStack(), effect.command());
            } else if (definition.scope() == StageScope.GLOBAL && effect.type() == StageEffect.Type.UNLOCK_QUEST && effect.quest() != null) {
                NoreStagesSavedData.get(server.overworld()).unlockGlobalQuest(effect.quest());
            } else if (definition.scope() == StageScope.GLOBAL && effect.type() == StageEffect.Type.SET_RANK && effect.rank() != null) {
                promoteGlobalRank(server, effect.rank());
            }
        }
    }

    private static void applyGlobalRankEffects(MinecraftServer server, StageDefinition definition) {
        if (definition.scope() != StageScope.GLOBAL) {
            return;
        }
        for (StageEffect effect : definition.effects()) {
            if (effect.type() == StageEffect.Type.SET_RANK && effect.rank() != null) {
                promoteGlobalRank(server, effect.rank());
            }
        }
    }

    private static void applyOneTimePlayerEffects(MinecraftServer server, StageDefinition definition, ServerPlayer player) {
        for (StageEffect effect : definition.effects()) {
            if (effect.type() == StageEffect.Type.PLAYER_COMMAND && !effect.command().isBlank()) {
                runCommand(server.createCommandSourceStack(), placeholders(effect.command(), player));
            }
        }
    }

    private static void applyRepeatablePlayerEffects(MinecraftServer server, StageDefinition definition, ServerPlayer player) {
        for (StageEffect effect : definition.effects()) {
            if (effect.type() == StageEffect.Type.ATTRIBUTE && effect.attribute() != null) {
                applyAttribute(player, definition.id(), effect);
            } else if (effect.type() == StageEffect.Type.UNLOCK_RECIPE && effect.recipe() != null) {
                unlockRecipe(server, player, effect.recipe());
            } else if (effect.type() == StageEffect.Type.UNLOCK_QUEST && effect.quest() != null) {
                unlockQuest(player, definition.scope(), effect.quest());
            } else if (definition.scope() == StageScope.LOCAL && effect.type() == StageEffect.Type.SET_RANK && effect.rank() != null) {
                promoteLocalRank(player, effect.rank());
            }
        }
    }

    public static ResourceLocation effectiveRank(ServerPlayer player) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        ResourceLocation rank = StageDataRegistry.rankData().defaultRank();
        rank = StageDataRegistry.strongerRank(rank, data.globalRank());
        rank = StageDataRegistry.strongerRank(rank, data.localRank(StageSubjectResolver.localSubject(player)));
        return rank;
    }

    private static void promoteGlobalRank(MinecraftServer server, ResourceLocation rank) {
        int index = StageDataRegistry.rankIndex(rank);
        if (index < 0) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages rank {}", rank);
            return;
        }
        NoreStagesSavedData data = NoreStagesSavedData.get(server.overworld());
        data.promoteGlobalRank(rank, index, StageDataRegistry.rankIndex(data.globalRank()));
    }

    private static void promoteLocalRank(ServerPlayer player, ResourceLocation rank) {
        int index = StageDataRegistry.rankIndex(rank);
        if (index < 0) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages rank {}", rank);
            return;
        }
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        UUID subject = StageSubjectResolver.localSubject(player);
        data.promoteLocalRank(subject, rank, index, StageDataRegistry.rankIndex(data.localRank(subject)));
    }

    private static void unlockQuest(ServerPlayer player, StageScope scope, ResourceLocation quest) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        if (scope == StageScope.GLOBAL) {
            data.unlockGlobalQuest(quest);
        } else {
            data.unlockLocalQuest(StageSubjectResolver.localSubject(player), quest);
        }
        FtbStagesCompat.grantQuestStage(player, quest);
    }

    private static void unlockRecipe(MinecraftServer server, ServerPlayer player, ResourceLocation recipeId) {
        Optional<RecipeHolder<?>> recipe = server.getRecipeManager().byKey(recipeId);
        if (recipe.isEmpty()) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages recipe unlock {}", recipeId);
            return;
        }
        player.awardRecipes(Collections.singleton(recipe.get()));
    }

    private static void applyAttribute(ServerPlayer player, ResourceLocation stage, StageEffect effect) {
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute());
        if (holder.isEmpty()) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages player attribute {}", effect.attribute());
            return;
        }
        AttributeInstance instance = player.getAttribute(holder.get());
        if (instance == null) {
            NoreStages.LOGGER.warn("Skipped unsupported NoreStages player attribute {} for {}", effect.attribute(), player.getGameProfile().getName());
            return;
        }
        ResourceLocation modifierId = attributeModifierId(stage, effect.attribute());
        instance.addOrUpdateTransientModifier(new AttributeModifier(modifierId, effect.value(), effect.operation()));
    }

    private static void removeKnownAttributeModifiers(ServerPlayer player) {
        for (StageDefinition definition : StageDataRegistry.stages()) {
            for (StageEffect effect : definition.effects()) {
                if (effect.type() != StageEffect.Type.ATTRIBUTE || effect.attribute() == null) {
                    continue;
                }
                Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute());
                if (holder.isEmpty()) {
                    continue;
                }
                AttributeInstance instance = player.getAttribute(holder.get());
                if (instance != null) {
                    instance.removeModifier(attributeModifierId(definition.id(), effect.attribute()));
                }
            }
        }
    }

    private static ResourceLocation attributeModifierId(ResourceLocation stage, ResourceLocation attribute) {
        return ResourceLocation.fromNamespaceAndPath(stage.getNamespace(), "stage/" + stage.getPath().replace('/', '_') + "/" + attribute.getNamespace() + "_" + attribute.getPath().replace('.', '_'));
    }

    private static void syncAllOnline(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> {
            StageService.syncPlayerEffects(player);
            NoreStagesNetwork.sendSnapshot(player);
        });
    }

    private static void syncOnlineSubject(MinecraftServer server, UUID subject) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (StageSubjectResolver.localSubject(player).equals(subject)) {
                syncPlayerEffects(player);
                NoreStagesNetwork.sendSnapshot(player);
            }
        }
    }

    private static void runCommand(CommandSourceStack source, String command) {
        source.getServer().getCommands().performPrefixedCommand(source.withSuppressedOutput().withPermission(4), command);
    }

    private static String placeholders(String command, ServerPlayer player) {
        return command
                .replace("{player}", player.getGameProfile().getName())
                .replace("{uuid}", player.getUUID().toString());
    }

    private static boolean numericTriggerMet(ServerPlayer player, StageCondition.NumericTrigger trigger) {
        if ("server_age".equals(trigger.type())) {
            return player.server.getTickCount() >= trigger.threshold();
        }
        if ("scoreboard".equals(trigger.type())) {
            Objective objective = player.getScoreboard().getObjective(trigger.objectiveName());
            if (objective == null) {
                return false;
            }
            ReadOnlyScoreInfo score = player.getScoreboard().getPlayerScoreInfo(player, objective);
            return score != null && score.value() >= trigger.threshold();
        }
        return false;
    }

    private static Set<String> activeGlobalFacts(NoreStagesSavedData data) {
        Set<String> facts = new HashSet<>(data.globalFacts());
        data.globalStages().forEach(stage -> facts.add(TriggerFact.of("stage", stage).key()));
        return facts;
    }

    private static Set<String> activeLocalFacts(NoreStagesSavedData data, UUID subject) {
        Set<String> facts = new HashSet<>(data.localFacts(subject));
        data.localStages(subject).forEach(stage -> facts.add(TriggerFact.of("stage", stage).key()));
        return facts;
    }

    public static void tellBlocked(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("norestages.blocked", "a required stage"), true);
    }

    public static void tellBlocked(ServerPlayer player, ResourceLocation stage) {
        String stageName = StageDataRegistry.stage(stage).map(StageDefinition::displayName).orElse(stage.toString());
        player.displayClientMessage(Component.translatable("norestages.blocked", stageName), true);
    }
}
