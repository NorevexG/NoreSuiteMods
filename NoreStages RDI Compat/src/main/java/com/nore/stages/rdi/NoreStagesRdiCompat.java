package com.nore.stages.rdi;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(NoreStagesRdiCompat.MODID)
public final class NoreStagesRdiCompat {
    public static final String MODID = "norestages_rdi_compat";

    public NoreStagesRdiCompat(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, NoreStagesRdiCompatConfig.SPEC);
    }
}
