package com.nore.stages.rdi;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NoreStagesRdiCompatConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<HudTheme> HUD_THEME;
    private static final ModConfigSpec.BooleanValue SHOW_SKULL_OVERLAY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("retroDamageIndicators");
        HUD_THEME = builder
                .comment("Texture theme for Retro Damage Indicators' main HUD frame.")
                .defineEnum("hudTheme", HudTheme.SILVER);
        SHOW_SKULL_OVERLAY = builder
                .comment("Shows a skull overlay on Retro Damage Indicators when the displayed entity is above the player's NoreStages rank.")
                .define("showSkullOverlay", true);
        builder.pop();
        SPEC = builder.build();
    }

    private NoreStagesRdiCompatConfig() {
    }

    public static HudTheme hudTheme() {
        return HUD_THEME.get();
    }

    public static boolean showSkullOverlay() {
        return SHOW_SKULL_OVERLAY.get();
    }

    public enum HudTheme {
        DEFAULT,
        SILVER,
        PLATINUM,
        BROWN
    }
}
