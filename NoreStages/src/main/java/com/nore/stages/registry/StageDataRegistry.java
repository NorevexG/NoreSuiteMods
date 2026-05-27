package com.nore.stages.registry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nore.stages.NoreStages;
import com.nore.stages.rule.EntityStatRule;
import com.nore.stages.rule.LootRule;
import com.nore.stages.rank.RankData;
import com.nore.stages.rule.SpawnRule;
import com.nore.stages.rule.StageRestriction;
import com.nore.stages.rule.WorldEventGateRule;
import com.nore.stages.stage.StageDefinition;
import com.nore.stages.stage.StageScope;
import com.nore.stages.compat.ftb.FtbQuestBridge;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.function.Consumer;

public final class StageDataRegistry {
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, StageDefinition> STAGES = new LinkedHashMap<>();
    private static final List<StageRestriction> BLOCK_RESTRICTIONS = new ArrayList<>();
    private static final List<StageRestriction> ITEM_RESTRICTIONS = new ArrayList<>();
    private static final List<StageRestriction> DIMENSION_RESTRICTIONS = new ArrayList<>();
    private static final List<StageRestriction> RECIPE_RESTRICTIONS = new ArrayList<>();
    private static final Map<ResourceLocation, List<EntityStatRule>> ENTITY_RULES = new LinkedHashMap<>();
    private static final List<SpawnRule> SPAWN_RULES = new ArrayList<>();
    private static final Map<ResourceLocation, List<LootRule>> LOOT_RULES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, WorldEventGateRule> WORLD_EVENT_GATES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<WorldEventGateRule>> WORLD_EVENT_GATES_BY_EVENT = new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> FTB_QUEST_TRIGGERS = new LinkedHashMap<>();
    private static final Map<String, NoreStagesSavedData.QuestGate> FTB_QUEST_GATES = new LinkedHashMap<>();
    private static RankData RANK_DATA = RankData.empty();

    private StageDataRegistry() {
    }

    public static void reload(MinecraftServer server) {
        STAGES.clear();
        BLOCK_RESTRICTIONS.clear();
        ITEM_RESTRICTIONS.clear();
        DIMENSION_RESTRICTIONS.clear();
        RECIPE_RESTRICTIONS.clear();
        ENTITY_RULES.clear();
        SPAWN_RULES.clear();
        LOOT_RULES.clear();
        WORLD_EVENT_GATES.clear();
        WORLD_EVENT_GATES_BY_EVENT.clear();
        FTB_QUEST_TRIGGERS.clear();
        FTB_QUEST_GATES.clear();
        RANK_DATA = RankData.empty();

        loadRanks(server);
        loadStages(server);
        loadRestrictions(server);
        loadEntityStats(server);
        loadSpawnRules(server);
        loadLootRules(server);
        loadWorldEvents(server);
        NoreStages.LOGGER.info("Loaded {} NoreStages stage(s), {} block restriction(s), {} item restriction(s), {} dimension restriction(s), {} recipe restriction(s), {} entity stat target(s), {} spawn rule(s), {} loot rule target(s), and {} world event gate(s).",
                STAGES.size(), BLOCK_RESTRICTIONS.size(), ITEM_RESTRICTIONS.size(), DIMENSION_RESTRICTIONS.size(), RECIPE_RESTRICTIONS.size(), ENTITY_RULES.size(), SPAWN_RULES.size(), LOOT_RULES.size(), WORLD_EVENT_GATES.size());
    }

    public static Collection<StageDefinition> stages() {
        return STAGES.values();
    }

    public static int stageCount() {
        return STAGES.size();
    }

    public static int blockRestrictionCount() {
        return BLOCK_RESTRICTIONS.size();
    }

    public static int itemRestrictionCount() {
        return ITEM_RESTRICTIONS.size();
    }

    public static int dimensionRestrictionCount() {
        return DIMENSION_RESTRICTIONS.size();
    }

    public static int recipeRestrictionCount() {
        return RECIPE_RESTRICTIONS.size();
    }

    public static int entityRuleTargetCount() {
        return ENTITY_RULES.size();
    }

    public static int spawnRuleCount() {
        return SPAWN_RULES.size();
    }

    public static int lootRuleTargetCount() {
        return LOOT_RULES.size();
    }

    public static int worldEventGateCount() {
        return WORLD_EVENT_GATES.size();
    }

