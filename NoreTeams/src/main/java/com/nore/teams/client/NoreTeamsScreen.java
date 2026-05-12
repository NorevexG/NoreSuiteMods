package com.nore.teams.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nore.teams.NoreTeams;
import com.nore.teams.network.NoreTeamsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class NoreTeamsScreen extends Screen {
    private static final String TEXTURES_PROPERTY = "textures";
    private static final String SKIN_CACHE_FILE = "noreteams-skin-cache.properties";
    private static final Map<UUID, GameProfile> SKIN_PROFILE_CACHE = new HashMap<>();
    private static final Map<UUID, ResourceLocation> SESSION_SKIN_CACHE = new HashMap<>();
    private static boolean skinCacheLoaded;

    private static final int UI_SIZE = 256;
    private static final int SOCIAL_HEIGHT = 184;
    private static final int PASSIVE_REFRESH_INTERVAL = 40;

    private static final int CLOSE_X = 235;
    private static final int CLOSE_Y = 10;
    private static final int TOP_MAIL_X = 194;
    private static final int TOP_MAIL_Y = 10;
    private static final int MAIL_NOTIFICATION_X = 202;
    private static final int MAIL_NOTIFICATION_Y = 8;
    private static final int TEAMS_TAB_X = 84;
    private static final int MAIL_TEAMS_TAB_X = 83;
    private static final int PLAYERS_TAB_X = 16;
    private static final int TAB_Y = 21;
    private static final int MAIL_TAB_Y = 21;
    private static final int MAIL_CLOSE_Y = 10;

    private static final int ROW_X = 21;
    private static final int ROW_Y = 50;
    private static final int ROW_WIDTH = 199;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_STEP = 19;
    private static final int PLAYER_VISIBLE_ROWS = 6;
    private static final int TEAM_VISIBLE_ROWS = 5;
    private static final int HEAD_X_OFFSET = 6;
    private static final int HEAD_Y_OFFSET = 5;
    private static final int HEAD_SIZE = 8;
    private static final int NAME_X_OFFSET = 20;
    private static final int NAME_Y_OFFSET = 6;
    private static final int PROFILE_NAME_GAP = 22;
    private static final int CROWN_X_OFFSET = 184;
    private static final int CROWN_Y_OFFSET = 6;
    private static final int ADMIN_RANK_X_OFFSET = 185;
    private static final int ADMIN_RANK_Y_OFFSET = 5;
    private static final int MODERATOR_RANK_X_OFFSET = 185;
    private static final int MODERATOR_RANK_Y_OFFSET = 7;

    private static final int SCROLL_X = 228;
    private static final int SCROLL_PREV_Y = 50;
    private static final int SCROLL_NEXT_Y = 156;
    private static final int SCROLL_THUMB_MIN_Y = 58;
    private static final int SCROLL_THUMB_MAX_Y = 142;
    private static final int TEAM_SCROLL_NEXT_Y = 137;
    private static final int TEAM_SCROLL_THUMB_MAX_Y = 123;

    private static final int TEAM_BUTTON_X = 19;
    private static final int TEAM_BUTTON_Y = 149;
    private static final int TEAM_BUTTON_WIDTH = 76;
    private static final int TEAM_BUTTON_HEIGHT = 16;

    private static final int POPUP_X = 136;
    private static final int POPUP_Y = 24;
    private static final int POPUP_ACCEPT_X = 17;
    private static final int POPUP_ACCEPT_Y = 34;
    private static final int POPUP_DECLINE_X = 69;
    private static final int POPUP_DECLINE_Y = 34;
    private static final int POPUP_VIEW_ALL_X = 7;
    private static final int POPUP_VIEW_ALL_Y = 55;
    private static final int POPUP_PROFILE_X_OFFSET = 1;
    private static final int POPUP_PROFILE_Y_OFFSET = 12;
    private static final int POPUP_FAKE_PROFILE_X_OFFSET = 9;
    private static final int POPUP_FAKE_PROFILE_Y_OFFSET = 11;

    private static final int MAIL_CARD_X = 36;
    private static final int MAIL_CARD_FIRST_Y = 59;
    private static final int MAIL_CARD_STEP = 50;
    private static final int MAIL_ACCEPT_X = 12;
    private static final int MAIL_ACCEPT_Y = 27;
    private static final int MAIL_DECLINE_X = 64;
    private static final int MAIL_DECLINE_Y = 27;
    private static final int MAIL_SCROLL_X = 218;
    private static final int MAIL_SCROLL_PREV_Y = 54;
    private static final int MAIL_SCROLL_NEXT_Y = 152;
    private static final int MAIL_SCROLL_THUMB_MIN_Y = 62;
    private static final int MAIL_SCROLL_THUMB_MAX_Y = 139;
    private static final int MAIL_VISIBLE_ROWS = 2;
    private static final int MAIL_PROFILE_X_OFFSET = -4;
    private static final int MAIL_PROFILE_Y_OFFSET = 5;
    private static final int MAIL_FAKE_PROFILE_X_OFFSET = 4;
    private static final int MAIL_FAKE_PROFILE_Y_OFFSET = 4;

    private static final ResourceLocation PLAYER_WINDOW = tex("player_window_ui");
    private static final ResourceLocation TEAM_WINDOW = tex("team_window_ui");
    private static final ResourceLocation MAIL_WINDOW = tex("mail/mail_window_ui");
    private static final ResourceLocation PLAYERS_BUTTON = tex("players_button");
    private static final ResourceLocation TEAMS_BUTTON = tex("teams_button");
    private static final ResourceLocation PLAYERS_BUTTON_FULL = tex("players_button_full");
    private static final ResourceLocation TEAMS_BUTTON_FULL = tex("teams_button_full");
    private static final ResourceLocation CLOSE_BUTTON = tex("close_button");
    private static final ResourceLocation CLOSE_BUTTON_HOVER = tex("close_button_hover");
    private static final ResourceLocation MAIL_BUTTON = tex("mail_button");
    private static final ResourceLocation MAIL_NOTIFICATION = tex("mail_button_notification");
    private static final ResourceLocation LEAVE_BUTTON = tex("leave_team_button");
    private static final ResourceLocation LEAVE_BUTTON_CLICK = tex("leave_team_button_click");
    private static final ResourceLocation DISBAND_BUTTON = tex("disband_team_button");
    private static final ResourceLocation DISBAND_BUTTON_CLICK = tex("disband_team_button_click");
    private static final ResourceLocation CREATE_BUTTON = tex("create_team_button");
    private static final ResourceLocation CREATE_BUTTON_CLICK = tex("create_team_button_click");
    private static final ResourceLocation CROWN = tex("crown");
    private static final ResourceLocation ADMIN_RANK = tex("admin_rank");
    private static final ResourceLocation MODERATOR_RANK = tex("moderator_rank");
    private static final ResourceLocation OTHER_ROW = tex("player_banner/other_player_banner");
    private static final ResourceLocation SELF_ROW = tex("player_banner/your_player_banner");
    private static final ResourceLocation SCROLL_PREV = tex("scrolling/previous_page_button");
    private static final ResourceLocation SCROLL_PREV_CLICK = tex("scrolling/previous_page_button_click");
    private static final ResourceLocation SCROLL_NEXT = tex("scrolling/next_page_button");
    private static final ResourceLocation SCROLL_NEXT_CLICK = tex("scrolling/next_page_button_click");
    private static final ResourceLocation SCROLL_SLIDER = tex("scrolling/slider");
    private static final ResourceLocation ONE_INVITE = tex("mail/one_invite");
    private static final ResourceLocation MULTIPLE_INVITE = tex("mail/multiple_invite");
    private static final ResourceLocation NO_MAIL = tex("mail/no_mail");
    private static final ResourceLocation MAIL_CARD = tex("mail/mail_button");
    private static final ResourceLocation ACCEPT_BUTTON = tex("mail/accept_button");
    private static final ResourceLocation DECLINE_BUTTON = tex("mail/decline_button");
    private static final ResourceLocation VIEW_ALL_MAIL_BUTTON = tex("mail/view_all_mail_button");

    private View view = View.SOCIAL;
    private SocialTab tab = SocialTab.PLAYERS;
    private boolean mailPopupOpen;
    private int scroll;
    private int messageScroll;
    private final List<Integer> pendingRefreshes = new ArrayList<>();
    private ContextMenu contextMenu;
    private boolean draggingMainScroll;
    private boolean draggingMailScroll;
    private int passiveRefreshTicks = PASSIVE_REFRESH_INTERVAL;

    public NoreTeamsScreen() {
        super(Component.literal("Nore Teams"));
    }

    @Override
    protected void init() {
        NoreTeamsClientState.clear();
        passiveRefreshTicks = PASSIVE_REFRESH_INTERVAL;
        requestRefresh();
    }

    @Override
    public void tick() {
        cacheOnlinePlayerSkins();
        leaveMailViewIfEmpty();
        if (--passiveRefreshTicks <= 0) {
            passiveRefreshTicks = PASSIVE_REFRESH_INTERVAL;
            requestRefresh();
        }
        if (!pendingRefreshes.isEmpty()) {
            List<Integer> remaining = new ArrayList<>();
            for (int refresh : pendingRefreshes) {
                int ticks = refresh - 1;
                if (ticks <= 0) {
                    requestRefresh();
                } else {
                    remaining.add(ticks);
                }
            }
            pendingRefreshes.clear();
            pendingRefreshes.addAll(remaining);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        leaveMailViewIfEmpty();
        renderTransparentBackground(graphics);
        Layout layout = layout();
        if (view == View.MAIL) {
            drawMailWindow(graphics, layout, mouseX, mouseY);
        } else {
            drawSocialWindow(graphics, layout, mouseX, mouseY);
        }
        if (contextMenu != null) {
            contextMenu.render(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = layout();
        if (contextMenu != null) {
            Optional<String> command = contextMenu.commandAt(mouseX, mouseY);
            contextMenu = null;
            if (command.isPresent()) {
                sendCommand(command.get());
                return true;
            }
            return true;
        }

        int closeX = u("close.x", CLOSE_X);
        int closeY = view == View.MAIL ? u("mail.close.y", MAIL_CLOSE_Y) : u("close.y", CLOSE_Y);
        if (button == 0 && insideUi(layout, mouseX, mouseY, closeX, closeY, 15, 15)) {
            onClose();
            return true;
        }

        if (view == View.MAIL) {
            return handleMailWindowClick(layout, mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && insideUi(layout, mouseX, mouseY, u("topMail.x", TOP_MAIL_X), u("topMail.y", TOP_MAIL_Y), 15, 15)) {
            mailPopupOpen = !mailPopupOpen;
            requestRefresh();
            return true;
        }
        if (button == 0 && tab == SocialTab.PLAYERS && insideUi(layout, mouseX, mouseY, u("teamsTab.x", TEAMS_TAB_X), u("tab.y", TAB_Y), 65, 15)) {
            tab = SocialTab.TEAM;
            scroll = 0;
            mailPopupOpen = false;
            requestRefresh();
            return true;
        }
        if (button == 0 && tab == SocialTab.TEAM && insideUi(layout, mouseX, mouseY, u("playersTab.x", PLAYERS_TAB_X), u("tab.y", TAB_Y), 65, 15)) {
            tab = SocialTab.PLAYERS;
            scroll = 0;
            mailPopupOpen = false;
            requestRefresh();
            return true;
        }
        if (mailPopupOpen && handleMailPopupClick(layout, mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && handleMainScrollClick(layout, mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && tab == SocialTab.TEAM && handleTeamButtonClick(layout, mouseX, mouseY)) {
            return true;
        }
        RowTarget target = rowAt(layout, mouseX, mouseY);
        if (target != null && button == 1) {
            contextMenu = ContextMenu.forRow((int) mouseX, (int) mouseY, target, NoreTeamsClientState.snapshot());
            return contextMenu != null;
        }
        mailPopupOpen = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingMainScroll = false;
        draggingMailScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingMainScroll) {
            updateMainScrollFromMouse(mouseY);
            return true;
        }
        if (button == 0 && draggingMailScroll) {
            updateMailScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        if (view == View.MAIL) {
            if (insideUi(layout, mouseX, mouseY, 0, 0, UI_SIZE, SOCIAL_HEIGHT)) {
                scrollMail((int) -Math.signum(scrollY));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (insideUi(layout, mouseX, mouseY, u("row.x", ROW_X), u("row.y", ROW_Y), ROW_WIDTH, visibleRows() * u("row.step", ROW_STEP) - 1)) {
            scroll = clamp(scroll - (int) Math.signum(scrollY), 0, maxMainScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && view == View.MAIL) {
            view = View.SOCIAL;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void drawSocialWindow(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        blit(graphics, tab == SocialTab.PLAYERS ? PLAYER_WINDOW : TEAM_WINDOW, layout.x, layout.y, UI_SIZE, UI_SIZE);
        if (tab == SocialTab.PLAYERS) {
            blit(graphics, TEAMS_BUTTON, layout.x + u("teamsTab.x", TEAMS_TAB_X), layout.y + u("tab.y", TAB_Y), 65, 15);
        } else {
            blit(graphics, PLAYERS_BUTTON, layout.x + u("playersTab.x", PLAYERS_TAB_X), layout.y + u("tab.y", TAB_Y), 65, 15);
            drawTeamActionButton(graphics, layout, mouseX, mouseY);
        }
        drawTopButtons(graphics, layout, mouseX, mouseY);
        drawRows(graphics, layout);
        drawMainScrollbar(graphics, layout, mouseX, mouseY);
        if (mailPopupOpen) {
            drawMailPopup(graphics, layout, mouseX, mouseY);
        }
    }

    private void drawMailWindow(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        blit(graphics, MAIL_WINDOW, layout.x, layout.y, UI_SIZE, UI_SIZE);
        blit(graphics, hoverUi(layout, mouseX, mouseY, u("close.x", CLOSE_X), u("mail.close.y", MAIL_CLOSE_Y), 15, 15) ? CLOSE_BUTTON_HOVER : CLOSE_BUTTON, layout.x + u("close.x", CLOSE_X), layout.y + u("mail.close.y", MAIL_CLOSE_Y), 15, 15);
        List<NoreTeamsNetwork.InviteEntry> invites = NoreTeamsClientState.snapshot().invites();
        int start = Math.min(messageScroll, Math.max(0, invites.size()));
        int max = Math.min(MAIL_VISIBLE_ROWS, invites.size() - start);
        blit(graphics, PLAYERS_BUTTON_FULL, layout.x + u("playersTab.x", PLAYERS_TAB_X), layout.y + u("mailTab.y", MAIL_TAB_Y), 66, 15);
        blit(graphics, TEAMS_BUTTON_FULL, layout.x + MAIL_TEAMS_TAB_X, layout.y + MAIL_TAB_Y, 66, 15);
        for (int i = 0; i < max; i++) {
            drawMailCard(graphics, layout, invites.get(start + i), u("mailCard.x", MAIL_CARD_X), u("mailCard.firstY", MAIL_CARD_FIRST_Y) + i * u("mailCard.step", MAIL_CARD_STEP), mouseX, mouseY);
        }
        drawMailScrollbar(graphics, layout, mouseX, mouseY);
    }

    private void drawTopButtons(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        boolean hasMail = !NoreTeamsClientState.snapshot().invites().isEmpty();
        blit(graphics, MAIL_BUTTON, layout.x + u("topMail.x", TOP_MAIL_X), layout.y + u("topMail.y", TOP_MAIL_Y), 15, 15);
        if (hasMail) {
            blit(graphics, MAIL_NOTIFICATION, layout.x + u("mailNotification.x", MAIL_NOTIFICATION_X), layout.y + u("mailNotification.y", MAIL_NOTIFICATION_Y), 9, 9);
        }
        blit(graphics, hoverUi(layout, mouseX, mouseY, u("close.x", CLOSE_X), u("close.y", CLOSE_Y), 15, 15) ? CLOSE_BUTTON_HOVER : CLOSE_BUTTON, layout.x + u("close.x", CLOSE_X), layout.y + u("close.y", CLOSE_Y), 15, 15);
    }

    private void drawTeamActionButton(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        NoreTeamsNetwork.SnapshotPayload snapshot = NoreTeamsClientState.snapshot();
        if (!snapshot.inTeam()) {
            drawCreateButton(graphics, layout, mouseX, mouseY);
            return;
        }
        ResourceLocation texture = snapshot.owner() ? DISBAND_BUTTON : LEAVE_BUTTON;
        ResourceLocation click = snapshot.owner() ? DISBAND_BUTTON_CLICK : LEAVE_BUTTON_CLICK;
        boolean pressed = leftMouseDown() && hoverUi(layout, mouseX, mouseY, u("teamButton.x", TEAM_BUTTON_X), u("teamButton.y", TEAM_BUTTON_Y), TEAM_BUTTON_WIDTH, TEAM_BUTTON_HEIGHT);
        blit(graphics, pressed ? click : texture, layout.x + u("teamButton.x", TEAM_BUTTON_X), layout.y + u("teamButton.y", TEAM_BUTTON_Y), TEAM_BUTTON_WIDTH, TEAM_BUTTON_HEIGHT);
    }

    private void drawCreateButton(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        boolean pressed = leftMouseDown() && hoverUi(layout, mouseX, mouseY, u("teamButton.x", TEAM_BUTTON_X), u("teamButton.y", TEAM_BUTTON_Y), TEAM_BUTTON_WIDTH, TEAM_BUTTON_HEIGHT);
        blit(graphics, pressed ? CREATE_BUTTON_CLICK : CREATE_BUTTON, layout.x + u("teamButton.x", TEAM_BUTTON_X), layout.y + u("teamButton.y", TEAM_BUTTON_Y), TEAM_BUTTON_WIDTH, TEAM_BUTTON_HEIGHT);
    }

    private void drawRows(GuiGraphics graphics, Layout layout) {
        List<RowTarget> rows = activeRows();
        int start = Math.min(scroll, Math.max(0, rows.size()));
        int max = visibleRows();
        for (int i = 0; i < max && start + i < rows.size(); i++) {
            drawRow(graphics, layout, rows.get(start + i), u("row.x", ROW_X), u("row.y", ROW_Y) + i * u("row.step", ROW_STEP));
        }
    }

    private void drawRow(GuiGraphics graphics, Layout layout, RowTarget row, int x, int y) {
        blit(graphics, row.relation == NoreTeamsNetwork.Relation.SELF ? SELF_ROW : OTHER_ROW, layout.x + x, layout.y + y, ROW_WIDTH, ROW_HEIGHT);
        int headX = layout.x + x + u("head.xOffset", HEAD_X_OFFSET);
        int headY = layout.y + y + u("head.yOffset", HEAD_Y_OFFSET);
        drawHead(graphics, row.id, headX, headY, HEAD_SIZE);
        String name = ellipsize(row.name, row.owner ? 136 : 154);
        drawShadowText(graphics, name, layout.x + x + u("name.xOffset", NAME_X_OFFSET), layout.y + y + u("name.yOffset", NAME_Y_OFFSET), row.textColor(), row.shadowColor());
        if (tab == SocialTab.TEAM && row.owner) {
            blit(graphics, CROWN, layout.x + x + CROWN_X_OFFSET, layout.y + y + CROWN_Y_OFFSET, 9, 6);
        } else if (tab == SocialTab.TEAM) {
            drawRankIcon(graphics, layout, row, x, y);
        }
    }

    private void drawRankIcon(GuiGraphics graphics, Layout layout, RowTarget row, int x, int y) {
        switch (row.normalizedRole()) {
            case "ADMIN" -> blit(graphics, ADMIN_RANK, layout.x + x + ADMIN_RANK_X_OFFSET, layout.y + y + ADMIN_RANK_Y_OFFSET, 7, 8);
            case "MODERATOR" -> blit(graphics, MODERATOR_RANK, layout.x + x + MODERATOR_RANK_X_OFFSET, layout.y + y + MODERATOR_RANK_Y_OFFSET, 7, 5);
            default -> {
            }
        }
    }

    private void drawMainScrollbar(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        String prefix = scrollPrefix();
        int scrollX = u(prefix + ".x", SCROLL_X);
        boolean prevPressed = leftMouseDown() && hoverUi(layout, mouseX, mouseY, scrollX, u(prefix + ".prevY", SCROLL_PREV_Y), 7, 7);
        boolean nextPressed = leftMouseDown() && hoverUi(layout, mouseX, mouseY, scrollX, u(prefix + ".nextY", SCROLL_NEXT_Y), 7, 7);
        blit(graphics, prevPressed ? SCROLL_PREV_CLICK : SCROLL_PREV, layout.x + scrollX, layout.y + u(prefix + ".prevY", SCROLL_PREV_Y), 7, 7);
        blit(graphics, nextPressed ? SCROLL_NEXT_CLICK : SCROLL_NEXT, layout.x + scrollX, layout.y + u(prefix + ".nextY", SCROLL_NEXT_Y), 7, 7);
        int y = sliderY(scroll, maxMainScroll(), u(prefix + ".thumbMinY", SCROLL_THUMB_MIN_Y), u(prefix + ".thumbMaxY", SCROLL_THUMB_MAX_Y));
        blit(graphics, SCROLL_SLIDER, layout.x + scrollX, layout.y + y, 7, 13);
    }

    private void drawMailScrollbar(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        int scrollX = u("mailScroll.x", MAIL_SCROLL_X);
        boolean prevPressed = leftMouseDown() && hoverUi(layout, mouseX, mouseY, scrollX, u("mailScroll.prevY", MAIL_SCROLL_PREV_Y), 7, 7);
        boolean nextPressed = leftMouseDown() && hoverUi(layout, mouseX, mouseY, scrollX, u("mailScroll.nextY", MAIL_SCROLL_NEXT_Y), 7, 7);
        blit(graphics, prevPressed ? SCROLL_PREV_CLICK : SCROLL_PREV, layout.x + scrollX, layout.y + u("mailScroll.prevY", MAIL_SCROLL_PREV_Y), 7, 7);
        blit(graphics, nextPressed ? SCROLL_NEXT_CLICK : SCROLL_NEXT, layout.x + scrollX, layout.y + u("mailScroll.nextY", MAIL_SCROLL_NEXT_Y), 7, 7);
        int y = sliderY(messageScroll, maxMailScroll(), u("mailScroll.thumbMinY", MAIL_SCROLL_THUMB_MIN_Y), u("mailScroll.thumbMaxY", MAIL_SCROLL_THUMB_MAX_Y));
        blit(graphics, SCROLL_SLIDER, layout.x + scrollX, layout.y + y, 7, 13);
    }

    private void drawMailPopup(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        List<NoreTeamsNetwork.InviteEntry> invites = NoreTeamsClientState.snapshot().invites();
        if (invites.isEmpty()) {
            blit(graphics, NO_MAIL, layout.x + u("popup.x", POPUP_X), layout.y + u("popup.y", POPUP_Y), 133, 27);
            return;
        }
        NoreTeamsNetwork.InviteEntry invite = invites.getFirst();
        boolean multiple = invites.size() > 1;
        int popupX = u("popup.x", POPUP_X);
        int popupY = u("popup.y", POPUP_Y);
        blit(graphics, multiple ? MULTIPLE_INVITE : ONE_INVITE, layout.x + popupX, layout.y + popupY, 133, multiple ? 73 : 57);
        int profileX = popupX + POPUP_PROFILE_X_OFFSET;
        int profileY = popupY + POPUP_PROFILE_Y_OFFSET;
        drawInviteHead(graphics, invite, layout.x + popupX + POPUP_FAKE_PROFILE_X_OFFSET, layout.y + popupY + POPUP_FAKE_PROFILE_Y_OFFSET);
        drawInviteNameAt(graphics, layout, invite, profileX + u("popupNameGap", PROFILE_NAME_GAP), profileY, 102);
        drawInviteButtons(graphics, layout, invite, popupX + u("popup.acceptX", POPUP_ACCEPT_X), popupY + u("popup.acceptY", POPUP_ACCEPT_Y), popupX + u("popup.declineX", POPUP_DECLINE_X), popupY + u("popup.declineY", POPUP_DECLINE_Y), mouseX, mouseY);
        if (multiple) {
            blit(graphics, VIEW_ALL_MAIL_BUTTON, layout.x + popupX + u("popup.viewAllX", POPUP_VIEW_ALL_X), layout.y + popupY + u("popup.viewAllY", POPUP_VIEW_ALL_Y), 119, 12);
        }
    }

    private void drawMailCard(GuiGraphics graphics, Layout layout, NoreTeamsNetwork.InviteEntry invite, int x, int y, int mouseX, int mouseY) {
        blit(graphics, MAIL_CARD, layout.x + x, layout.y + y, 123, 45);
        int profileX = x + MAIL_PROFILE_X_OFFSET;
        int profileY = y + MAIL_PROFILE_Y_OFFSET;
        drawInviteHead(graphics, invite, layout.x + x + MAIL_FAKE_PROFILE_X_OFFSET, layout.y + y + MAIL_FAKE_PROFILE_Y_OFFSET);
        drawInviteNameAt(graphics, layout, invite, profileX + u("popupNameGap", PROFILE_NAME_GAP), profileY, 92);
        drawInviteButtons(graphics, layout, invite, x + u("mailAccept.x", MAIL_ACCEPT_X), y + u("mailAccept.y", MAIL_ACCEPT_Y), x + u("mailDecline.x", MAIL_DECLINE_X), y + u("mailDecline.y", MAIL_DECLINE_Y), mouseX, mouseY);
    }

    private void drawInviteNameAt(GuiGraphics graphics, Layout layout, NoreTeamsNetwork.InviteEntry invite, int x, int y, int maxWidth) {
        NoreTeamsNetwork.Relation relation = relationFor(invite.ownerId());
        drawShadowText(graphics, ellipsize(invite.ownerName(), maxWidth), layout.x + x, layout.y + y, textColor(relation), shadowColor(relation));
    }

    private NoreTeamsNetwork.Relation relationFor(UUID playerId) {
        return NoreTeamsClientState.snapshot().players().stream()
                .filter(player -> player.id().equals(playerId))
                .findFirst()
                .map(NoreTeamsNetwork.PlayerEntry::relation)
                .orElse(NoreTeamsNetwork.Relation.OTHER);
    }

    private void drawInviteButtons(GuiGraphics graphics, Layout layout, NoreTeamsNetwork.InviteEntry invite, int acceptX, int acceptY, int declineX, int declineY, int mouseX, int mouseY) {
        blit(graphics, ACCEPT_BUTTON, layout.x + acceptX, layout.y + acceptY, 47, 14);
        blit(graphics, DECLINE_BUTTON, layout.x + declineX, layout.y + declineY, 47, 14);
    }

    private boolean handleTeamButtonClick(Layout layout, double mouseX, double mouseY) {
        if (!insideUi(layout, mouseX, mouseY, u("teamButton.x", TEAM_BUTTON_X), u("teamButton.y", TEAM_BUTTON_Y), TEAM_BUTTON_WIDTH, TEAM_BUTTON_HEIGHT)) {
            return false;
        }
        NoreTeamsNetwork.SnapshotPayload snapshot = NoreTeamsClientState.snapshot();
        if (!snapshot.inTeam()) {
            sendCommand("noreteams create");
        } else {
            sendCommand(snapshot.owner() ? "noreteams disband" : "noreteams leave");
        }
        return true;
    }

    private boolean handleMainScrollClick(Layout layout, double mouseX, double mouseY) {
        String prefix = scrollPrefix();
        int scrollX = u(prefix + ".x", SCROLL_X);
        if (insideUi(layout, mouseX, mouseY, scrollX, u(prefix + ".prevY", SCROLL_PREV_Y), 7, 7)) {
            scroll = clamp(scroll - visibleRows(), 0, maxMainScroll());
            return true;
        }
        if (insideUi(layout, mouseX, mouseY, scrollX, u(prefix + ".nextY", SCROLL_NEXT_Y), 7, 7)) {
            scroll = clamp(scroll + visibleRows(), 0, maxMainScroll());
            return true;
        }
        int thumbMin = u(prefix + ".thumbMinY", SCROLL_THUMB_MIN_Y);
        int thumbMax = u(prefix + ".thumbMaxY", SCROLL_THUMB_MAX_Y);
        if (insideUi(layout, mouseX, mouseY, scrollX, thumbMin, 7, thumbMax - thumbMin + 13)) {
            draggingMainScroll = true;
            updateMainScrollFromMouse(mouseY);
            return true;
        }
        return false;
    }

    private boolean handleMailWindowClick(Layout layout, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (insideUi(layout, mouseX, mouseY, u("playersTab.x", PLAYERS_TAB_X), u("mailTab.y", MAIL_TAB_Y), 66, 15)) {
            view = View.SOCIAL;
            tab = SocialTab.PLAYERS;
            scroll = 0;
            requestRefresh();
            return true;
        }
        if (insideUi(layout, mouseX, mouseY, MAIL_TEAMS_TAB_X, MAIL_TAB_Y, 66, 15)) {
            view = View.SOCIAL;
            tab = SocialTab.TEAM;
            scroll = 0;
            requestRefresh();
            return true;
        }
        int mailScrollX = u("mailScroll.x", MAIL_SCROLL_X);
        if (insideUi(layout, mouseX, mouseY, mailScrollX, u("mailScroll.prevY", MAIL_SCROLL_PREV_Y), 7, 7)) {
            scrollMail(-MAIL_VISIBLE_ROWS);
            return true;
        }
        if (insideUi(layout, mouseX, mouseY, mailScrollX, u("mailScroll.nextY", MAIL_SCROLL_NEXT_Y), 7, 7)) {
            scrollMail(MAIL_VISIBLE_ROWS);
            return true;
        }
        int mailThumbMin = u("mailScroll.thumbMinY", MAIL_SCROLL_THUMB_MIN_Y);
        int mailThumbMax = u("mailScroll.thumbMaxY", MAIL_SCROLL_THUMB_MAX_Y);
        if (insideUi(layout, mouseX, mouseY, mailScrollX, mailThumbMin, 7, mailThumbMax - mailThumbMin + 13)) {
            draggingMailScroll = true;
            updateMailScrollFromMouse(mouseY);
            return true;
        }
        List<NoreTeamsNetwork.InviteEntry> invites = NoreTeamsClientState.snapshot().invites();
        int start = Math.min(messageScroll, Math.max(0, invites.size()));
        for (int i = 0; i < MAIL_VISIBLE_ROWS && start + i < invites.size(); i++) {
            int y = u("mailCard.firstY", MAIL_CARD_FIRST_Y) + i * u("mailCard.step", MAIL_CARD_STEP);
            int x = u("mailCard.x", MAIL_CARD_X);
            if (handleInviteButtonClick(layout, invites.get(start + i), x + u("mailAccept.x", MAIL_ACCEPT_X), y + u("mailAccept.y", MAIL_ACCEPT_Y), x + u("mailDecline.x", MAIL_DECLINE_X), y + u("mailDecline.y", MAIL_DECLINE_Y), mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleMailPopupClick(Layout layout, double mouseX, double mouseY) {
        List<NoreTeamsNetwork.InviteEntry> invites = NoreTeamsClientState.snapshot().invites();
        if (invites.isEmpty()) {
            return insideUi(layout, mouseX, mouseY, u("popup.x", POPUP_X), u("popup.y", POPUP_Y), 133, 27);
        }
        NoreTeamsNetwork.InviteEntry invite = invites.getFirst();
        int popupX = u("popup.x", POPUP_X);
        int popupY = u("popup.y", POPUP_Y);
        if (handleInviteButtonClick(layout, invite, popupX + u("popup.acceptX", POPUP_ACCEPT_X), popupY + u("popup.acceptY", POPUP_ACCEPT_Y), popupX + u("popup.declineX", POPUP_DECLINE_X), popupY + u("popup.declineY", POPUP_DECLINE_Y), mouseX, mouseY)) {
            return true;
        }
        if (invites.size() > 1 && insideUi(layout, mouseX, mouseY, popupX + u("popup.viewAllX", POPUP_VIEW_ALL_X), popupY + u("popup.viewAllY", POPUP_VIEW_ALL_Y), 119, 12)) {
            view = View.MAIL;
            mailPopupOpen = false;
            messageScroll = 0;
            requestRefresh();
            return true;
        }
        return insideUi(layout, mouseX, mouseY, popupX, popupY, 133, invites.size() > 1 ? 73 : 57);
    }

    private boolean handleInviteButtonClick(Layout layout, NoreTeamsNetwork.InviteEntry invite, int acceptX, int acceptY, int declineX, int declineY, double mouseX, double mouseY) {
        if (insideUi(layout, mouseX, mouseY, acceptX, acceptY, 47, 14)) {
            sendCommand("noreteams join " + invite.partyId());
            return true;
        }
        if (insideUi(layout, mouseX, mouseY, declineX, declineY, 47, 14)) {
            sendCommand("noreteams decline " + invite.partyId());
            return true;
        }
        return false;
    }

    private void updateMainScrollFromMouse(double mouseY) {
        String prefix = scrollPrefix();
        scroll = scrollFromMouse(mouseY, layout().y + u(prefix + ".thumbMinY", SCROLL_THUMB_MIN_Y), layout().y + u(prefix + ".thumbMaxY", SCROLL_THUMB_MAX_Y), maxMainScroll());
    }

    private void updateMailScrollFromMouse(double mouseY) {
        messageScroll = scrollFromMouse(mouseY, layout().y + u("mailScroll.thumbMinY", MAIL_SCROLL_THUMB_MIN_Y), layout().y + u("mailScroll.thumbMaxY", MAIL_SCROLL_THUMB_MAX_Y), maxMailScroll());
    }

    private int scrollFromMouse(double mouseY, int minY, int maxY, int maxScroll) {
        if (maxScroll <= 0) {
            return 0;
        }
        double progress = (mouseY - minY) / Math.max(1.0, maxY - minY);
        return clamp((int) Math.round(progress * maxScroll), 0, maxScroll);
    }

    private void scrollMail(int amount) {
        messageScroll = clamp(messageScroll + amount, 0, maxMailScroll());
    }

    private RowTarget rowAt(Layout layout, double mouseX, double mouseY) {
        int max = visibleRows();
        if (!insideUi(layout, mouseX, mouseY, u("row.x", ROW_X), u("row.y", ROW_Y), ROW_WIDTH, max * u("row.step", ROW_STEP) - 1)) {
            return null;
        }
        int rowStep = u("row.step", ROW_STEP);
        int rowOffset = (int) mouseY - layout.y - u("row.y", ROW_Y);
        int row = rowOffset / rowStep + scroll;
        if (rowOffset % rowStep >= ROW_HEIGHT) {
            return null;
        }
        List<RowTarget> rows = activeRows();
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    private List<RowTarget> activeRows() {
        NoreTeamsNetwork.SnapshotPayload snapshot = NoreTeamsClientState.snapshot();
        if (tab == SocialTab.TEAM) {
            return snapshot.teamMembers().stream()
                    .map(member -> new RowTarget(member.id(), member.name(), relationForMember(member.id(), snapshot), member.role(), member.owner()))
                    .sorted(Comparator.comparing(RowTarget::sortName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        return snapshot.players().stream()
                .map(player -> {
                    Optional<NoreTeamsNetwork.MemberEntry> member = memberFor(player.id(), snapshot);
                    return new RowTarget(player.id(), player.name(), player.relation(), member.map(NoreTeamsNetwork.MemberEntry::role).orElse(""), member.map(NoreTeamsNetwork.MemberEntry::owner).orElse(false));
                })
                .toList();
    }

    private Optional<NoreTeamsNetwork.MemberEntry> memberFor(UUID id, NoreTeamsNetwork.SnapshotPayload snapshot) {
        return snapshot.teamMembers().stream().filter(member -> member.id().equals(id)).findFirst();
    }

    private NoreTeamsNetwork.Relation relationForMember(UUID id, NoreTeamsNetwork.SnapshotPayload snapshot) {
        return snapshot.players().stream()
                .filter(player -> player.id().equals(id))
                .map(NoreTeamsNetwork.PlayerEntry::relation)
                .findFirst()
                .orElse(NoreTeamsNetwork.Relation.TEAMMATE);
    }

    private int visibleRows() {
        return tab == SocialTab.PLAYERS ? PLAYER_VISIBLE_ROWS : TEAM_VISIBLE_ROWS;
    }

    private int maxMainScroll() {
        return Math.max(0, activeRows().size() - visibleRows());
    }

    private int maxMailScroll() {
        return Math.max(0, NoreTeamsClientState.snapshot().invites().size() - MAIL_VISIBLE_ROWS);
    }

    private String scrollPrefix() {
        return tab == SocialTab.TEAM ? "teamScroll" : "mainScroll";
    }

    private int sliderY(int value, int max, int minY, int maxY) {
        if (max <= 0) {
            return minY;
        }
        return minY + (maxY - minY) * value / max;
    }

    private void drawHead(GuiGraphics graphics, UUID playerId, int x, int y, int size) {
        Minecraft client = Minecraft.getInstance();
        PlayerInfo info = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(playerId);
        if (info != null) {
            rememberLiveSkin(info);
            rememberSkinProfile(info.getProfile());
            PlayerFaceRenderer.draw(graphics, info.getSkin().texture(), x, y, size);
            return;
        }
        ResourceLocation sessionSkin = SESSION_SKIN_CACHE.get(playerId);
        if (sessionSkin != null) {
            PlayerFaceRenderer.draw(graphics, sessionSkin, x, y, size);
            return;
        }
        loadSkinCache();
        GameProfile cached = SKIN_PROFILE_CACHE.get(playerId);
        if (cached != null) {
            PlayerFaceRenderer.draw(graphics, client.getSkinManager().getInsecureSkin(cached).texture(), x, y, size);
            return;
        }
        PlayerFaceRenderer.draw(graphics, DefaultPlayerSkin.getDefaultTexture(), x, y, size);
    }

    @SuppressWarnings("unused")
    private void drawFakeHead(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF6A4A2C);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFFC08A5A);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, 0xFF2B1C12);
        graphics.fill(x + size - 3, y + 2, x + size - 2, y + 3, 0xFF2B1C12);
        graphics.fill(x + 2, y + size - 3, x + size - 2, y + size - 2, 0xFF7A3F2A);
    }

    private static void rememberLiveSkin(PlayerInfo info) {
        if (info != null && info.getProfile().getId() != null) {
            SESSION_SKIN_CACHE.put(info.getProfile().getId(), info.getSkin().texture());
        }
    }

    private static void rememberSkinProfile(GameProfile profile) {
        if (profile == null || profile.getId() == null) {
            return;
        }
        Optional<Property> texture = textureProperty(profile);
        if (texture.isEmpty()) {
            return;
        }
        GameProfile existing = SKIN_PROFILE_CACHE.get(profile.getId());
        if (existing != null && sameTexture(texture.get(), textureProperty(existing).orElse(null))
                && safeName(profile).equals(safeName(existing))) {
            return;
        }
        GameProfile stored = new GameProfile(profile.getId(), safeName(profile));
        stored.getProperties().put(TEXTURES_PROPERTY, texture.get());
        SKIN_PROFILE_CACHE.put(profile.getId(), stored);
        saveSkinCache();
    }

    private static Optional<Property> textureProperty(GameProfile profile) {
        return profile.getProperties().get(TEXTURES_PROPERTY).stream().findFirst();
    }

    private static boolean sameTexture(Property a, Property b) {
        return b != null
                && a.value().equals(b.value())
                && ((a.signature() == null && b.signature() == null)
                || (a.signature() != null && a.signature().equals(b.signature())));
    }

    private void cacheOnlinePlayerSkins() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return;
        }
        client.getConnection().getOnlinePlayers().forEach(info -> {
            rememberLiveSkin(info);
            rememberSkinProfile(info.getProfile());
        });
    }

    private static void loadSkinCache() {
        if (skinCacheLoaded) {
            return;
        }
        skinCacheLoaded = true;
        Path path = skinCachePath();
        if (!Files.isRegularFile(path)) {
            return;
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException ex) {
            NoreTeams.LOGGER.warn("Could not load Nore Teams skin cache", ex);
            return;
        }
        properties.stringPropertyNames().stream()
                .filter(key -> key.endsWith(".value"))
                .forEach(key -> loadCachedProfile(properties, key.substring(0, key.length() - ".value".length())));
    }

    private static void loadCachedProfile(Properties properties, String idText) {
        try {
            UUID id = UUID.fromString(idText);
            String value = properties.getProperty(idText + ".value", "");
            if (value.isBlank()) {
                return;
            }
            String name = properties.getProperty(idText + ".name", idText);
            String signature = properties.getProperty(idText + ".signature", "");
            GameProfile profile = new GameProfile(id, name.isBlank() ? idText : name);
            profile.getProperties().put(TEXTURES_PROPERTY, signature.isBlank()
                    ? new Property(TEXTURES_PROPERTY, value)
                    : new Property(TEXTURES_PROPERTY, value, signature));
            SKIN_PROFILE_CACHE.put(id, profile);
        } catch (IllegalArgumentException ex) {
            NoreTeams.LOGGER.debug("Skipping invalid Nore Teams cached skin entry {}", idText);
        }
    }

    private static void saveSkinCache() {
        Path path = skinCachePath();
        Properties properties = new Properties();
        SKIN_PROFILE_CACHE.forEach((id, profile) -> textureProperty(profile).ifPresent(texture -> {
            String prefix = id.toString();
            properties.setProperty(prefix + ".name", safeName(profile));
            properties.setProperty(prefix + ".value", texture.value());
            if (texture.signature() != null) {
                properties.setProperty(prefix + ".signature", texture.signature());
            }
        }));
        try {
            Files.createDirectories(path.getParent());
            try (var output = Files.newOutputStream(path)) {
                properties.store(output, "Nore Teams cached player skin texture properties");
            }
        } catch (IOException ex) {
            NoreTeams.LOGGER.warn("Could not save Nore Teams skin cache", ex);
        }
    }

    private static Path skinCachePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(SKIN_CACHE_FILE);
    }

    private static String safeName(GameProfile profile) {
        return profile.getName() == null || profile.getName().isBlank() ? profile.getId().toString() : profile.getName();
    }

    private void drawInviteHead(GuiGraphics graphics, NoreTeamsNetwork.InviteEntry invite, int x, int y) {
        drawHead(graphics, invite.ownerId(), x, y, HEAD_SIZE);
    }

    private void drawShadowText(GuiGraphics graphics, String text, int x, int y, int color, int shadow) {
        graphics.drawString(font, text, x, y + 1, shadow, false);
        graphics.drawString(font, text, x, y, color, false);
    }

    private String ellipsize(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        int ellipsisWidth = font.width("...");
        if (width <= ellipsisWidth) {
            return "";
        }
        return font.plainSubstrByWidth(text, width - ellipsisWidth) + "...";
    }

    private void sendCommand(String command) {
        if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
            String[] parts = command.split(" ", 3);
            if (parts.length >= 2 && "noreteams".equals(parts[0])) {
                NoreTeamsNetwork.sendUiAction(parts[1], parts.length == 3 ? parts[2] : "");
            }
            mailPopupOpen = false;
            scheduleRefreshes();
        }
    }

    private void scheduleRefreshes() {
        pendingRefreshes.clear();
        pendingRefreshes.add(2);
        pendingRefreshes.add(8);
        pendingRefreshes.add(20);
    }

    private void requestRefresh() {
        if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
            passiveRefreshTicks = PASSIVE_REFRESH_INTERVAL;
            NoreTeamsNetwork.requestSnapshot();
        }
    }

    private void leaveMailViewIfEmpty() {
        if (view == View.MAIL && NoreTeamsClientState.snapshot().invites().isEmpty()) {
            view = View.SOCIAL;
            tab = SocialTab.PLAYERS;
            mailPopupOpen = false;
            messageScroll = 0;
            scroll = 0;
            draggingMailScroll = false;
            contextMenu = null;
        }
    }

    private int u(String key, int fallback) {
        return switch (key) {
            case "teamScroll.nextY" -> TEAM_SCROLL_NEXT_Y;
            case "teamScroll.thumbMaxY" -> TEAM_SCROLL_THUMB_MAX_Y;
            default -> fallback;
        };
    }

    private Layout layout() {
        int x = (width - UI_SIZE) / 2;
        int y = Math.max(8, (height - SOCIAL_HEIGHT) / 2);
        return new Layout(x, y);
    }

    private boolean hoverUi(Layout layout, double mouseX, double mouseY, int x, int y, int width, int height) {
        return insideUi(layout, mouseX, mouseY, x, y, width, height);
    }

    private boolean insideUi(Layout layout, double mouseX, double mouseY, int x, int y, int width, int height) {
        return inside(mouseX, mouseY, layout.x + x, layout.y + y, width, height);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean leftMouseDown() {
        return minecraft != null && GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(NoreTeams.MODID, "textures/gui/teams/" + path + ".png");
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    private enum View {
        SOCIAL,
        MAIL
    }

    private enum SocialTab {
        PLAYERS,
        TEAM
    }

    private record Layout(int x, int y) {
    }

    private record RowTarget(UUID id, String name, NoreTeamsNetwork.Relation relation, String role, boolean owner) {
        private String sortName() {
            return name;
        }

        private boolean isTeamMember() {
            return relation == NoreTeamsNetwork.Relation.TEAMMATE || relation == NoreTeamsNetwork.Relation.SELF;
        }

        private String normalizedRole() {
            return role == null ? "" : role.toUpperCase(Locale.ROOT);
        }

        private int textColor() {
            return NoreTeamsScreen.textColor(relation);
        }

        private int shadowColor() {
            return NoreTeamsScreen.shadowColor(relation);
        }
    }

    private static int textColor(NoreTeamsNetwork.Relation relation) {
        return switch (relation) {
            case SELF -> 0x65DF65;
            case TEAMMATE -> 0x56DDE5;
            case ALLY -> 0xE5A553;
            case OTHER -> 0xE9E9E9;
        };
    }

    private static int shadowColor(NoreTeamsNetwork.Relation relation) {
        return switch (relation) {
            case SELF -> 0x2E4639;
            case TEAMMATE -> 0x3C4F55;
            case ALLY -> 0x524B44;
            case OTHER -> 0x4F5257;
        };
    }

    private static final class ContextMenu {
        private final int x;
        private final int y;
        private final List<Action> actions;

        private ContextMenu(int x, int y, List<Action> actions) {
            this.x = x;
            this.y = y;
            this.actions = actions;
        }

        private static ContextMenu forRow(int x, int y, RowTarget row, NoreTeamsNetwork.SnapshotPayload snapshot) {
            List<Action> actions = new ArrayList<>();
            if (row.relation != NoreTeamsNetwork.Relation.SELF) {
                if (snapshot.inTeam() && snapshot.owner() && row.relation != NoreTeamsNetwork.Relation.TEAMMATE) {
                    actions.add(Action.invite(row.name));
                }
                if (row.relation == NoreTeamsNetwork.Relation.ALLY) {
                    actions.add(Action.removeAlly(row.id));
                } else if (!row.isTeamMember()) {
                    actions.add(Action.addAlly(row.id));
                }
                if (snapshot.owner() && row.isTeamMember() && !row.owner) {
                    actions.add(Action.kick(row.id));
                    String role = row.normalizedRole();
                    if (role.equals("MEMBER") || role.equals("MODERATOR")) {
                        actions.add(Action.promote(row.id));
                    }
                    if (role.equals("MODERATOR") || role.equals("ADMIN")) {
                        actions.add(Action.demote(row.id));
                    }
                    actions.add(Action.transfer(row.id));
                }
            }
            if (actions.isEmpty()) {
                return null;
            }
            return new ContextMenu(
                    x,
                    y - 16,
                    actions
            );
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY) {
            ResourceLocation window = switch (actions.size()) {
                case 1 -> ActionTextures.SINGLE_WINDOW;
                case 2 -> ActionTextures.DUO_WINDOW;
                case 3 -> ActionTextures.THREE_WINDOW;
                case 4 -> ActionTextures.FOUR_WINDOW;
                case 5 -> ActionTextures.FIVE_WINDOW;
                default -> ActionTextures.FIVE_WINDOW;
            };
            int height = switch (actions.size()) {
                case 1 -> 19;
                case 2 -> 33;
                case 3 -> 47;
                case 4 -> 61;
                default -> 75;
            };
            blit(graphics, window, x, y, 91, height);
            for (int i = 0; i < actions.size(); i++) {
                int buttonX = x + 6;
                int actionY = y + 3 + i * 14;
                Action action = actions.get(i);
                boolean pressed = inside(mouseX, mouseY, buttonX, actionY, 82, 13)
                        && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                blit(graphics, pressed ? action.clickTexture : action.texture, buttonX, actionY, 82, 13);
            }
        }

        private Optional<String> commandAt(double mouseX, double mouseY) {
            for (int i = 0; i < actions.size(); i++) {
                int buttonX = x + 6;
                int actionY = y + 3 + i * 14;
                if (inside(mouseX, mouseY, buttonX, actionY, 82, 13)) {
                    return Optional.of(actions.get(i).command);
                }
            }
            return Optional.empty();
        }
    }

    private record Action(ResourceLocation texture, ResourceLocation clickTexture, String command) {
        private static Action invite(String name) {
            return new Action(ActionTextures.INVITE, ActionTextures.INVITE_CLICK, "noreteams invite " + name);
        }

        private static Action addAlly(UUID id) {
            return new Action(ActionTextures.ADD_ALLY, ActionTextures.ADD_ALLY_CLICK, "noreteams ally " + id);
        }

        private static Action removeAlly(UUID id) {
            return new Action(ActionTextures.REMOVE_ALLY, ActionTextures.REMOVE_ALLY_CLICK, "noreteams unally " + id);
        }

        private static Action kick(UUID id) {
            return new Action(ActionTextures.KICK, ActionTextures.KICK_CLICK, "noreteams kick " + id);
        }

        private static Action promote(UUID id) {
            return new Action(ActionTextures.PROMOTE, ActionTextures.PROMOTE_CLICK, "noreteams promote " + id);
        }

        private static Action demote(UUID id) {
            return new Action(ActionTextures.DEMOTE, ActionTextures.DEMOTE_CLICK, "noreteams demote " + id);
        }

        private static Action transfer(UUID id) {
            return new Action(ActionTextures.TRANSFER, ActionTextures.TRANSFER_CLICK, "noreteams transfer " + id);
        }
    }

    private static final class ActionTextures {
        private static final ResourceLocation SINGLE_WINDOW = tex("user_action/single_action_window");
        private static final ResourceLocation DUO_WINDOW = tex("user_action/duo_action_window");
        private static final ResourceLocation THREE_WINDOW = tex("user_action/three_action_window");
        private static final ResourceLocation FOUR_WINDOW = tex("user_action/four_action_window");
        private static final ResourceLocation FIVE_WINDOW = tex("user_action/five_action_window");
        private static final ResourceLocation INVITE = tex("user_action/invite_player_button");
        private static final ResourceLocation INVITE_CLICK = tex("user_action/invite_player_button_click");
        private static final ResourceLocation ADD_ALLY = tex("user_action/add_ally_button");
        private static final ResourceLocation ADD_ALLY_CLICK = tex("user_action/add_ally_button_click");
        private static final ResourceLocation REMOVE_ALLY = tex("user_action/remove_ally_button");
        private static final ResourceLocation REMOVE_ALLY_CLICK = tex("user_action/remove_ally_button_click");
        private static final ResourceLocation KICK = tex("user_action/kick_player_button");
        private static final ResourceLocation KICK_CLICK = tex("user_action/kick_player_button_click");
        private static final ResourceLocation PROMOTE = tex("user_action/promote_button");
        private static final ResourceLocation PROMOTE_CLICK = tex("user_action/promote_button_click");
        private static final ResourceLocation DEMOTE = tex("user_action/demote_button");
        private static final ResourceLocation DEMOTE_CLICK = tex("user_action/demote_button_click");
        private static final ResourceLocation TRANSFER = tex("user_action/transfer_button");
        private static final ResourceLocation TRANSFER_CLICK = tex("user_action/transfer_button_click");
    }
}
