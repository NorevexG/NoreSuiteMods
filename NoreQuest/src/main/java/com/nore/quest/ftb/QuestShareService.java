package com.nore.quest.ftb;

import com.nore.quest.NoreQuest;
import com.nore.quest.api.QuestCompletionMode;
import com.nore.teams.api.NoreTeamsApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuestShareService {
    private static final ThreadLocal<Boolean> APPLYING_SHARED_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Map<Object, Map<Object, Long>>> BEFORE_PROGRESS = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Map<Object, Map<Object, Boolean>>> BEFORE_COMPLETED = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Deque<ServerPlayer>> ACTOR_CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile boolean diagnosticsEnabled = false;

    private QuestShareService() {
    }

    public static void beforeFtbProgress(Object teamData, Object task) {
        if (APPLYING_SHARED_PROGRESS.get()) {
            return;
        }
        long before = progress(teamData, task);
        BEFORE_PROGRESS.get()
                .computeIfAbsent(teamData, ignored -> new IdentityHashMap<>())
                .put(task, before);
        diagnostic("TeamData.setProgress HEAD team={} task={} before={} actorContext={}",
                identity(teamData), describeTask(task), before, playerName(currentFtbPlayer()));
    }

    public static void shareFromFtbProgress(Object sourceTeamData, Object task) {
        Long previousProgress = removePreviousProgress(sourceTeamData, task);
        if (previousProgress == null || APPLYING_SHARED_PROGRESS.get()) {
            return;
        }

        long currentProgress = progress(sourceTeamData, task);
        long delta = currentProgress - previousProgress;
        diagnostic("TeamData.setProgress TAIL team={} task={} before={} after={} delta={} actorContext={}",
                identity(sourceTeamData), describeTask(task), previousProgress, currentProgress, delta, playerName(currentFtbPlayer()));
        if (delta <= 0L) {
            return;
        }

        ServerPlayer actor = currentFtbPlayer();
        shareProgressDelta(actor, sourceTeamData, task, delta);
    }

    public static int shareFromFtbCompletion(Object sourceTeamData, Object task) {
        return shareFromFtbCompletion(currentFtbPlayer(), sourceTeamData, task, false);
    }

    public static void beforeFtbSubmitCompletion(Object teamData, Object task) {
        if (APPLYING_SHARED_PROGRESS.get()) {
            return;
        }
        boolean completed = isCompleted(teamData, task);
        BEFORE_COMPLETED.get()
                .computeIfAbsent(teamData, ignored -> new IdentityHashMap<>())
                .put(task, completed);
        diagnostic("submitTask HEAD team={} task={} completed={} actorContext={}",
                identity(teamData), describeTask(task), completed, playerName(currentFtbPlayer()));
    }

    public static int shareFromFtbSubmitCompletion(ServerPlayer actor, Object sourceTeamData, Object task) {
        Boolean wasCompleted = removePreviousCompleted(sourceTeamData, task);
        if (wasCompleted == null || wasCompleted || APPLYING_SHARED_PROGRESS.get()) {
            diagnostic("submitTask RETURN rejected team={} task={} beforeCompleted={} nowCompleted={} actor={} applying={}",
                    identity(sourceTeamData), describeTask(task), wasCompleted, isCompleted(sourceTeamData, task),
                    playerName(actor), APPLYING_SHARED_PROGRESS.get());
            return 0;
        }

        if (!isCompleted(sourceTeamData, task)) {
            diagnostic("submitTask RETURN no completion team={} task={} actor={}",
                    identity(sourceTeamData), describeTask(task), playerName(actor));
            return 0;
        }

        return shareFromFtbCompletion(actor, sourceTeamData, task, true);
    }

    private static int shareFromFtbCompletion(ServerPlayer actor, Object sourceTeamData, Object task, boolean requireSourceCompleted) {
        if (sourceTeamData == null || task == null || APPLYING_SHARED_PROGRESS.get()) {
            diagnostic("shareFromFtbCompletion rejected early team={} task={} applying={}",
                    identity(sourceTeamData), describeTask(task), APPLYING_SHARED_PROGRESS.get());
            return 0;
        }

        Object quest = Reflector.call(task, "getQuest").orElse(null);
        Object actorTeamData = actor == null ? null : teamData(actor);
        if (actor == null || quest == null || actorTeamData != sourceTeamData) {
            diagnostic("shareFromFtbCompletion rejected actor={} quest={} actorTeam={} sourceTeam={} task={}",
                    playerName(actor), describeQuest(quest), identity(actorTeamData), identity(sourceTeamData), describeTask(task));
            return 0;
        }

        if (requireSourceCompleted && !isCompleted(sourceTeamData, task)) {
            diagnostic("shareFromFtbCompletion rejected source incomplete actor={} quest={} task={}",
                    playerName(actor), describeQuest(quest), describeTask(task));
            return 0;
        }

        QuestCompletionMode mode = modeFor(actor.server, quest);
        if (mode == QuestCompletionMode.SOLO) {
            diagnostic("shareFromFtbCompletion solo actor={} quest={} task={}",
                    playerName(actor), describeQuest(quest), describeTask(task));
            return 0;
        }

        long completionProgress = Math.max(maxProgress(task), progress(sourceTeamData, task));
        if (completionProgress <= 0L) {
            diagnostic("shareFromFtbCompletion rejected no completion progress actor={} quest={} task={} progress={}",
                    playerName(actor), describeQuest(quest), describeTask(task), completionProgress);
            return 0;
        }

        int shared = 0;
        Set<UUID> targets = NoreTeamsApi.getRelatedPlayers(actor.server, actor.getUUID(), mode.includeAllies());
        diagnostic("shareFromFtbCompletion begin actor={} quest={} task={} mode={} completionProgress={} relatedTargets={}",
                playerName(actor), describeQuest(quest), describeTask(task), mode, completionProgress, targets.size());
        APPLYING_SHARED_PROGRESS.set(true);
        try {
            for (UUID targetId : targets) {
                if (targetId.equals(actor.getUUID())) {
                    diagnostic("shareFromFtbCompletion skip self target={}", targetId);
                    continue;
                }

                ServerPlayer target = actor.server.getPlayerList().getPlayer(targetId);
                if (target == null) {
                    diagnostic("shareFromFtbCompletion skip offline target={}", targetId);
                    continue;
                }

                Object targetTeamData = teamData(target);
                if (targetTeamData == null || targetTeamData == sourceTeamData || !canAcceptProgress(targetTeamData, task, quest)) {
                    diagnostic("shareFromFtbCompletion skip target={} targetTeam={} sameTeamData={} canAccept={}",
                            playerName(target), identity(targetTeamData), targetTeamData == sourceTeamData,
                            targetTeamData != null && canAcceptProgress(targetTeamData, task, quest));
                    continue;
                }

                if (isCompleted(targetTeamData, task)) {
                    diagnostic("shareFromFtbCompletion skip completed target={} targetTeam={}",
                            playerName(target), identity(targetTeamData));
                    continue;
                }

                long before = progress(targetTeamData, task);
                setProgressWithPlayerContext(target, targetTeamData, task, completionProgress);
                long after = progress(targetTeamData, task);
                shared++;
                diagnostic("shareFromFtbCompletion applied target={} targetTeam={} before={} after={} completionProgress={}",
                        playerName(target), identity(targetTeamData), before, after, completionProgress);
            }
        } finally {
            APPLYING_SHARED_PROGRESS.set(false);
        }

        diagnostic("shareFromFtbCompletion end actor={} quest={} task={} shared={}",
                playerName(actor), describeQuest(quest), describeTask(task), shared);
        return shared;
    }

    public static int shareProgressDelta(ServerPlayer actor, Object sourceTeamData, Object task, long delta) {
        if (actor == null || sourceTeamData == null || task == null || delta <= 0L || APPLYING_SHARED_PROGRESS.get()) {
            diagnostic("shareProgressDelta rejected early actor={} team={} task={} delta={} applying={}",
                    playerName(actor), identity(sourceTeamData), describeTask(task), delta, APPLYING_SHARED_PROGRESS.get());
            return 0;
        }

        Object quest = Reflector.call(task, "getQuest").orElse(null);
        Object actorTeamData = teamData(actor);
        if (quest == null || actorTeamData != sourceTeamData) {
            diagnostic("shareProgressDelta rejected actor={} quest={} actorTeam={} sourceTeam={} task={} delta={}",
                    playerName(actor), describeQuest(quest), identity(actorTeamData), identity(sourceTeamData), describeTask(task), delta);
            return 0;
        }

        QuestCompletionMode mode = modeFor(actor.server, quest);
        if (mode == QuestCompletionMode.SOLO) {
            diagnostic("shareProgressDelta solo actor={} quest={} task={} delta={}",
                    playerName(actor), describeQuest(quest), describeTask(task), delta);
            return 0;
        }

        int shared = 0;
        Set<UUID> targets = NoreTeamsApi.getRelatedPlayers(actor.server, actor.getUUID(), mode.includeAllies());
        diagnostic("shareProgressDelta begin actor={} quest={} task={} mode={} delta={} relatedTargets={}",
                playerName(actor), describeQuest(quest), describeTask(task), mode, delta, targets.size());
        APPLYING_SHARED_PROGRESS.set(true);
        try {
            for (UUID targetId : targets) {
                if (targetId.equals(actor.getUUID())) {
                    diagnostic("shareProgressDelta skip self target={}", targetId);
                    continue;
                }

                ServerPlayer target = actor.server.getPlayerList().getPlayer(targetId);
                if (target == null) {
                    diagnostic("shareProgressDelta skip offline target={}", targetId);
                    continue;
                }

                Object targetTeamData = teamData(target);
                if (targetTeamData == null || targetTeamData == sourceTeamData || !canAcceptProgress(targetTeamData, task, quest)) {
                    diagnostic("shareProgressDelta skip target={} targetTeam={} sameTeamData={} canAccept={}",
                            playerName(target), identity(targetTeamData), targetTeamData == sourceTeamData,
                            targetTeamData != null && canAcceptProgress(targetTeamData, task, quest));
                    continue;
                }

                long before = progress(targetTeamData, task);
                addProgressWithPlayerContext(target, targetTeamData, task, delta);
                long after = progress(targetTeamData, task);
                shared++;
                diagnostic("shareProgressDelta applied target={} targetTeam={} before={} after={} delta={}",
                        playerName(target), identity(targetTeamData), before, after, delta);
            }
        } finally {
            APPLYING_SHARED_PROGRESS.set(false);
        }

        diagnostic("shareProgressDelta end actor={} quest={} task={} shared={}",
                playerName(actor), describeQuest(quest), describeTask(task), shared);
        return shared;
    }

    public static Object teamDataFor(ServerPlayer player) {
        return teamData(player);
    }

    public static long progressFor(Object teamData, Object task) {
        return progress(teamData, task);
    }

    public static Object serverQuestFileInstance() {
        return serverQuestFile();
    }

    public static boolean isApplyingSharedProgress() {
        return APPLYING_SHARED_PROGRESS.get();
    }

    public static void setDiagnosticsEnabled(boolean enabled) {
        diagnosticsEnabled = enabled;
    }

    public static void setLoadedQuestMode(MinecraftServer server, String questId, QuestCompletionMode mode) {
        Object serverQuestFile = serverQuestFile();
        if (serverQuestFile == null) {
            return;
        }

        try {
            long id = Long.parseUnsignedLong(questId, 16);
            Object quest = Reflector.call(serverQuestFile, "get", id).orElse(null);
            if (quest != null && "dev.ftb.mods.ftbquests.quest.Quest".equals(quest.getClass().getName())) {
                FtbQuestModeAccess.set(quest, mode);
            }
        } catch (NumberFormatException ignored) {
            NoreQuest.LOGGER.debug("Could not parse FTB quest id {} while setting Nore Quest mode", questId);
        }
    }

    public static QuestCompletionMode modeForQuestId(MinecraftServer server, String questId) {
        Object quest = loadedQuest(server, questId);
        if (quest != null) {
            return modeFor(server, quest);
        }
        return QuestModeStore.get(server, questId);
    }

    public static void setQuestMode(MinecraftServer server, String questId, QuestCompletionMode mode) {
        QuestModeStore.set(server, questId, mode);
        setLoadedQuestMode(server, questId, mode);
    }

    public static boolean diagnosticsEnabled() {
        return diagnosticsEnabled;
    }

    public static void diagnostic(String message, Object... args) {
        if (diagnosticsEnabled) {
            NoreQuest.LOGGER.info("[NoreQuestDiag] " + message, args);
        }
    }

    public static String describeTask(Object task) {
        if (task == null) {
            return "null";
        }

        Object quest = Reflector.call(task, "getQuest").orElse(null);
        String taskId = Reflector.call(task, "getCodeString")
                .map(Object::toString)
                .or(() -> Reflector.call(task, "getId").map(Object::toString))
                .orElse(identity(task));
        return task.getClass().getName() + "#" + taskId + " quest=" + describeQuest(quest);
    }

    public static String describeQuest(Object quest) {
        if (quest == null) {
            return "null";
        }

        return Reflector.call(quest, "getCodeString")
                .map(Object::toString)
                .or(() -> Reflector.call(quest, "getId").map(Object::toString))
                .orElse(identity(quest));
    }

    public static void beginActorContext(ServerPlayer actor) {
        if (actor == null) {
            return;
        }
        ACTOR_CONTEXT.get().push(actor);
        diagnostic("Actor context begin {}", playerName(actor));
    }

    public static void endActorContext(ServerPlayer actor) {
        if (actor == null) {
            return;
        }
        Deque<ServerPlayer> actors = ACTOR_CONTEXT.get();
        if (!actors.isEmpty()) {
            if (actors.peek() == actor) {
                actors.pop();
            } else {
                actors.remove(actor);
            }
        }
        if (actors.isEmpty()) {
            ACTOR_CONTEXT.remove();
        }
        diagnostic("Actor context end {}", playerName(actor));
    }

    public static void withActorContext(ServerPlayer actor, Runnable action) {
        if (actor == null) {
            action.run();
            return;
        }
        beginActorContext(actor);
        try {
            action.run();
        } finally {
            endActorContext(actor);
        }
    }

    public static int shareQuestCommand(ServerPlayer actor, String questId, QuestCompletionMode mode) {
        int count = 0;
        if (mode == QuestCompletionMode.SOLO) {
            return runFtbComplete(actor.server, actor, questId) ? 1 : 0;
        }
        for (UUID targetId : NoreTeamsApi.getRelatedPlayers(actor.server, actor.getUUID(), mode.includeAllies())) {
            ServerPlayer online = actor.server.getPlayerList().getPlayer(targetId);
            if (online != null && runFtbComplete(actor.server, online, questId)) {
                count++;
            }
        }
        return count;
    }

    static boolean runFtbComplete(MinecraftServer server, ServerPlayer player, String questId) {
        try {
            String command = "ftbquests change_progress " + player.getGameProfile().getName() + " complete " + questId;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
            return true;
        } catch (RuntimeException ex) {
            NoreQuest.LOGGER.warn("Could not run FTB Quests completion command for {}", questId, ex);
            return false;
        }
    }

    private static Long removePreviousProgress(Object teamData, Object task) {
        Map<Object, Map<Object, Long>> teamEntries = BEFORE_PROGRESS.get();
        Map<Object, Long> taskEntries = teamEntries.get(teamData);
        if (taskEntries == null) {
            return null;
        }
        Long previousProgress = taskEntries.remove(task);
        if (taskEntries.isEmpty()) {
            teamEntries.remove(teamData);
        }
        return previousProgress;
    }

    private static Boolean removePreviousCompleted(Object teamData, Object task) {
        Map<Object, Map<Object, Boolean>> teamEntries = BEFORE_COMPLETED.get();
        Map<Object, Boolean> taskEntries = teamEntries.get(teamData);
        if (taskEntries == null) {
            return null;
        }
        Boolean previousCompleted = taskEntries.remove(task);
        if (taskEntries.isEmpty()) {
            teamEntries.remove(teamData);
        }
        return previousCompleted;
    }

    private static QuestCompletionMode modeFor(MinecraftServer server, Object quest) {
        String questId = questId(quest);
        if (FtbQuestModeAccess.contains(quest)) {
            return FtbQuestModeAccess.get(quest);
        }
        if (QuestModeStore.contains(server, questId)) {
            return QuestModeStore.get(server, questId);
        }
        return FtbQuestModeAccess.get(quest);
    }

    private static Object loadedQuest(MinecraftServer server, String questId) {
        Object serverQuestFile = serverQuestFile();
        if (serverQuestFile == null) {
            return null;
        }
        try {
            long id = Long.parseUnsignedLong(questId, 16);
            Object quest = Reflector.call(serverQuestFile, "get", id).orElse(null);
            if (quest != null && "dev.ftb.mods.ftbquests.quest.Quest".equals(quest.getClass().getName())) {
                return quest;
            }
        } catch (NumberFormatException ignored) {
            NoreQuest.LOGGER.debug("Could not parse FTB quest id {} while reading Nore Quest mode", questId);
        }
        return null;
    }

    private static boolean canAcceptProgress(Object teamData, Object task, Object quest) {
        boolean canStartQuest = Reflector.call(teamData, "canStartTasks", quest)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
        if (!canStartQuest) {
            return false;
        }
        return Reflector.call(task, "checkTaskSequence", teamData)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(true);
    }

    private static void addProgressWithPlayerContext(ServerPlayer player, Object teamData, Object task, long delta) {
        Runnable apply = () -> Reflector.invoke(teamData, "addProgress", task, delta);
        Object serverQuestFile = serverQuestFile();
        if (serverQuestFile == null || !Reflector.invoke(serverQuestFile, "withPlayerContext", player, apply)) {
            apply.run();
        }
    }

    private static void setProgressWithPlayerContext(ServerPlayer player, Object teamData, Object task, long progress) {
        Runnable apply = () -> Reflector.invoke(teamData, "setProgress", task, progress);
        Object serverQuestFile = serverQuestFile();
        if (serverQuestFile == null || !Reflector.invoke(serverQuestFile, "withPlayerContext", player, apply)) {
            apply.run();
        }
    }

    private static Object teamData(ServerPlayer player) {
        try {
            Class<?> teamDataClass = Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
            Method get = teamDataClass.getMethod("get", Player.class);
            return get.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            NoreQuest.LOGGER.debug("Could not resolve FTB quest TeamData for {}", player.getGameProfile().getName(), ex);
            return null;
        }
    }

    private static long progress(Object teamData, Object task) {
        return Reflector.call(teamData, "getProgress", task)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(0L);
    }

    private static long maxProgress(Object task) {
        return Reflector.call(task, "getMaxProgress")
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(0L);
    }

    private static boolean isCompleted(Object teamData, Object task) {
        return Reflector.call(teamData, "isCompleted", task)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    private static ServerPlayer currentFtbPlayer() {
        Object serverQuestFile = serverQuestFile();
        if (serverQuestFile != null) {
            Object player = Reflector.call(serverQuestFile, "getCurrentPlayer").orElse(null);
            if (player instanceof ServerPlayer serverPlayer) {
                return serverPlayer;
            }
        }
        Deque<ServerPlayer> actors = ACTOR_CONTEXT.get();
        return actors.isEmpty() ? null : actors.peek();
    }

    private static Object serverQuestFile() {
        try {
            Class<?> fileClass = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
            Field instance = fileClass.getField("INSTANCE");
            return instance.get(null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static String questId(Object quest) {
        return Reflector.call(quest, "getCodeString")
                .map(Object::toString)
                .or(() -> Reflector.call(quest, "getId").map(Object::toString))
                .orElse(quest.toString());
    }

    private static String playerName(ServerPlayer player) {
        return player == null ? "null" : player.getGameProfile().getName() + "/" + player.getUUID();
    }

    private static String identity(Object value) {
        return value == null ? "null" : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