    public static Optional<StageDefinition> stage(ResourceLocation id) {
        return Optional.ofNullable(STAGES.get(id));
    }

    public static List<StageRestriction> blockRestrictions(ResourceLocation block) {
        return BLOCK_RESTRICTIONS.stream().filter(restriction -> restriction.target().equals(block)).toList();
    }

    public static List<StageRestriction> itemRestrictions(ResourceLocation item) {
        return ITEM_RESTRICTIONS.stream().filter(restriction -> restriction.target().equals(item)).toList();
    }

    public static List<StageRestriction> dimensionRestrictions(ResourceLocation dimension) {
        return DIMENSION_RESTRICTIONS.stream().filter(restriction -> restriction.target().equals(dimension)).toList();
    }

    public static List<StageRestriction> recipeRestrictions(ResourceLocation recipe) {
        return RECIPE_RESTRICTIONS.stream().filter(restriction -> restriction.target().equals(recipe)).toList();
    }

    public static List<StageRestriction> recipeRestrictions() {
        return List.copyOf(RECIPE_RESTRICTIONS);
    }

    public static List<EntityStatRule> entityRules(ResourceLocation entity) {
        return ENTITY_RULES.getOrDefault(entity, List.of());
    }

    public static List<EntityStatRule> entityRankRules() {
        return ENTITY_RULES.values().stream()
                .flatMap(List::stream)
                .filter(rule -> rule.rank() != null)
                .toList();
    }

    public static List<SpawnRule> spawnRules() {
        return List.copyOf(SPAWN_RULES);
    }

    public static List<LootRule> lootRules(ResourceLocation entity) {
        return LOOT_RULES.getOrDefault(entity, List.of());
    }

    public static List<LootRule> lootRules() {
        return LOOT_RULES.values().stream().flatMap(List::stream).toList();
    }

    public static RankData rankData() {
        return RANK_DATA;
    }

    public static List<ResourceLocation> ranks() {
        return RANK_DATA.ranks();
    }

    public static int rankIndex(ResourceLocation rank) {
        if (rank == null) {
            return -1;
        }
        return RANK_DATA.ranks().indexOf(rank);
    }

    public static boolean hasRank(ResourceLocation rank) {
        return rank != null && rankIndex(rank) >= 0;
    }

    public static ResourceLocation strongerRank(ResourceLocation left, ResourceLocation right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return rankIndex(right) > rankIndex(left) ? right : left;
    }

    public static List<WorldEventGateRule> worldEventGates() {
        return List.copyOf(WORLD_EVENT_GATES.values());
    }

    public static List<WorldEventGateRule> worldEventGates(ResourceLocation event) {
        return WORLD_EVENT_GATES_BY_EVENT.getOrDefault(event, List.of());
    }

    public static ResourceLocation ftbQuestTrigger(String target) {
        return FTB_QUEST_TRIGGERS.get(target);
    }

    public static NoreStagesSavedData.QuestGate ftbQuestGate(String target) {
        return FTB_QUEST_GATES.get(target);
    }

    public static Map<String, ResourceLocation> ftbQuestTriggers() {
        return Map.copyOf(FTB_QUEST_TRIGGERS);
    }

    public static Map<String, NoreStagesSavedData.QuestGate> ftbQuestGates() {
        return Map.copyOf(FTB_QUEST_GATES);
    }

