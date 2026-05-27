package com.nore.stages.service;

import com.nore.stages.NoreStages;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.rule.WorldEventGateRule;
import com.nore.stages.stage.StageScope;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.village.VillageSiegeEvent;

public final class WorldEventGateService {
    public static final ResourceLocation VANILLA_RAID = ResourceLocation.withDefaultNamespace("raid");
    public static final ResourceLocation PATROL = ResourceLocation.withDefaultNamespace("patrol");
    public static final ResourceLocation VILLAGE_SIEGE = ResourceLocation.withDefaultNamespace("village_siege");

    private WorldEventGateService() {
    }

    public static boolean shouldSuppress(ServerLevel level, ResourceLocation event) {
        return !isAllowed(level, event);
    }

    public static boolean isAllowed(ServerLevel level, ResourceLocation event) {
        var rules = StageDataRegistry.worldEventGates(event);
        if (rules.isEmpty()) {
            return true;
        }
        for (WorldEventGateRule rule : rules) {
            if (isActive(level, rule) && rule.multiplier() > 0.0) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldSuppressVanillaRaid(ServerPlayer player, BlockPos pos) {
        if (shouldSuppress(player.serverLevel(), VANILLA_RAID)) {
            player.clearRaidOmenPosition();
            removeEffect(player, MobEffects.RAID_OMEN);
            return true;
        }
        return false;
    }

    public static void suppressLockedRaidOmen(ServerPlayer player) {
        if (shouldSuppress(player.serverLevel(), VANILLA_RAID)) {
            player.clearRaidOmenPosition();
            removeEffect(player, MobEffects.RAID_OMEN);
        }
    }

    public static void handleVillageSiege(VillageSiegeEvent event) {
        if (event.getLevel() instanceof ServerLevel level && shouldSuppress(level, VILLAGE_SIEGE)) {
            event.setCanceled(true);
        }
    }

    public static int activeRuleCount(ServerLevel level) {
        int count = 0;
        for (WorldEventGateRule rule : StageDataRegistry.worldEventGates()) {
            if (isActive(level, rule)) {
                count++;
            }
        }
        return count;
    }

    public static String describe(WorldEventGateRule rule, ServerLevel level) {
        String stageText = rule.stage() == null ? "always" : rule.stage().toString();
        String activeText = isActive(level, rule) ? "active" : "locked";
        String gateText = isActive(level, rule) && rule.multiplier() > 0.0 ? "allowed" : "blocked";
        return rule.id() + " event=" + rule.event() + " gate=" + gateText + " stage=" + stageText + " " + activeText;
    }

    private static boolean isActive(ServerLevel level, WorldEventGateRule rule) {
        if (rule.stage() == null) {
            return true;
        }
        if (rule.scope() != StageScope.GLOBAL) {
            NoreStages.LOGGER.warn("Skipped local NoreStages world event gate {} because world events are global.", rule.id());
            return false;
        }
        return NoreStagesSavedData.get(level).hasGlobalStage(rule.stage());
    }

    private static void removeEffect(ServerPlayer player, Holder<MobEffect> effect) {
        if (player.hasEffect(effect)) {
            player.removeEffect(effect);
        }
    }
}
