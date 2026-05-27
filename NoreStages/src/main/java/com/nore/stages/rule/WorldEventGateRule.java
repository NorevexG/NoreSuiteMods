package com.nore.stages.rule;

import com.google.gson.JsonObject;
import com.nore.stages.stage.StageScope;
import net.minecraft.resources.ResourceLocation;

public record WorldEventGateRule(ResourceLocation id, ResourceLocation event, ResourceLocation stage, StageScope scope, double multiplier) {
    public static WorldEventGateRule parse(ResourceLocation id, JsonObject json) {
        ResourceLocation event = ResourceLocation.parse(json.get("event").getAsString());
        ResourceLocation stage = json.has("stage") ? ResourceLocation.parse(json.get("stage").getAsString()) : null;
        StageScope scope = StageScope.parse(json.has("scope") ? json.get("scope").getAsString() : "global", StageScope.GLOBAL);
        double multiplier = json.has("allow") ? (json.get("allow").getAsBoolean() ? 1.0 : 0.0)
                : json.has("enabled") ? (json.get("enabled").getAsBoolean() ? 1.0 : 0.0)
                : json.has("multiplier") ? json.get("multiplier").getAsDouble()
                : json.has("frequency") ? json.get("frequency").getAsDouble()
                : 1.0;
        return new WorldEventGateRule(id, event, stage, scope, Math.max(0.0, multiplier));
    }
}
