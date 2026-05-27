package com.nore.stages.stage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public record StageCondition(Set<String> all, Set<String> any, Set<String> not, AtLeast atLeast, Set<NumericTrigger> numericTriggers) {
    public static StageCondition empty() {
        return new StageCondition(Set.of(), Set.of(), Set.of(), AtLeast.empty(), Set.of());
    }

    public boolean matches(Set<String> facts) {
        if (!facts.containsAll(all)) {
            return false;
        }
        if (!any.isEmpty() && any.stream().noneMatch(facts::contains)) {
            return false;
        }
        if (not.stream().anyMatch(facts::contains)) {
            return false;
        }
        return atLeast.matches(facts);
    }

    public Set<ResourceLocation> requiredStages() {
        Set<ResourceLocation> stages = new HashSet<>();
        collectStageFacts(all, stages);
        collectStageFacts(any, stages);
        collectStageFacts(atLeast.facts(), stages);
        return Set.copyOf(stages);
    }

    public static StageCondition parse(JsonObject json) {
        if (json == null) {
            return empty();
        }
        return new StageCondition(
                readFactSet(json.get("all")),
                readFactSet(json.get("any")),
                readFactSet(json.get("not")),
                AtLeast.parse(json.getAsJsonObject("at_least")),
                readNumericTriggers(json));
    }

    private static Set<String> readFactSet(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return Set.of();
        }
        Set<String> facts = new LinkedHashSet<>();
        element.getAsJsonArray().forEach(item -> {
            if (item.isJsonObject()) {
                facts.add(readFact(item.getAsJsonObject()));
            }
        });
        return facts;
    }

    private static Set<NumericTrigger> readNumericTriggers(JsonObject object) {
        Set<NumericTrigger> triggers = new LinkedHashSet<>();
        readNumericTriggers(object.get("all"), triggers);
        readNumericTriggers(object.get("any"), triggers);
        JsonElement atLeast = object.get("at_least");
        if (atLeast != null && atLeast.isJsonObject()) {
            readNumericTriggers(atLeast.getAsJsonObject().get("conditions"), triggers);
        }
        return triggers;
    }

    private static void readNumericTriggers(JsonElement element, Set<NumericTrigger> triggers) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        element.getAsJsonArray().forEach(item -> {
            if (!item.isJsonObject()) {
                return;
            }
            JsonObject object = item.getAsJsonObject();
            String type = object.has("type") ? object.get("type").getAsString() : "command";
            if ("server_age".equals(type) || "scoreboard".equals(type)) {
                triggers.add(NumericTrigger.parse(object));
            }
        });
    }

    private static String readFact(JsonObject object) {
        String type = object.has("type") ? object.get("type").getAsString() : "command";
        if ("server_age".equals(type) || "scoreboard".equals(type)) {
            return NumericTrigger.parse(object).fact();
        }
        String rawId = object.has("id") ? object.get("id").getAsString()
                : object.has("objective") ? object.get("objective").getAsString()
                : object.has("entity") ? object.get("entity").getAsString()
                : object.has("item") ? object.get("item").getAsString()
                : object.has("block") ? object.get("block").getAsString()
                : object.has("dimension") ? object.get("dimension").getAsString()
                : object.has("advancement") ? object.get("advancement").getAsString()
                : "minecraft:empty";
        return new TriggerFact(type, ResourceLocation.parse(rawId)).key();
    }

    private static void collectStageFacts(Set<String> facts, Set<ResourceLocation> out) {
        for (String fact : facts) {
            if (fact.startsWith("stage:")) {
                ResourceLocation stage = ResourceLocation.tryParse(fact.substring("stage:".length()));
                if (stage != null) {
                    out.add(stage);
                }
            }
        }
    }

    public record AtLeast(int count, Set<String> facts) {
        public static AtLeast empty() {
            return new AtLeast(0, Set.of());
        }

        public boolean matches(Set<String> activeFacts) {
            if (count <= 0 || facts.isEmpty()) {
                return true;
            }
            long matched = facts.stream().filter(activeFacts::contains).count();
            return matched >= count;
        }

        static AtLeast parse(JsonObject object) {
            if (object == null) {
                return empty();
            }
            int count = object.has("count") ? object.get("count").getAsInt() : 0;
            Set<String> facts = readFactSet(object.get("conditions"));
            return new AtLeast(count, facts);
        }
    }

    public record NumericTrigger(String type, ResourceLocation id, int threshold) {
        static NumericTrigger parse(JsonObject object) {
            String type = object.has("type") ? object.get("type").getAsString() : "scoreboard";
            String rawId = object.has("id") ? object.get("id").getAsString()
                    : object.has("objective") ? object.get("objective").getAsString()
                    : type + "_" + readThreshold(type, object);
            return new NumericTrigger(type, ResourceLocation.parse(rawId), readThreshold(type, object));
        }

        public String fact() {
            return new TriggerFact(type, id).key();
        }

        public String objectiveName() {
            return id.getPath();
        }

        private static int readThreshold(String type, JsonObject object) {
            if ("server_age".equals(type) && object.has("seconds")) {
                return object.get("seconds").getAsInt() * 20;
            }
            if (object.has("ticks")) {
                return object.get("ticks").getAsInt();
            }
            if (object.has("value")) {
                return object.get("value").getAsInt();
            }
            if (object.has("score")) {
                return object.get("score").getAsInt();
            }
            if (object.has("min")) {
                return object.get("min").getAsInt();
            }
            return 0;
        }
    }
}
