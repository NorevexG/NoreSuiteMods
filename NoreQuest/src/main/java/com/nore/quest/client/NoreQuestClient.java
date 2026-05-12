package com.nore.quest.client;

import com.nore.quest.NoreQuest;
import com.nore.quest.ftb.Reflector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Consumer;

@Mod(value = NoreQuest.MODID, dist = Dist.CLIENT)
public final class NoreQuestClient {
    public NoreQuestClient() {
        NeoForge.EVENT_BUS.register(this);
        hideFtbTeamsSidebarButtons();
    }

    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        Screen screen = event.getNewScreen();
        if (screen == null) {
            return;
        }
        String className = screen.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("ftbteams") || className.contains("ftb_teams")) {
            event.setNewScreen(new FtbTeamsDisabledScreen());
        }
    }

    private static void hideFtbTeamsSidebarButtons() {
        try {
            Class<?> eventClass = Class.forName("dev.ftb.mods.ftblibrary.api.sidebar.SidebarButtonCreatedEvent");
            Field eventField = eventClass.getField("EVENT");
            Object eventBus = eventField.get(null);
            Consumer<Object> listener = event -> {
                Object button = Reflector.call(event, "getButton").orElse(null);
                Object id = Reflector.call(button, "getId").orElse(null);
                if (id != null && id.toString().startsWith("ftbteams:")) {
                    Reflector.invoke(button, "setForceHidden", true);
                }
            };
            Reflector.invoke(eventBus, "register", listener);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static final class FtbTeamsDisabledScreen extends Screen {
        private FtbTeamsDisabledScreen() {
            super(Component.literal("FTB Teams Disabled"));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(this.font, "FTB Teams is disabled. Use /noreteams instead.", this.width / 2, this.height / 2, 0xFFFFFF);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}
