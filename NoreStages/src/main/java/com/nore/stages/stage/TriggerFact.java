package com.nore.stages.stage;

import net.minecraft.resources.ResourceLocation;

public record TriggerFact(String type, ResourceLocation id) {
    public String key() {
        return type + ":" + id;
    }

    public static TriggerFact of(String type, ResourceLocation id) {
        return new TriggerFact(type, id);
    }
}
