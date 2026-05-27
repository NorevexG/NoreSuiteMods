package com.nore.stages.rule;

import com.nore.stages.stage.StageScope;
import net.minecraft.resources.ResourceLocation;

public record StageRestriction(ResourceLocation target, ResourceLocation stage, StageScope scope, boolean use, boolean place, boolean dropNothing) {
}
