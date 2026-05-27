package com.nore.stages.stage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record StageDefinition(ResourceLocation id, StageScope scope, String displayName, StageCondition condition, List<StageEffect> effects) {
    public static StageDefinition parse(ResourceLocation id, JsonObject json) {
        StageScope scope = StageScope.parse(json.has("scope") ? json.get("scope").getAsString() : "local", StageScope.LOCAL);
        String displayName = id.toString();
        if (json.has("display") && json.get("display").isJsonObject()) {
            JsonObject display = json.getAsJsonObject("display");
            if (display.has("name")) {
                displayName = display.get("name").getAsString();
            }
        }

        StageCondition condition = StageCondition.parse(json.has("conditions") ? json.getAsJsonObject("conditions") : null);
        List<StageEffect> effects = new ArrayList<>();
        if (json.has("effects") && json.get("effects").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("effects");
            array.forEach(element -> {
                if (element.isJsonObject()) {
                    effects.add(StageEffect.parse(element.getAsJsonObject()));
                }
            });
        }
        return new StageDefinition(id, scope, displayName, condition, List.copyOf(effects));
    }
}
