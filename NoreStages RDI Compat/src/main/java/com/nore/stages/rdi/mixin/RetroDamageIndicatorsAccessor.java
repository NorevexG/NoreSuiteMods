package com.nore.stages.rdi.mixin;

import com.github.alexmodguy.retrodamageindicators.RetroDamageIndicators;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RetroDamageIndicators.class, remap = false)
public interface RetroDamageIndicatorsAccessor {
    @Accessor("damageIndicatorEntity")
    static LivingEntity norestagesRdiCompat$getDamageIndicatorEntity() {
        throw new AssertionError();
    }
}
