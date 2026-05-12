package com.nore.teams;

import com.mojang.logging.LogUtils;
import com.nore.teams.command.NoreTeamsCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(NoreTeams.MODID)
public class NoreTeams {
    public static final String MODID = "noreteams";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoreTeams(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, NoreTeamsConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NoreTeamsCommand.register(event.getDispatcher());
    }
}
