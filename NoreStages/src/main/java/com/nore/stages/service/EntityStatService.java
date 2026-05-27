package com.nore.stages.service;

import com.nore.stages.NoreStages;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.rule.EntityStatRule;
import com.nore.stages.stage.StageScope;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Optional;

public final class EntityStatService {
    private EntityStatService() {
    }

    public static void apply(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        for (EntityStatRule rule : StageDataRegistry.entityRules(entityId)) {
            if (!isActive(entity, rule)) {
                continue;
            }
            rule.attributes().forEach((attributeId, change) -> applyAttribute(entity, entityId, attributeId, change, true));
            rule.effects().forEach(effect -> applyEffect(entity, entityId, effect));
            rule.randomEffects().forEach(group -> applyRandomEffect(entity, entityId, group));
        }
    }

    public static void applyPersistent(LivingEntity entity) {
        if (!entity.isAlive()) {
            return;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        for (EntityStatRule rule : StageDataRegistry.entityRules(entityId)) {
            if (!rule.persistent() || !isActive(entity, rule)) {
                continue;
            }
            rule.attributes().forEach((attributeId, change) -> applyAttribute(entity, entityId, attributeId, change, false));
        }
    }

    private static boolean isActive(LivingEntity entity, EntityStatRule rule) {
        if (rule.stage() == null) {
            return true;
        }
        if (rule.scope() != StageScope.GLOBAL) {
            NoreStages.LOGGER.warn("Skipped local NoreStages entity stat rule for {} because spawned entities have no stable local subject.", rule.entity());
            return false;
        }
        return NoreStagesSavedData.get(entity.level()).hasGlobalStage(rule.stage());
    }

    private static void applyAttribute(LivingEntity entity, ResourceLocation entityId, ResourceLocation attributeId, EntityStatRule.AttributeChange change, boolean fillHealth) {
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
        if (holder.isEmpty()) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages entity attribute {} for {}", attributeId, entityId);
            return;
        }
        AttributeInstance instance = entity.getAttribute(holder.get());
        if (instance == null) {
            NoreStages.LOGGER.warn("Skipped unsupported NoreStages entity attribute {} for {}", attributeId, entityId);
            return;
        }
        double current = instance.getBaseValue();
        double value = switch (change.operation()) {
            case ADD -> current + change.value();
            case MULTIPLY -> current * change.value();
            case SET -> change.value();
        };
        instance.setBaseValue(value);
        if (attributeId.equals(ResourceLocation.withDefaultNamespace("generic.max_health")) && entity.getHealth() > (float) value) {
            entity.setHealth((float) value);
        } else if (fillHealth && attributeId.equals(ResourceLocation.withDefaultNamespace("generic.max_health")) && entity.getHealth() < (float) value) {
            entity.setHealth((float) value);
        }
    }

    private static void applyEffect(LivingEntity entity, ResourceLocation entityId, EntityStatRule.EffectChange change) {
        if (entity.getRandom().nextDouble() > change.chance()) {
            return;
        }
        addEffect(entity, entityId, change);
    }

    private static void applyRandomEffect(LivingEntity entity, ResourceLocation entityId, EntityStatRule.RandomEffectGroup group) {
        if (group.effects().isEmpty() || entity.getRandom().nextDouble() > group.chance()) {
            return;
        }
        EntityStatRule.EffectChange change = group.effects().get(entity.getRandom().nextInt(group.effects().size()));
        addEffect(entity, entityId, change);
    }

    private static void addEffect(LivingEntity entity, ResourceLocation entityId, EntityStatRule.EffectChange change) {
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(change.effect());
        if (holder.isEmpty()) {
            NoreStages.LOGGER.warn("Skipped unknown NoreStages mob effect {} for {}", change.effect(), entityId);
            return;
        }
        entity.addEffect(new MobEffectInstance(holder.get(), change.duration(), change.amplifier(), change.ambient(), change.visible(), change.showIcon()));
    }
}
