package com.nore.stages.client;

import com.nore.stages.NoreStages;
import com.nore.stages.NoreStagesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = NoreStages.MODID, dist = Dist.CLIENT)
public final class NoreStagesClient {
    public NoreStagesClient() {
        NeoForge.EVENT_BUS.register(ClientGameEvents.class);
    }

    public static final class ClientGameEvents {
        private ClientGameEvents() {
        }

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            if (event.getItemStack().isEmpty()) {
                return;
            }
            if (!NoreStagesConfig.showLockedRecipeTooltips()) {
                return;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
            NoreStagesClientState.lockedStages(itemId).ifPresent(stages -> {
                event.getToolTip().add(Component.literal("Locked Recipe").withStyle(ChatFormatting.RED));
                stages.stream().limit(2).forEach(stage -> event.getToolTip()
                        .add(Component.literal("Required stage: " + stage).withStyle(ChatFormatting.GRAY)));
                if (stages.size() > 2) {
                    event.getToolTip().add(Component.literal("Required stages: " + (stages.size() - 2) + " more").withStyle(ChatFormatting.GRAY));
                }
            });
        }

        @SubscribeEvent
        public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
            ThreatIndicatorRenderer.render(event);
        }
    }
}
