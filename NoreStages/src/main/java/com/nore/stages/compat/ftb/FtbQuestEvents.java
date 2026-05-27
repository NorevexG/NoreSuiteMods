package com.nore.stages.compat.ftb;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import net.neoforged.fml.ModList;

public final class FtbQuestEvents {
    private static boolean registered;

    private FtbQuestEvents() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbquests")) {
            return;
        }
        registered = true;
        ObjectCompletedEvent.QUEST.register(event -> {
            FtbQuestBridge.onQuestCompleted("quest", event.getQuest().getCodeString(), event.getOnlineMembers());
            return EventResult.pass();
        });
        ObjectCompletedEvent.CHAPTER.register(event -> {
            FtbQuestBridge.onQuestCompleted("chapter", event.getChapter().getCodeString(), event.getOnlineMembers());
            return EventResult.pass();
        });
    }
}
