package com.nore.stages.rdi.client;

import com.nore.stages.rdi.NoreStagesRdiCompat;
import com.nore.stages.rdi.NoreStagesRdiCompatConfig;
import net.minecraft.resources.ResourceLocation;

public final class RdiHudThemeTextures {
    private static final ResourceLocation ORIGINAL = ResourceLocation.fromNamespaceAndPath("retrodamageindicators", "textures/gui/damage_indicator.png");
    private static final ResourceLocation SILVER = ResourceLocation.fromNamespaceAndPath(NoreStagesRdiCompat.MODID, "textures/gui/themes/damage_indicator_silver.png");
    private static final ResourceLocation PLATINUM = ResourceLocation.fromNamespaceAndPath(NoreStagesRdiCompat.MODID, "textures/gui/themes/damage_indicator_platinum.png");
    private static final ResourceLocation BROWN = ResourceLocation.fromNamespaceAndPath(NoreStagesRdiCompat.MODID, "textures/gui/themes/damage_indicator_brown.png");

    private RdiHudThemeTextures() {
    }

    public static ResourceLocation replaceMainHudTexture(ResourceLocation texture) {
        if (!ORIGINAL.equals(texture)) {
            return texture;
        }
        return switch (NoreStagesRdiCompatConfig.hudTheme()) {
            case SILVER -> SILVER;
            case PLATINUM -> PLATINUM;
            case BROWN -> BROWN;
            case DEFAULT -> texture;
        };
    }
}
