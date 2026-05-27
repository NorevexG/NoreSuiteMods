package com.nore.stages.rule;

import com.google.gson.JsonObject;
import com.nore.stages.stage.StageScope;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record SpawnRule(ResourceLocation entity, ResourceLocation stage, StageScope scope, Action action, MobCategory category, int weight, double multiplier, int minCount, int maxCount, List<ResourceLocation> dimensions, List<ResourceLocation> biomes, boolean forcePlacement) {
    public static SpawnRule parse(JsonObject json) {
        ResourceLocation entity = ResourceLocation.parse(json.get("entity").getAsString());
        ResourceLocation stage = json.has("stage") ? ResourceLocation.parse(json.get("stage").getAsString()) : null;
        StageScope scope = StageScope.parse(json.has("scope") ? json.get("scope").getAsString() : "global", StageScope.GLOBAL);
        Action action = Action.parse(json.has("action") ? json.get("action").getAsString() : "add");
        MobCategory category = json.has("category") ? MobCategory.valueOf(json.get("category").getAsString().toUpperCase(Locale.ROOT)) : null;
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 100;
        double multiplier = json.has("multiplier") ? json.get("multiplier").getAsDouble() : 1.0;
        int minCount = json.has("min_count") ? json.get("min_count").getAsInt() : 1;
        int maxCount = json.has("max_count") ? json.get("max_count").getAsInt() : minCount;
        List<ResourceLocation> dimensions = readLocations(json, "dimension", "dimensions");
        List<ResourceLocation> biomes = readLocations(json, "biome", "biomes");
        boolean forcePlacement = json.has("force_placement")
                ? json.get("force_placement").getAsBoolean()
                : action == Action.ADD && (!dimensions.isEmpty() || !biomes.isEmpty());
        return new SpawnRule(entity, stage, scope, action, category, weight, multiplier, minCount, Math.max(minCount, maxCount), List.copyOf(dimensions), List.copyOf(biomes), forcePlacement);
    }

    public boolean hasBiomeFilter() {
        return !biomes.isEmpty();
    }

    public boolean hasLocationFilter() {
        return !dimensions.isEmpty() || !biomes.isEmpty();
    }

    private static List<ResourceLocation> readLocations(JsonObject json, String singleKey, String arrayKey) {
        List<ResourceLocation> locations = new ArrayList<>();
        if (json.has(singleKey)) {
            locations.add(ResourceLocation.parse(json.get(singleKey).getAsString()));
        }
        if (json.has(arrayKey) && json.get(arrayKey).isJsonArray()) {
            json.getAsJsonArray(arrayKey).forEach(element -> locations.add(ResourceLocation.parse(element.getAsString())));
        }
        return locations;
    }

    public enum Action {
        ADD,
        SET_WEIGHT,
        MULTIPLY_WEIGHT,
        REMOVE;

        static Action parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "set_weight", "set" -> SET_WEIGHT;
                case "multiply_weight", "multiply", "mul" -> MULTIPLY_WEIGHT;
                case "remove", "disable" -> REMOVE;
                default -> ADD;
            };
        }
    }
}
