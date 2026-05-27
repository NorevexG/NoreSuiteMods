package com.nore.stages.stage;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;

public record StageEffect(Type type, String command, ResourceLocation attribute, ResourceLocation recipe, ResourceLocation quest, ResourceLocation rank, double value, AttributeModifier.Operation operation) {
    public static StageEffect parse(JsonObject json) {
        Type type = Type.parse(json.has("type") ? json.get("type").getAsString() : "none");
        String command = json.has("command") ? json.get("command").getAsString() : "";
        ResourceLocation attribute = json.has("attribute") ? ResourceLocation.parse(json.get("attribute").getAsString()) : null;
        ResourceLocation recipe = json.has("recipe") ? ResourceLocation.parse(json.get("recipe").getAsString()) : null;
        ResourceLocation quest = json.has("quest") ? ResourceLocation.parse(json.get("quest").getAsString()) : null;
        ResourceLocation rank = json.has("rank") ? ResourceLocation.parse(json.get("rank").getAsString()) : null;
        double value = json.has("value") ? json.get("value").getAsDouble() : 0.0;
        AttributeModifier.Operation operation = parseOperation(json.has("operation") ? json.get("operation").getAsString() : "add_value");
        return new StageEffect(type, command, attribute, recipe, quest, rank, value, operation);
    }

    private static AttributeModifier.Operation parseOperation(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total", "multiply" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    public enum Type {
        PLAYER_COMMAND,
        SERVER_COMMAND,
        ATTRIBUTE,
        UNLOCK_RECIPE,
        UNLOCK_QUEST,
        SET_RANK,
        NONE;

        static Type parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "player_command", "player-command" -> PLAYER_COMMAND;
                case "server_command", "command", "server-command" -> SERVER_COMMAND;
                case "attribute", "grant_attribute" -> ATTRIBUTE;
                case "unlock_recipe", "recipe" -> UNLOCK_RECIPE;
                case "unlock_quest", "quest" -> UNLOCK_QUEST;
                case "set_rank", "rank", "grant_rank" -> SET_RANK;
                default -> NONE;
            };
        }
    }
}
