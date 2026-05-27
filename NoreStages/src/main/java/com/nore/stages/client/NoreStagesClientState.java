package com.nore.stages.client;

import com.nore.stages.network.NoreStagesNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class NoreStagesClientState {
    private static Map<ResourceLocation, List<String>> lockedItems = Map.of();
    private static Set<ResourceLocation> lockedRecipes = Set.of();
    private static Set<ResourceLocation> activeStages = Set.of();
    private static Set<ResourceLocation> directStages = Set.of();
    private static Map<String, NoreStagesNetwork.FtbQuestGate> ftbQuestGates = Map.of();
    private static Map<ResourceLocation, Integer> rankIndexes = Map.of();
    private static ResourceLocation playerRank;
    private static int warningDifference = 1;
    private static int deadlyDifference = 2;
    private static List<NoreStagesNetwork.RankRule> rankRules = List.of();

    private NoreStagesClientState() {
    }

    public static void setLockedContent(NoreStagesNetwork.LockedContentPayload payload) {
        lockedItems = payload.items().stream()
                .collect(Collectors.toUnmodifiableMap(NoreStagesNetwork.LockedItem::item, NoreStagesNetwork.LockedItem::stages));
        lockedRecipes = Set.copyOf(payload.recipes());
        activeStages = Set.copyOf(payload.stages());
        directStages = Set.copyOf(payload.directStages());
        ftbQuestGates = payload.ftbQuestGates().stream()
                .collect(Collectors.toUnmodifiableMap(NoreStagesNetwork.FtbQuestGate::target, gate -> gate));
        rankIndexes = rankIndexes(payload.ranks());
        playerRank = payload.playerRank();
        warningDifference = payload.warningDifference();
        deadlyDifference = payload.deadlyDifference();
        rankRules = List.copyOf(payload.rankRules());
        refreshJei();
    }

    public static Optional<List<String>> lockedStages(ResourceLocation item) {
        return Optional.ofNullable(lockedItems.get(item));
    }

    public static Set<ResourceLocation> lockedRecipes() {
        return lockedRecipes;
    }

    public static Set<ResourceLocation> lockedItems() {
        return lockedItems.keySet();
    }

    public static boolean isFtbQuestVisible(String target) {
        NoreStagesNetwork.FtbQuestGate gate = ftbQuestGates.get(target);
        if (gate == null) {
            return true;
        }
        return gate.directOnly() ? directStages.contains(gate.stage()) : activeStages.contains(gate.stage());
    }

    public static void clear() {
        lockedItems = Map.of();
        lockedRecipes = Set.of();
        activeStages = Set.of();
        directStages = Set.of();
        ftbQuestGates = Map.of();
        rankIndexes = Map.of();
        playerRank = null;
        warningDifference = 1;
        deadlyDifference = 2;
        rankRules = List.of();
        refreshJei();
    }

    public static Threat threat(EntityType<?> entityType) {
        if (rankIndexes.isEmpty() || playerRank == null) {
            return Threat.NONE;
        }
        Integer playerIndex = rankIndexes.get(playerRank);
        if (playerIndex == null) {
            return Threat.NONE;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        int entityIndex = -1;
        for (NoreStagesNetwork.RankRule rule : rankRules) {
            if (!rule.entity().equals(entityId) || !isRankRuleActive(rule)) {
                continue;
            }
            Integer rankIndex = rankIndexes.get(rule.rank());
            if (rankIndex != null && rankIndex > entityIndex) {
                entityIndex = rankIndex;
            }
        }
        int difference = entityIndex - playerIndex;
        if (difference >= deadlyDifference) {
            return Threat.DEADLY;
        }
        if (difference >= warningDifference) {
            return Threat.WARNING;
        }
        return Threat.NONE;
    }

    private static boolean isRankRuleActive(NoreStagesNetwork.RankRule rule) {
        return rule.stage() == null || activeStages.contains(rule.stage());
    }

    private static Map<ResourceLocation, Integer> rankIndexes(List<ResourceLocation> ranks) {
        Map<ResourceLocation, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < ranks.size(); i++) {
            indexes.put(ranks.get(i), i);
        }
        return Map.copyOf(indexes);
    }

    public enum Threat {
        NONE,
        WARNING,
        DEADLY
    }

    private static void refreshJei() {
        if (!ModList.get().isLoaded("jei")) {
            return;
        }
        try {
            Class.forName("com.nore.stages.compat.jei.NoreStagesJeiPlugin")
                    .getMethod("refreshHiddenContent")
                    .invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void refreshClientIntegrations() {
        refreshJei();
    }
}
