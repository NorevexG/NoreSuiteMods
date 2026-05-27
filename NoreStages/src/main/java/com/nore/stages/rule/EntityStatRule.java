package com.nore.stages.rule;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import com.nore.stages.stage.StageScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record EntityStatRule(ResourceLocation entity, ResourceLocation stage, StageScope scope, ResourceLocation rank, boolean persistent, Map<ResourceLocation, AttributeChange> attributes, List<EffectChange> effects, List<RandomEffectGroup> randomEffects) {
    public static EntityStatRule parse(JsonObject json) {
        ResourceLocation entity = ResourceLocation.parse(json.get("entity").getAsString());
        ResourceLocation stage = json.has("stage") ? ResourceLocation.parse(json.get("stage").getAsString()) : null;
        StageScope scope = StageScope.parse(json.has("scope") ? json.get("scope").getAsString() : "global", StageScope.GLOBAL);
        ResourceLocation rank = json.has("rank") ? ResourceLocation.parse(json.get("rank").getAsString()) : null;
        boolean persistent = json.has("persistent") && json.get("persistent").getAsBoolean();
        Map<ResourceLocation, AttributeChange> attributes = new LinkedHashMap<>();
        if (json.has("attributes") && json.get("attributes").isJsonObject()) {
            json.getAsJsonObject("attributes").entrySet().forEach(entry -> {
                JsonObject change = entry.getValue().getAsJsonObject();
                String op = change.has("operation") ? change.get("operation").getAsString() : "set";
                double value = change.has("value") ? change.get("value").getAsDouble() : 0.0;
                attributes.put(ResourceLocation.parse(entry.getKey()), new AttributeChange(Operation.parse(op), value));
            });
        }
        List<EffectChange> effects = new ArrayList<>();
        if (json.has("effects") && json.get("effects").isJsonArray()) {
            json.getAsJsonArray("effects").forEach(element -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject effect = element.getAsJsonObject();
                ResourceLocation id = ResourceLocation.parse(effect.get("effect").getAsString());
                int duration = effect.has("duration") ? effect.get("duration").getAsInt() : -1;
                int amplifier = effect.has("amplifier") ? effect.get("amplifier").getAsInt() : 0;
                boolean ambient = effect.has("ambient") && effect.get("ambient").getAsBoolean();
                boolean visible = !effect.has("visible") || effect.get("visible").getAsBoolean();
                boolean showIcon = !effect.has("show_icon") || effect.get("show_icon").getAsBoolean();
                double chance = effect.has("chance") ? effect.get("chance").getAsDouble() : 1.0;
                effects.add(new EffectChange(id, duration, amplifier, ambient, visible, showIcon, clampChance(chance)));
            });
        }
        List<RandomEffectGroup> randomEffects = new ArrayList<>();
        if (json.has("random_effects") && json.get("random_effects").isJsonArray()) {
            json.getAsJsonArray("random_effects").forEach(element -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject group = element.getAsJsonObject();
                double chance = group.has("chance") ? group.get("chance").getAsDouble() : 1.0;
                List<EffectChange> choices = new ArrayList<>();
                if (group.has("effects") && group.get("effects").isJsonArray()) {
                    group.getAsJsonArray("effects").forEach(choice -> {
                        if (!choice.isJsonObject()) {
                            return;
                        }
                        JsonObject effect = choice.getAsJsonObject();
                        ResourceLocation id = ResourceLocation.parse(effect.get("effect").getAsString());
                        int duration = effect.has("duration") ? effect.get("duration").getAsInt() : -1;
                        int amplifier = effect.has("amplifier") ? effect.get("amplifier").getAsInt() : 0;
                        boolean ambient = effect.has("ambient") && effect.get("ambient").getAsBoolean();
                        boolean visible = !effect.has("visible") || effect.get("visible").getAsBoolean();
                        boolean showIcon = !effect.has("show_icon") || effect.get("show_icon").getAsBoolean();
                        choices.add(new EffectChange(id, duration, amplifier, ambient, visible, showIcon, 1.0));
                    });
                }
                if (!choices.isEmpty()) {
                    randomEffects.add(new RandomEffectGroup(clampChance(chance), List.copyOf(choices)));
                }
            });
        }
        return new EntityStatRule(entity, stage, scope, rank, persistent, Map.copyOf(attributes), List.copyOf(effects), List.copyOf(randomEffects));
    }

    private static double clampChance(double chance) {
        return Math.max(0.0, Math.min(1.0, chance));
    }

    public record AttributeChange(Operation operation, double value) {
    }

    public record EffectChange(ResourceLocation effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, double chance) {
    }

    public record RandomEffectGroup(double chance, List<EffectChange> effects) {
    }

    public enum Operation {
        SET,
        ADD,
        MULTIPLY;

        static Operation parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "add" -> ADD;
                case "multiply", "mul" -> MULTIPLY;
                default -> SET;
            };
        }
    }
}
