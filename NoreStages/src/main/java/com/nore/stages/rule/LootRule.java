package com.nore.stages.rule;

import com.google.gson.JsonObject;
import com.nore.stages.stage.StageScope;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record LootRule(ResourceLocation entity, ResourceLocation stage, StageScope scope, double multiplier, List<BonusDrop> bonusDrops) {
    public static LootRule parse(JsonObject json) {
        ResourceLocation entity = ResourceLocation.parse(json.get("entity").getAsString());
        ResourceLocation stage = json.has("stage") ? ResourceLocation.parse(json.get("stage").getAsString()) : null;
        StageScope scope = StageScope.parse(json.has("scope") ? json.get("scope").getAsString() : "global", StageScope.GLOBAL);
        double multiplier = json.has("multiplier") ? json.get("multiplier").getAsDouble() : 1.0;
        List<BonusDrop> bonusDrops = new ArrayList<>();
        if (json.has("drops") && json.get("drops").isJsonArray()) {
            json.getAsJsonArray("drops").forEach(element -> {
                if (element.isJsonObject()) {
                    bonusDrops.add(BonusDrop.parse(element.getAsJsonObject()));
                }
            });
        }
        return new LootRule(entity, stage, scope, multiplier, List.copyOf(bonusDrops));
    }

    public record BonusDrop(ResourceLocation item, int count, double chance) {
        static BonusDrop parse(JsonObject json) {
            ResourceLocation item = ResourceLocation.parse(json.get("item").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            double chance = json.has("chance") ? json.get("chance").getAsDouble() : 1.0;
            return new BonusDrop(item, Math.max(1, count), Math.max(0.0, Math.min(1.0, chance)));
        }
    }
}
