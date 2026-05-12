package com.nore.teams.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nore.teams.NoreTeams;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = NoreTeams.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NoreTeams.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NoreTeamsClient {
    private static final KeyMapping OPEN_SCREEN = new KeyMapping(
            "key.noreteams.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.noreteams"
    );

    public NoreTeamsClient() {
        NeoForge.EVENT_BUS.register(ClientGameEvents.class);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN);
    }

    public static final class ClientGameEvents {
        private ClientGameEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_SCREEN.consumeClick()) {
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                minecraft.setScreen(minecraft.screen instanceof NoreTeamsScreen ? null : new NoreTeamsScreen());
            }
        }
    }
}
