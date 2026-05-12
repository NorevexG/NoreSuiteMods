package com.nore.teams;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NoreTeamsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_DEBUG_COMMANDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("noreteams");
        ENABLE_DEBUG_COMMANDS = builder
                .comment("Enables temporary fake-player testing commands under /noreteams debug.")
                .define("enableDebugCommands", false);
        builder.pop();
        SPEC = builder.build();
    }

    private NoreTeamsConfig() {
    }

    public static boolean enableDebugCommands() {
        return ENABLE_DEBUG_COMMANDS.get();
    }
}
