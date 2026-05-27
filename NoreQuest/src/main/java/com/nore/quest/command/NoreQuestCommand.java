package com.nore.quest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.nore.quest.api.QuestCompletionMode;
import com.nore.quest.ftb.QuestModeStore;
import com.nore.quest.ftb.QuestShareService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class NoreQuestCommand {
    private NoreQuestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("norequest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mode")
                        .then(Commands.literal("get")
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .executes(ctx -> getMode(ctx.getSource(), StringArgumentType.getString(ctx, "quest")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    for (QuestCompletionMode mode : QuestCompletionMode.values()) {
                                                        builder.suggest(mode.name().toLowerCase());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> setMode(ctx.getSource(), StringArgumentType.getString(ctx, "quest"), StringArgumentType.getString(ctx, "mode")))))))
                .then(Commands.literal("share")
                        .then(Commands.argument("quest", StringArgumentType.string())
                                .executes(ctx -> share(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "quest")))))
                .executes(ctx -> help(ctx.getSource())));
    }

    private static int getMode(CommandSourceStack source, String questId) {
        QuestCompletionMode mode = QuestModeStore.get(source.getServer(), questId);
        source.sendSuccess(() -> Component.literal("Quest " + questId + " mode: " + mode.name().toLowerCase()), false);
        return 1;
    }

    private static int setMode(CommandSourceStack source, String questId, String modeName) {
        QuestCompletionMode mode = QuestCompletionMode.parse(modeName);
        QuestShareService.setQuestMode(source.getServer(), questId, mode);
        source.sendSuccess(() -> Component.literal("Quest " + questId + " mode set to " + mode.name().toLowerCase()), true);
        return 1;
    }

    private static int share(ServerPlayer actor, String questId) {
        QuestCompletionMode mode = QuestModeStore.get(actor.server, questId);
        int count = QuestShareService.shareQuestCommand(actor, questId, mode);
        actor.sendSystemMessage(Component.literal("Shared " + questId + " completion to " + count + " player(s)."));
        return count;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Nore Quest: /norequest mode get <quest>, /norequest mode set <quest> <solo|team|allied>, /norequest share <quest>"), false);
        return 1;
    }
}
