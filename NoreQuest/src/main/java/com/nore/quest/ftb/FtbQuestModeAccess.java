package com.nore.quest.ftb;

import com.nore.quest.api.QuestCompletionMode;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.WeakHashMap;

public final class FtbQuestModeAccess {
    private static final String NBT_KEY = "norequest:completion_mode";
    private static final Map<Object, QuestCompletionMode> MODES = new WeakHashMap<>();

    private FtbQuestModeAccess() {
    }

    public static QuestCompletionMode get(Object quest) {
        return MODES.getOrDefault(quest, QuestCompletionMode.SOLO);
    }

    public static boolean contains(Object quest) {
        return MODES.containsKey(quest);
    }

    public static void set(Object quest, QuestCompletionMode mode) {
        MODES.put(quest, mode);
    }

    public static void read(Object quest, CompoundTag tag) {
        if (tag.contains(NBT_KEY)) {
            set(quest, QuestCompletionMode.parse(tag.getString(NBT_KEY)));
        } else {
            MODES.remove(quest);
        }
    }

    public static void write(Object quest, CompoundTag tag) {
        tag.putString(NBT_KEY, get(quest).name().toLowerCase());
    }
}