    private static void loadStages(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/stages", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> {
                    ResourceLocation id = dataId(resourceId, "norestages/stages");
                    StageDefinition definition = StageDefinition.parse(id, json);
                    STAGES.put(id, definition);
                    loadInlineStageData(id, definition.scope(), json);
                }));
    }

    private static void loadRanks(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/ranks", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> RANK_DATA = RankData.parse(json, RANK_DATA)));
    }

    private static void loadInlineStageData(ResourceLocation stage, StageScope scope, JsonObject json) {
        if (json.has("restrictions") && json.get("restrictions").isJsonObject()) {
            JsonObject restrictions = json.getAsJsonObject("restrictions");
            readInlineRestrictions(stage, scope, restrictions, "blocks", BLOCK_RESTRICTIONS);
            readInlineRestrictions(stage, scope, restrictions, "items", ITEM_RESTRICTIONS);
            readInlineRestrictions(stage, scope, restrictions, "dimensions", DIMENSION_RESTRICTIONS);
            readInlineRestrictions(stage, scope, restrictions, "recipes", RECIPE_RESTRICTIONS);
        }
        readInlineRules(json, "entity_stats", object -> {
            EntityStatRule rule = EntityStatRule.parse(withStageDefaults(object, stage, scope));
            ENTITY_RULES.computeIfAbsent(rule.entity(), ignored -> new ArrayList<>()).add(rule);
        });
        readInlineRules(json, "loot_rules", object -> {
            LootRule rule = LootRule.parse(withStageDefaults(object, stage, scope));
            LOOT_RULES.computeIfAbsent(rule.entity(), ignored -> new ArrayList<>()).add(rule);
        });
        readInlineRules(json, "spawn_rules", object -> SPAWN_RULES.add(SpawnRule.parse(withStageDefaults(object, stage, scope))));
        readInlineRules(json, "world_events", new Consumer<>() {
            private int index;

            @Override
            public void accept(JsonObject object) {
                JsonObject copy = withStageDefaults(object, stage, scope);
                String suffix = copy.has("event") ? copy.get("event").getAsString().replace(':', '_').replace('/', '_') : "event";
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(stage.getNamespace(), stage.getPath() + "/" + suffix + "_" + index++);
                WorldEventGateRule rule = WorldEventGateRule.parse(id, copy);
                WORLD_EVENT_GATES.put(id, rule);
                WORLD_EVENT_GATES_BY_EVENT.computeIfAbsent(rule.event(), ignored -> new ArrayList<>()).add(rule);
            }
        });
        readInlineFtb(stage, scope, json);
    }

    private static void readInlineRules(JsonObject json, String key, Consumer<JsonObject> consumer) {
        if (!json.has(key)) {
            return;
        }
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(item -> {
                if (item.isJsonObject()) {
                    consumer.accept(item.getAsJsonObject());
                }
            });
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("rules") && object.get("rules").isJsonArray()) {
                object.getAsJsonArray("rules").forEach(item -> {
                    if (item.isJsonObject()) {
                        consumer.accept(item.getAsJsonObject());
                    }
                });
            } else {
                consumer.accept(object);
            }
        }
    }

    private static JsonObject withStageDefaults(JsonObject object, ResourceLocation stage, StageScope scope) {
        JsonObject copy = object.deepCopy();
        if (!copy.has("stage")) {
            copy.addProperty("stage", stage.toString());
        }
        if (!copy.has("scope")) {
            copy.addProperty("scope", scope.name().toLowerCase(Locale.ROOT));
        }
        return copy;
    }

    private static void readInlineFtb(ResourceLocation stage, StageScope scope, JsonObject json) {
        if (!json.has("ftb") || !json.get("ftb").isJsonObject()) {
            return;
        }
        JsonObject ftb = json.getAsJsonObject("ftb");
        if (ftb.has("triggers") && ftb.get("triggers").isJsonArray()) {
            ftb.getAsJsonArray("triggers").forEach(element -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject object = element.getAsJsonObject();
                String type = object.has("type") ? object.get("type").getAsString() : "quest";
                String id = object.get("id").getAsString();
                ResourceLocation targetStage = object.has("stage") ? ResourceLocation.parse(object.get("stage").getAsString()) : stage;
                FTB_QUEST_TRIGGERS.put(FtbQuestBridge.key(type, id), targetStage);
            });
        }
        if (ftb.has("gates") && ftb.get("gates").isJsonArray()) {
            ftb.getAsJsonArray("gates").forEach(element -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject object = element.getAsJsonObject();
                String type = object.has("type") ? object.get("type").getAsString() : "quest";
                String id = object.get("id").getAsString();
                ResourceLocation gateStage = object.has("stage") ? ResourceLocation.parse(object.get("stage").getAsString()) : stage;
                StageScope gateScope = StageScope.parse(object.has("scope") ? object.get("scope").getAsString() : scope.name(), scope);
                FTB_QUEST_GATES.put(FtbQuestBridge.key(type, id), new NoreStagesSavedData.QuestGate(gateStage, gateScope));
            });
        }
    }

    private static void loadRestrictions(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/restrictions", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> {
                    readRestrictions(json, "blocks", BLOCK_RESTRICTIONS);
                    readRestrictions(json, "items", ITEM_RESTRICTIONS);
                    readRestrictions(json, "dimensions", DIMENSION_RESTRICTIONS);
                    readRestrictions(json, "recipes", RECIPE_RESTRICTIONS);
                }));
    }

    private static void loadEntityStats(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/entity_stats", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> {
                    if (!json.has("rules") || !json.get("rules").isJsonArray()) {
                        return;
                    }
                    json.getAsJsonArray("rules").forEach(element -> {
                        if (element.isJsonObject()) {
                            EntityStatRule rule = EntityStatRule.parse(element.getAsJsonObject());
                            ENTITY_RULES.computeIfAbsent(rule.entity(), ignored -> new ArrayList<>()).add(rule);
                        }
                    });
                }));
    }

    private static void loadSpawnRules(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/spawn_rules", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> {
                    if (!json.has("rules") || !json.get("rules").isJsonArray()) {
                        return;
                    }
                    json.getAsJsonArray("rules").forEach(element -> {
                        if (element.isJsonObject()) {
                            SPAWN_RULES.add(SpawnRule.parse(element.getAsJsonObject()));
                        }
                    });
                }));
    }

    private static void loadLootRules(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/loot_rules", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> {
                    if (!json.has("rules") || !json.get("rules").isJsonArray()) {
                        return;
                    }
                    json.getAsJsonArray("rules").forEach(element -> {
                        if (element.isJsonObject()) {
                            LootRule rule = LootRule.parse(element.getAsJsonObject());
                            LOOT_RULES.computeIfAbsent(rule.entity(), ignored -> new ArrayList<>()).add(rule);
                        }
                    });
                }));
    }

    private static void loadWorldEvents(MinecraftServer server) {
        server.getResourceManager().listResources("norestages/world_events", id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> readJson(resourceId, resource).ifPresent(json -> {
                    ResourceLocation id = dataId(resourceId, "norestages/world_events");
                    WorldEventGateRule rule = WorldEventGateRule.parse(id, json);
                    WORLD_EVENT_GATES.put(id, rule);
                    WORLD_EVENT_GATES_BY_EVENT.computeIfAbsent(rule.event(), ignored -> new ArrayList<>()).add(rule);
                }));
    }

    private static void readRestrictions(JsonObject json, String key, List<StageRestriction> out) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return;
        }
        json.getAsJsonArray(key).forEach(element -> {
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject object = element.getAsJsonObject();
            String targetKey = switch (key) {
                case "blocks" -> "block";
                case "items" -> "item";
                case "recipes" -> "recipe";
                default -> "dimension";
            };
            ResourceLocation target = ResourceLocation.parse(object.get(targetKey).getAsString());
            ResourceLocation stage = ResourceLocation.parse(object.get("stage").getAsString());
            StageScope scope = StageScope.parse(object.has("scope") ? object.get("scope").getAsString() : "local", StageScope.LOCAL);
            boolean use = object.has("use") && object.get("use").getAsBoolean();
            boolean place = object.has("place") && object.get("place").getAsBoolean();
            boolean dropNothing = !object.has("drop") || "nothing".equalsIgnoreCase(object.get("drop").getAsString());
            out.add(new StageRestriction(target, stage, scope, use, place, dropNothing));
        });
    }

    private static void readInlineRestrictions(ResourceLocation stage, StageScope scope, JsonObject json, String key, List<StageRestriction> out) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return;
        }
        json.getAsJsonArray(key).forEach(element -> {
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject object = element.getAsJsonObject();
            String targetKey = switch (key) {
                case "blocks" -> "block";
                case "items" -> "item";
                case "recipes" -> "recipe";
                default -> "dimension";
            };
            ResourceLocation target = ResourceLocation.parse(object.get(targetKey).getAsString());
            StageScope restrictionScope = StageScope.parse(object.has("scope") ? object.get("scope").getAsString() : scope.name(), scope);
            boolean use = object.has("use") && object.get("use").getAsBoolean();
            boolean place = object.has("place") && object.get("place").getAsBoolean();
            boolean dropNothing = !object.has("drop") || "nothing".equalsIgnoreCase(object.get("drop").getAsString());
            out.add(new StageRestriction(target, stage, restrictionScope, use, place, dropNothing));
        });
    }

    private static Optional<JsonObject> readJson(ResourceLocation id, Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            return Optional.of(GSON.fromJson(reader, JsonObject.class));
        } catch (Exception ex) {
            NoreStages.LOGGER.warn("Failed to load NoreStages json {}", id, ex);
            return Optional.empty();
        }
    }

    private static ResourceLocation dataId(ResourceLocation fileId, String folder) {
        String path = fileId.getPath();
        String prefix = folder + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), path);
    }
}
