package com.nore.quest.ftb;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.util.Map;

public final class FtbTeamsCommandBlocker {
    private static final String[] COMMANDS = {"ftbteams", "ftbteam", "teams", "ftbteams_add_fake_player"};

    private FtbTeamsCommandBlocker() {
    }

    public static void block(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String command : COMMANDS) {
            remove(dispatcher.getRoot(), command);
        }
    }

    @SuppressWarnings("unchecked")
    private static void remove(CommandNode<CommandSourceStack> root, String child) {
        for (String fieldName : new String[]{"children", "literals", "arguments"}) {
            try {
                Field field = CommandNode.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(root);
                if (value instanceof Map<?, ?> map) {
                    ((Map<String, CommandNode<CommandSourceStack>>) map).remove(child);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
