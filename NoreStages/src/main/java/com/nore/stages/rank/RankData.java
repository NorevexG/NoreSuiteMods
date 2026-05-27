package com.nore.stages.rank;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RankData(List<ResourceLocation> ranks, ResourceLocation defaultRank, int warningDifference, int deadlyDifference) {
    public static RankData empty() {
        return new RankData(List.of(), null, 1, 2);
    }

    public static RankData parse(JsonObject json, RankData fallback) {
        List<ResourceLocation> ranks = new ArrayList<>(fallback.ranks());
        if (json.has("ranks") && json.get("ranks").isJsonArray()) {
            ranks.clear();
            JsonArray array = json.getAsJsonArray("ranks");
            for (JsonElement element : array) {
                ResourceLocation rank = readRank(element);
                if (rank != null && !ranks.contains(rank)) {
                    ranks.add(rank);
                }
            }
        }
        ResourceLocation defaultRank = fallback.defaultRank();
        if (json.has("default")) {
            defaultRank = ResourceLocation.parse(json.get("default").getAsString());
        } else if (defaultRank == null && !ranks.isEmpty()) {
            defaultRank = ranks.getFirst();
        }
        int warningDifference = fallback.warningDifference();
        int deadlyDifference = fallback.deadlyDifference();
        if (json.has("warning_difference")) {
            warningDifference = json.get("warning_difference").getAsInt();
        }
        if (json.has("deadly_difference")) {
            deadlyDifference = json.get("deadly_difference").getAsInt();
        }
        if (json.has("indicators") && json.get("indicators").isJsonObject()) {
            JsonObject indicators = json.getAsJsonObject("indicators");
            if (indicators.has("warning_difference")) {
                warningDifference = indicators.get("warning_difference").getAsInt();
            }
            if (indicators.has("deadly_difference")) {
                deadlyDifference = indicators.get("deadly_difference").getAsInt();
            }
        }
        return new RankData(List.copyOf(ranks), defaultRank, Math.max(1, warningDifference), Math.max(1, deadlyDifference));
    }

    private static ResourceLocation readRank(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return ResourceLocation.parse(element.getAsString());
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("id")) {
                return ResourceLocation.parse(object.get("id").getAsString());
            }
        }
        return null;
    }
}
