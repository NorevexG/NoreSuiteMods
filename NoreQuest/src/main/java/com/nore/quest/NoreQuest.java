package com.nore.quest;

import com.mojang.logging.LogUtils;
import com.nore.quest.command.NoreQuestCommand;
import com.nore.quest.ftb.FtbKillProgressBridge;
import com.nore.quest.ftb.FtbTeamsCommandBlocker;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(NoreQuest.MODID)
public class NoreQuest {
    public static final String MODID = "norequest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoreQuest() {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new FtbKillProgressBridge());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FtbTeamsCommandBlocker.block(event.getDispatcher());
        NoreQuestCommand.register(event.getDispatcher());
    }
}
