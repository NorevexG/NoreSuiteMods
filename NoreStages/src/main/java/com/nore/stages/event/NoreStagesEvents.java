package com.nore.stages.event;

import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.rule.StageRestriction;
import com.nore.stages.network.NoreStagesNetwork;
import com.nore.stages.service.EntityStatService;
import com.nore.stages.service.LootRuleService;
import com.nore.stages.service.SpawnRuleService;
import com.nore.stages.service.StageService;
import com.nore.stages.service.WorldEventGateService;
import com.nore.stages.stage.StageScope;
import com.nore.stages.stage.TriggerFact;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillageSiegeEvent;

public class NoreStagesEvents {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StageService.syncPlayerEffects(player);
            NoreStagesNetwork.sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StageService.syncPlayerEffects(player);
            NoreStagesNetwork.sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        for (String tag : player.getTags()) {
            ResourceLocation tagId = ResourceLocation.tryParse(tag);
            if (tagId != null) {
                StageService.recordFact(player, TriggerFact.of("tag", tagId).key());
            }
        }
        StageService.pollNumericTriggers(player);
        WorldEventGateService.suppressLockedRaidOmen(player);
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StageService.recordFact(player, TriggerFact.of("advancement", event.getAdvancement().id()).key());
        }
    }

    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            StageService.recordFact(player, TriggerFact.of("entity_killed", entityId).key());
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LootRuleService.apply(event);
    }

    @SubscribeEvent
    public void onItemPickedUp(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getOriginalStack().getItem());
            StageService.recordFact(player, TriggerFact.of("item_obtained", itemId).key());
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
            StageService.recordFact(player, TriggerFact.of("item_crafted", itemId).key());
        }
    }

    @SubscribeEvent
    public void onItemUsed(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
            if (blocked(player, itemId, true, false)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            StageService.recordFact(player, TriggerFact.of("item_used", itemId).key());
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
            if (blocked(player, itemId, true, true)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            StageService.recordFact(player, TriggerFact.of("item_used", itemId).key());
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
            StageService.recordFact(player, TriggerFact.of("block_placed", blockId).key());
        }
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
            StageService.recordFact(player, TriggerFact.of("block_broken", blockId).key());
        }
    }

    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
            for (StageRestriction restriction : StageDataRegistry.blockRestrictions(blockId)) {
                if (!StageService.hasStage(player, restriction.stage(), restriction.scope()) && restriction.dropNothing()) {
                    event.getDrops().clear();
                    event.setDroppedExperience(0);
                    StageService.tellBlocked(player, restriction.stage());
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StageService.recordFact(player, TriggerFact.of("dimension_entered", event.getTo().location()).key());
        }
    }

    @SubscribeEvent
    public void onTravelDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (StageRestriction restriction : StageDataRegistry.dimensionRestrictions(event.getDimension().location())) {
                if (!StageService.hasStage(player, restriction.stage(), restriction.scope())) {
                    event.setCanceled(true);
                    StageService.tellBlocked(player, restriction.stage());
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && !event.loadedFromDisk()) {
            SpawnRuleService.noteEntityJoined(living);
            EntityStatService.apply(living);
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            EntityStatService.applyPersistent(living);
        }
    }

    @SubscribeEvent
    public void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        SpawnRuleService.apply(event);
    }

    @SubscribeEvent
    public void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        SpawnRuleService.allowSpawnPlacement(event);
    }

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        SpawnRuleService.allowPosition(event);
    }

    @SubscribeEvent
    public void onVillageSiege(VillageSiegeEvent event) {
        WorldEventGateService.handleVillageSiege(event);
    }

    private static boolean blocked(ServerPlayer player, ResourceLocation itemId, boolean use, boolean place) {
        for (StageRestriction restriction : StageDataRegistry.itemRestrictions(itemId)) {
            boolean relevant = use && restriction.use() || place && restriction.place();
            if (relevant && !StageService.hasStage(player, restriction.stage(), restriction.scope())) {
                StageService.tellBlocked(player, restriction.stage());
                return true;
            }
        }
        return false;
    }
}
