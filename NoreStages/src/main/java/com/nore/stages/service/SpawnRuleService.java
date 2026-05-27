package com.nore.stages.service;

import com.nore.stages.NoreStages;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.rule.SpawnRule;
import com.nore.stages.stage.StageScope;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SpawnRuleService {
    private static final Map<ResourceLocation, Counters> COUNTERS = new LinkedHashMap<>();
    private static final Map<String, MobSpawnSettings.SpawnerData> SPAWNER_DATA_CACHE = new LinkedHashMap<>();

    private SpawnRuleService() {
    }

    public static void apply(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        List<SpawnRule> matchingRules = matchingRules(level, event.getPos(), event.getMobCategory());
        Set<ResourceLocation> biomeOverrideEntities = new HashSet<>();
        for (SpawnRule rule : matchingRules) {
            if (rule.hasBiomeFilter()) {
                biomeOverrideEntities.add(rule.entity());
            }
        }
        for (SpawnRule rule : matchingRules) {
            if (!rule.hasBiomeFilter() && biomeOverrideEntities.contains(rule.entity())) {
                continue;
            }
            counters(rule.entity()).potentialMatches++;
            applyRule(event, rule);
            counters(rule.entity()).rulesApplied++;
        }
    }

    public static void allowSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        Level level = event.getLevel().getLevel();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntityType());
        if (hasActiveRuleFor(level, event.getPos(), event.getEntityType())) {
            counters(entityId).placementChecks++;
        }
        if (allowsTransplantedNaturalSpawn(level, event.getPos(), event.getEntityType())) {
            counters(entityId).placementForced++;
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
        }
    }

    public static void allowPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        Level level = event.getLevel().getLevel();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (hasActiveRuleFor(level, event.getEntity().blockPosition(), event.getEntity().getType())) {
            counters(entityId).positionChecks++;
        }
        if (allowsTransplantedNaturalSpawn(level, event.getEntity().blockPosition(), event.getEntity().getType())) {
            counters(entityId).positionForced++;
            event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED);
        }
    }

    public static void noteEntityJoined(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        for (SpawnRule rule : StageDataRegistry.spawnRules()) {
            if (rule.entity().equals(entityId) && isActive(entity.level(), rule) && matchesLocation(entity.level(), entity.blockPosition(), rule)) {
                counters(entityId).joined++;
                return;
            }
        }
    }

    public static int activeRuleCount(Level level) {
        int count = 0;
        for (SpawnRule rule : StageDataRegistry.spawnRules()) {
            if (isActive(level, rule)) {
                count++;
            }
        }
        return count;
    }

    public static List<String> preview(Level level, BlockPos pos, MobCategory category) {
        LevelEvent.PotentialSpawns event = new LevelEvent.PotentialSpawns(level, category, pos, level.getBiome(pos).value().getMobSettings().getMobs(category));
        apply(event);
        return event.getSpawnerDataList().stream()
                .sorted((left, right) -> Integer.compare(right.getWeight().asInt(), left.getWeight().asInt()))
                .map(data -> BuiltInRegistries.ENTITY_TYPE.getKey(data.type) + " weight=" + data.getWeight().asInt() + " group=" + data.minCount + "-" + data.maxCount)
                .toList();
    }

    private static boolean isActive(Level level, SpawnRule rule) {
        if (rule.stage() == null) {
            return true;
        }
        if (rule.scope() != StageScope.GLOBAL) {
            NoreStages.LOGGER.warn("Skipped local NoreStages spawn rule for {} because natural spawn lists are global.", rule.entity());
            return false;
        }
        return NoreStagesSavedData.get(level).hasGlobalStage(rule.stage());
    }

    private static List<SpawnRule> matchingRules(Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.MobCategory category) {
        List<SpawnRule> rules = new ArrayList<>();
        for (SpawnRule rule : StageDataRegistry.spawnRules()) {
            if (isActive(level, rule) && matchesCategory(category, rule) && matchesLocation(level, pos, rule)) {
                rules.add(rule);
            }
        }
        return rules;
    }

    private static boolean matchesCategory(net.minecraft.world.entity.MobCategory category, SpawnRule rule) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rule.entity());
        if (type == null) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages spawn entity {}", rule.entity());
            return false;
        }
        return category == (rule.category() == null ? type.getCategory() : rule.category());
    }

    private static boolean matchesLocation(Level level, net.minecraft.core.BlockPos pos, SpawnRule rule) {
        if (!rule.dimensions().isEmpty() && !rule.dimensions().contains(level.dimension().location())) {
            return false;
        }
        if (rule.biomes().isEmpty()) {
            return true;
        }
        ResourceLocation biomeId = level.getBiome(pos).unwrapKey()
                .map(ResourceKey<Biome>::location)
                .orElse(null);
        return biomeId != null && rule.biomes().contains(biomeId);
    }

    private static boolean allowsTransplantedNaturalSpawn(Level level, net.minecraft.core.BlockPos pos, EntityType<?> type) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        for (SpawnRule rule : StageDataRegistry.spawnRules()) {
            if (rule.action() == SpawnRule.Action.REMOVE || !rule.forcePlacement() || !rule.entity().equals(entityId)) {
                continue;
            }
            if (isActive(level, rule) && matchesLocation(level, pos, rule)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasActiveRuleFor(Level level, BlockPos pos, EntityType<?> type) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        for (SpawnRule rule : StageDataRegistry.spawnRules()) {
            if (rule.entity().equals(entityId) && isActive(level, rule) && matchesLocation(level, pos, rule)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> stats() {
        return COUNTERS.entrySet().stream()
                .map(entry -> entry.getKey() + " potential=" + entry.getValue().potentialMatches
                        + " applied=" + entry.getValue().rulesApplied
                        + " placement=" + entry.getValue().placementChecks
                        + " placement_forced=" + entry.getValue().placementForced
                        + " position=" + entry.getValue().positionChecks
                        + " position_forced=" + entry.getValue().positionForced
                        + " joined=" + entry.getValue().joined)
                .toList();
    }

    public static void resetStats() {
        COUNTERS.clear();
    }

    private static Counters counters(ResourceLocation entity) {
        return COUNTERS.computeIfAbsent(entity, ignored -> new Counters());
    }

    private static void applyRule(LevelEvent.PotentialSpawns event, SpawnRule rule) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rule.entity());
        if (type == null) {
            return;
        }
        List<MobSpawnSettings.SpawnerData> matching = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : event.getSpawnerDataList()) {
            if (data.type.equals(type)) {
                matching.add(data);
            }
        }
        switch (rule.action()) {
            case ADD -> {
                if (matching.isEmpty()) {
                    event.addSpawnerData(spawnerData(type, rule.weight(), rule.minCount(), rule.maxCount()));
                }
            }
            case REMOVE -> matching.forEach(event::removeSpawnerData);
            case SET_WEIGHT -> replaceWeights(event, type, matching, rule.weight());
            case MULTIPLY_WEIGHT -> matching.forEach(data -> {
                int weight = Math.max(1, (int) Math.round(data.getWeight().asInt() * rule.multiplier()));
                event.removeSpawnerData(data);
                event.addSpawnerData(spawnerData(type, weight, data.minCount, data.maxCount));
            });
        }
    }

    private static void replaceWeights(LevelEvent.PotentialSpawns event, EntityType<?> type, List<MobSpawnSettings.SpawnerData> matching, int weight) {
        matching.forEach(data -> {
            event.removeSpawnerData(data);
            event.addSpawnerData(spawnerData(type, Math.max(1, weight), data.minCount, data.maxCount));
        });
    }

    private static MobSpawnSettings.SpawnerData spawnerData(EntityType<?> type, int weight, int minCount, int maxCount) {
        ResourceLocation entity = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String key = entity + "|" + Math.max(1, weight) + "|" + minCount + "|" + maxCount;
        return SPAWNER_DATA_CACHE.computeIfAbsent(key, ignored -> new MobSpawnSettings.SpawnerData(type, Math.max(1, weight), minCount, maxCount));
    }

    public static String describe(SpawnRule rule, Level level) {
        return describe(rule, level, null);
    }

    public static String describe(SpawnRule rule, Level level, BlockPos pos) {
        ResourceLocation stage = rule.stage();
        String stageText = stage == null ? "always" : stage.toString();
        String activeText = isActive(level, rule) ? "active" : "inactive";
        String dimensionText = rule.dimensions().isEmpty() ? "any_dimension" : "dimensions=" + rule.dimensions();
        String biomeText = rule.biomes().isEmpty() ? "any_biome" : "biomes=" + rule.biomes();
        String hereText = pos == null ? "" : " here=" + (matchesLocation(level, pos, rule) ? "yes" : "no");
        return rule.action().name().toLowerCase(Locale.ROOT) + " " + rule.entity() + " stage=" + stageText + " " + activeText + hereText + " " + dimensionText + " " + biomeText;
    }

    private static final class Counters {
        private int potentialMatches;
        private int rulesApplied;
        private int placementChecks;
        private int placementForced;
        private int positionChecks;
        private int positionForced;
        private int joined;
    }
}
