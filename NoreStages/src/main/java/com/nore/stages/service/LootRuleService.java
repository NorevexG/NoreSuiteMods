package com.nore.stages.service;

import com.nore.stages.NoreStages;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.rule.LootRule;
import com.nore.stages.stage.StageScope;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

public final class LootRuleService {
    private LootRuleService() {
    }

    public static void apply(LivingDropsEvent event) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        for (LootRule rule : StageDataRegistry.lootRules(entityId)) {
            if (!isActive(event.getEntity().level(), rule)) {
                continue;
            }
            multiplyExistingDrops(event, rule.multiplier());
            addBonusDrops(event, rule.bonusDrops());
        }
    }

    public static int activeRuleCount(Level level) {
        int count = 0;
        for (LootRule rule : StageDataRegistry.lootRules()) {
            if (isActive(level, rule)) {
                count++;
            }
        }
        return count;
    }

    public static String describe(LootRule rule, Level level) {
        String stageText = rule.stage() == null ? "always" : rule.stage().toString();
        String activeText = isActive(level, rule) ? "active" : "inactive";
        return rule.entity() + " multiplier=" + rule.multiplier() + " drops=" + rule.bonusDrops().size() + " stage=" + stageText + " " + activeText;
    }

    private static boolean isActive(Level level, LootRule rule) {
        if (rule.stage() == null) {
            return true;
        }
        if (rule.scope() != StageScope.GLOBAL) {
            NoreStages.LOGGER.warn("Skipped local NoreStages loot rule for {} because entity loot changes are global.", rule.entity());
            return false;
        }
        return NoreStagesSavedData.get(level).hasGlobalStage(rule.stage());
    }

    private static void multiplyExistingDrops(LivingDropsEvent event, double multiplier) {
        if (multiplier <= 1.0 || event.getDrops().isEmpty()) {
            return;
        }
        int guaranteedCopies = (int) multiplier - 1;
        double fractionalChance = multiplier - Math.floor(multiplier);
        List<ItemEntity> originalDrops = new ArrayList<>(event.getDrops());
        for (ItemEntity drop : originalDrops) {
            for (int i = 0; i < guaranteedCopies; i++) {
                event.getDrops().add(copyDrop(drop));
            }
            if (fractionalChance > 0.0 && event.getEntity().getRandom().nextDouble() < fractionalChance) {
                event.getDrops().add(copyDrop(drop));
            }
        }
    }

    private static void addBonusDrops(LivingDropsEvent event, List<LootRule.BonusDrop> drops) {
        for (LootRule.BonusDrop drop : drops) {
            if (event.getEntity().getRandom().nextDouble() > drop.chance()) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(drop.item());
            if (item == null) {
                NoreStages.LOGGER.warn("Skipped unknown NoreStages loot item {}", drop.item());
                continue;
            }
            event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(item, drop.count())));
        }
    }

    private static ItemEntity copyDrop(ItemEntity drop) {
        return new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
    }
}
