package com.nore.quest.api;

import com.nore.quest.ftb.QuestModeStore;
import com.nore.quest.ftb.QuestShareService;
import net.minecraft.server.level.ServerPlayer;

public final class NoreQuestApi {
    private NoreQuestApi() {
    }

    public static QuestCompletionMode getMode(ServerPlayer player, String questId) {
        return QuestModeStore.get(player.server, questId);
    }

    public static void setMode(ServerPlayer player, String questId, QuestCompletionMode mode) {
        QuestModeStore.set(player.server, questId, mode);
    }

    public static int shareCompletion(ServerPlayer actor, String questId) {
        return QuestShareService.shareQuestCommand(actor, questId, getMode(actor, questId));
    }

    public static void withActorContext(ServerPlayer actor, Runnable action) {
        QuestShareService.withActorContext(actor, action);
    }
}
