package com.nore.stages;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NoreStagesConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SHOW_LOCKED_RECIPE_TOOLTIPS;
    private static final ModConfigSpec.BooleanValue HIDE_LOCKED_JEI_RECIPES;
    private static final ModConfigSpec.BooleanValue SHOW_ENEMY_RANK_INDICATORS;
    private static final ModConfigSpec.BooleanValue ENABLE_DEBUG_COMMANDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("norestages");
        SHOW_LOCKED_RECIPE_TOOLTIPS = builder
                .comment("Adds a tooltip to items whose recipe output is currently locked by NoreStages.")
                .define("showLockedRecipeTooltips", true);
        HIDE_LOCKED_JEI_RECIPES = builder
                .comment("When JEI is installed, hide recipes whose output is currently locked by NoreStages. The server-side craft gate still applies either way.")
                .define("hideLockedJeiRecipes", false);
        SHOW_ENEMY_RANK_INDICATORS = builder
                .comment("Shows a small icon above enemies whose configured NoreStages rank is above the player's current rank.")
                .define("showEnemyRankIndicators", true);
        ENABLE_DEBUG_COMMANDS = builder
                .comment("Registers /norestages debug commands. These commands still require operator permission.")
                .define("enableDebugCommands", true);
        builder.pop();
        SPEC = builder.build();
    }

    private NoreStagesConfig() {
    }

    public static boolean showLockedRecipeTooltips() {
        return SHOW_LOCKED_RECIPE_TOOLTIPS.get();
    }

    public static boolean hideLockedJeiRecipes() {
        return HIDE_LOCKED_JEI_RECIPES.get();
    }

    public static boolean showEnemyRankIndicators() {
        return SHOW_ENEMY_RANK_INDICATORS.get();
    }

    public static boolean enableDebugCommands() {
        return ENABLE_DEBUG_COMMANDS.get();
    }
}
