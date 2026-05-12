package com.nore.quest.ftb;

import dev.architectury.hooks.level.entity.PlayerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class FtbKillProgressBridge {
    private final ThreadLocal<Capture> capture = new ThreadLocal<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeLivingDeath(LivingDeathEvent event) {
        Capture snapshot = capture(event);
        if (snapshot == null || snapshot.progressBefore.isEmpty()) {
            ServerPlayer actor = actor(event.getSource());
            if (actor != null) {
                QuestShareService.diagnostic("Kill bridge BEFORE no capture actor={} killed={} snapshot={} tasks=0",
                        actor.getGameProfile().getName(), event.getEntity().getType(), snapshot != null);
            }
            capture.remove();
            return;
        }

        QuestShareService.diagnostic("Kill bridge BEFORE actor={} killed={} team={} killTasks={}",
                snapshot.actor.getGameProfile().getName(), event.getEntity().getType(),
                Integer.toHexString(System.identityHashCode(snapshot.teamData)), snapshot.progressBefore.size());
        snapshot.progressBefore.forEach((task, before) -> QuestShareService.diagnostic(
                "Kill bridge BEFORE task={} progress={}", QuestShareService.describeTask(task), before));
        capture.set(snapshot);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterLivingDeath(LivingDeathEvent event) {
        Capture snapshot = capture.get();
        capture.remove();
        if (snapshot == null || QuestShareService.isApplyingSharedProgress()) {
            ServerPlayer actor = actor(event.getSource());
            if (actor != null) {
                QuestShareService.diagnostic("Kill bridge AFTER skipped actor={} killed={} snapshot={} applying={}",
                        actor.getGameProfile().getName(), event.getEntity().getType(), snapshot != null, QuestShareService.isApplyingSharedProgress());
            }
            return;
        }

        QuestShareService.diagnostic("Kill bridge AFTER actor={} killed={} trackedTasks={}",
                snapshot.actor.getGameProfile().getName(), event.getEntity().getType(), snapshot.progressBefore.size());
        for (Map.Entry<Object, Long> entry : snapshot.progressBefore.entrySet()) {
            Object task = entry.getKey();
            long before = entry.getValue();
            long after = QuestShareService.progressFor(snapshot.teamData, task);
            long delta = after - before;
            QuestShareService.diagnostic("Kill bridge AFTER task={} before={} after={} delta={}",
                    QuestShareService.describeTask(task), before, after, delta);
            if (delta > 0L) {
                int shared = QuestShareService.shareProgressDelta(snapshot.actor, snapshot.teamData, task, delta);
                QuestShareService.diagnostic("Kill bridge SHARE task={} delta={} shared={}",
                        QuestShareService.describeTask(task), delta, shared);
            }
        }
    }

    private static Capture capture(LivingDeathEvent event) {
        ServerPlayer actor = actor(event.getSource());
        if (actor == null || PlayerHooks.isFake(actor)) {
            return null;
        }

        Object teamData = QuestShareService.teamDataFor(actor);
        if (teamData == null) {
            QuestShareService.diagnostic("Kill bridge capture failed: no FTB TeamData for actor={}", actor.getGameProfile().getName());
            return null;
        }

        Map<Object, Long> progressBefore = new IdentityHashMap<>();
        List<?> tasks = killTasks();
        QuestShareService.diagnostic("Kill bridge capture actor={} killed={} killTasks={}",
                actor.getGameProfile().getName(), event.getEntity().getType(), tasks.size());
        for (Object task : tasks) {
            progressBefore.put(task, QuestShareService.progressFor(teamData, task));
        }

        return new Capture(actor, teamData, progressBefore);
    }

    private static ServerPlayer actor(DamageSource source) {
        if (source == null) {
            return null;
        }

        Entity attacker = source.getEntity();
        return attacker instanceof ServerPlayer player ? player : null;
    }

    private static List<?> killTasks() {
        Object serverQuestFile = QuestShareService.serverQuestFileInstance();
        if (serverQuestFile == null) {
            QuestShareService.diagnostic("Kill bridge killTasks failed: ServerQuestFile.INSTANCE is null");
            return List.of();
        }

        try {
            Class<?> killTaskClass = Class.forName("dev.ftb.mods.ftbquests.quest.task.KillTask");
            List<?> tasks = Reflector.call(serverQuestFile, "collect", killTaskClass)
                    .filter(List.class::isInstance)
                    .map(List.class::cast)
                    .orElse(List.of());
            QuestShareService.diagnostic("Kill bridge killTasks collected={}", tasks.size());
            return tasks;
        } catch (ClassNotFoundException ex) {
            QuestShareService.diagnostic("Kill bridge killTasks failed: KillTask class not found");
            return List.of();
        }
    }

    private record Capture(ServerPlayer actor, Object teamData, Map<Object, Long> progressBefore) {
    }
}
