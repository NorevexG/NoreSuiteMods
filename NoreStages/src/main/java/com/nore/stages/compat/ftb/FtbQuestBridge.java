package com.nore.stages.compat.ftb;

import com.nore.stages.client.NoreStagesClientState;
import com.nore.stages.compat.norequest.NoreQuestCompat;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.team.StageSubjectResolver;
import com.nore.stages.service.StageService;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public final class FtbQuestBridge {
    private static final int MAX_RECENT_COMPLETIONS = 8;
    private static final Deque<String> RECENT_COMPLETIONS = new ArrayDeque<>();

    private FtbQuestBridge() {
    }

    public static String key(String type, String id) {
        return type + ":" + id;
    }

    public static void onQuestCompleted(String type, String id, Collection<ServerPlayer> onlineMembers) {
        if (onlineMembers.isEmpty()) {
            return;
        }
        String target = key(type, id);
        ServerPlayer first = onlineMembers.iterator().next();
        ResourceLocation stage = NoreStagesSavedData.get(first.level()).ftbQuestTrigger(target);
        if (stage == null) {
            stage = StageDataRegistry.ftbQuestTrigger(target);
        }
        if (stage == null) {
            return;
        }
        boolean share = shouldShareStageFromGates(first, stage, type, id);
        List<String> memberNames = onlineMembers.stream().map(player -> player.getGameProfile().getName()).toList();
        List<String> shareTargets = first.server.getPlayerList().getPlayers().stream()
                .filter(player -> StageSubjectResolver.localShareTargets(first).contains(player.getUUID()))
                .map(player -> player.getGameProfile().getName())
                .toList();
        recordCompletion(target + " -> " + stage + ", members=" + memberNames + ", share=" + share + ", targets=" + shareTargets);
        for (ServerPlayer player : onlineMembers) {
            StageService.grantConfigured(player, stage, share);
        }
    }

    public static boolean isVisible(String type, String id, Object teamData) {
        String target = key(type, id);
        if (FMLEnvironment.dist.isClient()) {
            return NoreStagesClientState.isFtbQuestVisible(target);
        }
        try {
            Method getOnlineMembers = teamData.getClass().getMethod("getOnlineMembers");
            Object members = getOnlineMembers.invoke(teamData);
            if (!(members instanceof Collection<?> collection) || collection.isEmpty()) {
                return true;
            }
            for (Object member : collection) {
                if (member instanceof ServerPlayer player) {
                    NoreStagesSavedData.QuestGate gate = NoreStagesSavedData.get(player.level()).ftbQuestGate(target);
                    if (gate == null) {
                        gate = StageDataRegistry.ftbQuestGate(target);
                    }
                    if (gate == null || hasGateStage(player, target, gate)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    public static List<String> recentCompletions() {
        return List.copyOf(RECENT_COMPLETIONS);
    }

    private static boolean shouldShareStageFromGates(ServerPlayer player, ResourceLocation stage, String type, String id) {
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        boolean sawMatchingGate = false;
        for (var entry : data.ftbQuestGates().entrySet()) {
            if (!entry.getValue().stage().equals(stage)) {
                continue;
            }
            sawMatchingGate = true;
            if (!NoreQuestCompat.isSoloQuest(player, entry.getKey())) {
                return true;
            }
        }
        for (var entry : StageDataRegistry.ftbQuestGates().entrySet()) {
            if (!entry.getValue().stage().equals(stage)) {
                continue;
            }
            sawMatchingGate = true;
            if (!NoreQuestCompat.isSoloQuest(player, entry.getKey())) {
                return true;
            }
        }
        return !sawMatchingGate && NoreQuestCompat.shouldShareUnlock(player, type, id);
    }

    private static boolean hasGateStage(ServerPlayer player, String target, NoreStagesSavedData.QuestGate gate) {
        if (gate.scope() == com.nore.stages.stage.StageScope.LOCAL && NoreQuestCompat.isSoloQuest(player, target)) {
            return StageService.hasDirectLocalStage(player, gate.stage());
        }
        return StageService.hasStage(player, gate.stage(), gate.scope());
    }

    private static void recordCompletion(String value) {
        RECENT_COMPLETIONS.addFirst(value);
        while (RECENT_COMPLETIONS.size() > MAX_RECENT_COMPLETIONS) {
            RECENT_COMPLETIONS.removeLast();
        }
    }
}
