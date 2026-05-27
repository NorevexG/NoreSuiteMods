package com.nore.stages.rdi.client;

import com.github.alexmodguy.retrodamageindicators.Config;
import com.nore.stages.client.NoreStagesClientState;
import com.nore.stages.rdi.NoreStagesRdiCompat;
import com.nore.stages.rdi.NoreStagesRdiCompatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class RdiDangerBadgeRenderer {
    private static final ResourceLocation WARNING = ResourceLocation.fromNamespaceAndPath(NoreStagesRdiCompat.MODID, "textures/gui/rdi_threat_warning.png");
    private static final ResourceLocation DEADLY = ResourceLocation.fromNamespaceAndPath(NoreStagesRdiCompat.MODID, "textures/gui/rdi_threat_deadly.png");
    private static final int HUD_WIDTH = 208;
    private static final int HUD_HEIGHT = 78;
    private static final int SKULL_X = -18;
    private static final int SKULL_Y = -13;
    private static final int SKULL_SIZE = 64;
    private static final int Z = 60;

    private RdiDangerBadgeRenderer() {
    }

    public static void render(RenderGuiLayerEvent.Pre event, LivingEntity entity) {
        if (entity == null || Minecraft.getInstance().screen != null || !VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName())) {
            return;
        }
        if (!Config.INSTANCE.hudIndicatorEnabled.get() || !NoreStagesRdiCompatConfig.showSkullOverlay()) {
            return;
        }

        float scale = Config.INSTANCE.hudIndicatorSize.get().floatValue();
        int x = hudX(event, scale);
        int y = hudY(event, scale);
        ResourceLocation icon = iconFor(NoreStagesClientState.threat(entity.getType()));
        if (icon == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(icon, SKULL_X, SKULL_Y, Z, 0.0F, 0.0F, SKULL_SIZE, SKULL_SIZE, SKULL_SIZE, SKULL_SIZE);
        graphics.pose().popPose();
    }

    private static int hudX(RenderGuiLayerEvent.Pre event, float scale) {
        int offset = Config.INSTANCE.hudIndicatorPositionX.get();
        if (Config.INSTANCE.hudIndicatorAlignLeft.get()) {
            return offset;
        }
        return event.getGuiGraphics().guiWidth() - (int) (HUD_WIDTH * scale) - offset;
    }

    private static int hudY(RenderGuiLayerEvent.Pre event, float scale) {
        int offset = Config.INSTANCE.hudIndicatorPositionY.get();
        if (Config.INSTANCE.hudIndicatorAlignTop.get()) {
            return offset;
        }
        return event.getGuiGraphics().guiHeight() - (int) (HUD_HEIGHT * scale) - offset;
    }

    private static ResourceLocation iconFor(NoreStagesClientState.Threat threat) {
        return switch (threat) {
            case DEADLY -> DEADLY;
            case WARNING -> WARNING;
            case NONE -> null;
        };
    }
}
