package com.nore.stages;

import com.mojang.logging.LogUtils;
import com.nore.stages.command.NoreStagesCommand;
import com.nore.stages.event.NoreStagesEvents;
import com.nore.stages.network.NoreStagesNetwork;
import com.nore.stages.registry.StageDataRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(NoreStages.MODID)
public class NoreStages {
    public static final String MODID = "norestages";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoreStages(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, NoreStagesConfig.SPEC);
        modEventBus.addListener(this::onConfigReload);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new NoreStagesEvents());
        registerFtbQuestEvents();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NoreStagesCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        StageDataRegistry.reload(event.getServer());
        event.getServer().getPlayerList().getPlayers().forEach(NoreStagesNetwork::sendSnapshot);
    }

    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MODID) && FMLEnvironment.dist == Dist.CLIENT) {
            NoreStagesNetwork.refreshClientIntegrations();
        }
    }

    private static void registerFtbQuestEvents() {
        if (!ModList.get().isLoaded("ftbquests")) {
            return;
        }
        try {
            Class.forName("com.nore.stages.compat.ftb.FtbQuestEvents")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("FTB Quests is loaded, but NoreStages could not register quest completion hooks.", ex);
        }
    }
}
