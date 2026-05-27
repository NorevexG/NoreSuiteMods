package com.nore.stages.world;

import com.nore.stages.NoreStages;
import com.nore.stages.stage.StageScope;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NoreStagesSavedData extends SavedData {
    private static final String DATA_NAME = NoreStages.MODID + "_progress";
    private static final Factory<NoreStagesSavedData> FACTORY = new Factory<>(NoreStagesSavedData::new, NoreStagesSavedData::load);

    private final Set<ResourceLocation> globalStages = new LinkedHashSet<>();
    private final Set<String> globalFacts = new LinkedHashSet<>();
    private final Map<UUID, Set<ResourceLocation>> localStages = new LinkedHashMap<>();
    private final Map<UUID, Set<ResourceLocation>> directLocalStages = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> localFacts = new LinkedHashMap<>();
    private final Map<UUID, Set<ResourceLocation>> appliedGlobalStages = new LinkedHashMap<>();
    private final Map<UUID, Set<ResourceLocation>> appliedLocalStages = new LinkedHashMap<>();
    private final Set<ResourceLocation> globalQuestUnlocks = new LinkedHashSet<>();
    private final Map<UUID, Set<ResourceLocation>> localQuestUnlocks = new LinkedHashMap<>();
    private ResourceLocation globalRank;
    private final Map<UUID, ResourceLocation> localRanks = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> ftbQuestTriggers = new LinkedHashMap<>();
    private final Map<String, QuestGate> ftbQuestGates = new LinkedHashMap<>();

    public static NoreStagesSavedData get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean hasGlobalStage(ResourceLocation stage) {
        return globalStages.contains(stage);
    }

    public boolean grantGlobalStage(ResourceLocation stage) {
        boolean changed = globalStages.add(stage);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean revokeGlobalStage(ResourceLocation stage) {
        boolean changed = globalStages.remove(stage);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public int resetGlobalProgress() {
        int changed = globalStages.size() + globalFacts.size() + globalQuestUnlocks.size() + appliedGlobalStages.values().stream().mapToInt(Set::size).sum() + (globalRank == null ? 0 : 1);
        globalStages.clear();
        globalFacts.clear();
        globalQuestUnlocks.clear();
        appliedGlobalStages.clear();
        globalRank = null;
        if (changed > 0) {
            setDirty();
        }
        return changed;
    }

    public Set<ResourceLocation> globalStages() {
        return Set.copyOf(globalStages);
    }

    public boolean hasLocalStage(UUID subject, ResourceLocation stage) {
        return localStages.getOrDefault(subject, Set.of()).contains(stage);
    }

    public boolean grantLocalStage(UUID subject, ResourceLocation stage) {
        return grantLocalStage(subject, stage, true);
    }

    public boolean grantLocalStage(UUID subject, ResourceLocation stage, boolean direct) {
        boolean changed = localStages.computeIfAbsent(subject, ignored -> new LinkedHashSet<>()).add(stage);
        if (direct) {
            changed = directLocalStages.computeIfAbsent(subject, ignored -> new LinkedHashSet<>()).add(stage) || changed;
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean revokeLocalStage(UUID subject, ResourceLocation stage) {
        Set<ResourceLocation> stages = localStages.get(subject);
        if (stages == null) {
            return false;
        }
        boolean changed = stages.remove(stage);
        Set<ResourceLocation> directStages = directLocalStages.get(subject);
        if (directStages != null) {
            changed = directStages.remove(stage) || changed;
            if (directStages.isEmpty()) {
                directLocalStages.remove(subject);
            }
        }
        if (stages.isEmpty()) {
            localStages.remove(subject);
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Set<ResourceLocation> localStages(UUID subject) {
        return Set.copyOf(localStages.getOrDefault(subject, Set.of()));
    }

    public boolean hasDirectLocalStage(UUID subject, ResourceLocation stage) {
        return directLocalStages.getOrDefault(subject, Set.of()).contains(stage);
    }

    public Set<ResourceLocation> directLocalStages(UUID subject) {
        return Set.copyOf(directLocalStages.getOrDefault(subject, Set.of()));
    }

    public int resetLocalProgress(UUID subject, UUID playerId) {
        int changed = 0;
        Set<ResourceLocation> stages = localStages.remove(subject);
        if (stages != null) {
            changed += stages.size();
        }
        Set<ResourceLocation> directStages = directLocalStages.remove(subject);
        if (directStages != null) {
            changed += directStages.size();
        }
        Set<String> facts = localFacts.remove(subject);
        if (facts != null) {
            changed += facts.size();
        }
        Set<ResourceLocation> applied = appliedLocalStages.remove(playerId);
        if (applied != null) {
            changed += applied.size();
        }
        Set<ResourceLocation> quests = localQuestUnlocks.remove(subject);
        if (quests != null) {
            changed += quests.size();
        }
        if (localRanks.remove(subject) != null) {
            changed++;
        }
        if (changed > 0) {
            setDirty();
        }
        return changed;
    }

    public boolean recordGlobalFact(String fact) {
        boolean changed = globalFacts.add(fact);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean recordLocalFact(UUID subject, String fact) {
        boolean changed = localFacts.computeIfAbsent(subject, ignored -> new LinkedHashSet<>()).add(fact);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Set<String> globalFacts() {
        return Set.copyOf(globalFacts);
    }

    public Set<String> localFacts(UUID subject) {
        return Set.copyOf(localFacts.getOrDefault(subject, Set.of()));
    }

    public boolean unlockGlobalQuest(ResourceLocation quest) {
        boolean changed = globalQuestUnlocks.add(quest);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean unlockLocalQuest(UUID subject, ResourceLocation quest) {
        boolean changed = localQuestUnlocks.computeIfAbsent(subject, ignored -> new LinkedHashSet<>()).add(quest);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean hasQuestUnlocked(UUID subject, ResourceLocation quest) {
        return globalQuestUnlocks.contains(quest) || localQuestUnlocks.getOrDefault(subject, Set.of()).contains(quest);
    }

    public Set<ResourceLocation> globalQuestUnlocks() {
        return Set.copyOf(globalQuestUnlocks);
    }

    public Set<ResourceLocation> localQuestUnlocks(UUID subject) {
        return Set.copyOf(localQuestUnlocks.getOrDefault(subject, Set.of()));
    }

    public ResourceLocation globalRank() {
        return globalRank;
    }

    public boolean promoteGlobalRank(ResourceLocation rank, int newIndex, int oldIndex) {
        if (rank == null || newIndex < 0 || oldIndex >= newIndex) {
            return false;
        }
        globalRank = rank;
        setDirty();
        return true;
    }

    public ResourceLocation localRank(UUID subject) {
        return localRanks.get(subject);
    }

    public boolean promoteLocalRank(UUID subject, ResourceLocation rank, int newIndex, int oldIndex) {
        if (rank == null || newIndex < 0 || oldIndex >= newIndex) {
            return false;
        }
        localRanks.put(subject, rank);
        setDirty();
        return true;
    }

    public boolean setFtbQuestTrigger(String target, ResourceLocation stage) {
        boolean changed = !stage.equals(ftbQuestTriggers.put(target, stage));
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public ResourceLocation ftbQuestTrigger(String target) {
        return ftbQuestTriggers.get(target);
    }

    public Map<String, ResourceLocation> ftbQuestTriggers() {
        return Map.copyOf(ftbQuestTriggers);
    }

    public boolean setFtbQuestGate(String target, ResourceLocation stage, StageScope scope) {
        QuestGate gate = new QuestGate(stage, scope);
        boolean changed = !gate.equals(ftbQuestGates.put(target, gate));
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public QuestGate ftbQuestGate(String target) {
        return ftbQuestGates.get(target);
    }

    public Map<String, QuestGate> ftbQuestGates() {
        return Map.copyOf(ftbQuestGates);
    }

    public boolean hasAppliedGlobalStage(UUID playerId, ResourceLocation stage) {
        return appliedGlobalStages.getOrDefault(playerId, Set.of()).contains(stage);
    }

    public void markAppliedGlobalStage(UUID playerId, ResourceLocation stage) {
        appliedGlobalStages.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).add(stage);
        setDirty();
    }

    public boolean hasAppliedLocalStage(UUID playerId, ResourceLocation stage) {
        return appliedLocalStages.getOrDefault(playerId, Set.of()).contains(stage);
    }

    public void markAppliedLocalStage(UUID playerId, ResourceLocation stage) {
        appliedLocalStages.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).add(stage);
        setDirty();
    }

    private static NoreStagesSavedData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        NoreStagesSavedData data = new NoreStagesSavedData();
        readResourceSet(tag.getList("globalStages", Tag.TAG_STRING), data.globalStages);
        readStringSet(tag.getList("globalFacts", Tag.TAG_STRING), data.globalFacts);
        readResourceMap(tag.getList("localStages", Tag.TAG_COMPOUND), data.localStages);
        readResourceMap(tag.getList("directLocalStages", Tag.TAG_COMPOUND), data.directLocalStages);
        readStringMap(tag.getList("localFacts", Tag.TAG_COMPOUND), data.localFacts);
        readResourceMap(tag.getList("appliedGlobalStages", Tag.TAG_COMPOUND), data.appliedGlobalStages);
        readResourceMap(tag.getList("appliedLocalStages", Tag.TAG_COMPOUND), data.appliedLocalStages);
        readResourceSet(tag.getList("globalQuestUnlocks", Tag.TAG_STRING), data.globalQuestUnlocks);
        readResourceMap(tag.getList("localQuestUnlocks", Tag.TAG_COMPOUND), data.localQuestUnlocks);
        if (tag.contains("globalRank", Tag.TAG_STRING)) {
            data.globalRank = ResourceLocation.parse(tag.getString("globalRank"));
        }
        readSubjectResourceMap(tag.getList("localRanks", Tag.TAG_COMPOUND), data.localRanks);
        readStringResourceMap(tag.getList("ftbQuestTriggers", Tag.TAG_COMPOUND), data.ftbQuestTriggers);
        readQuestGateMap(tag.getList("ftbQuestGates", Tag.TAG_COMPOUND), data.ftbQuestGates);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        tag.put("globalStages", writeResourceSet(globalStages));
        tag.put("globalFacts", writeStringSet(globalFacts));
        tag.put("localStages", writeResourceMap(localStages));
        tag.put("directLocalStages", writeResourceMap(directLocalStages));
        tag.put("localFacts", writeStringMap(localFacts));
        tag.put("appliedGlobalStages", writeResourceMap(appliedGlobalStages));
        tag.put("appliedLocalStages", writeResourceMap(appliedLocalStages));
        tag.put("globalQuestUnlocks", writeResourceSet(globalQuestUnlocks));
        tag.put("localQuestUnlocks", writeResourceMap(localQuestUnlocks));
        if (globalRank != null) {
            tag.putString("globalRank", globalRank.toString());
        }
        tag.put("localRanks", writeSubjectResourceMap(localRanks));
        tag.put("ftbQuestTriggers", writeStringResourceMap(ftbQuestTriggers));
        tag.put("ftbQuestGates", writeQuestGateMap(ftbQuestGates));
        return tag;
    }

    private static ListTag writeResourceSet(Set<ResourceLocation> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value.toString())));
        return list;
    }

    private static ListTag writeStringSet(Set<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }

    private static ListTag writeResourceMap(Map<UUID, Set<ResourceLocation>> map) {
        ListTag list = new ListTag();
        map.forEach((subject, stages) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("subject", subject);
            tag.put("values", writeResourceSet(stages));
            list.add(tag);
        });
        return list;
    }

    private static ListTag writeStringMap(Map<UUID, Set<String>> map) {
        ListTag list = new ListTag();
        map.forEach((subject, facts) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("subject", subject);
            tag.put("values", writeStringSet(facts));
            list.add(tag);
        });
        return list;
    }

    private static void readResourceSet(ListTag list, Set<ResourceLocation> out) {
        for (int i = 0; i < list.size(); i++) {
            out.add(ResourceLocation.parse(list.getString(i)));
        }
    }

    private static void readStringSet(ListTag list, Set<String> out) {
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
    }

    private static void readResourceMap(ListTag list, Map<UUID, Set<ResourceLocation>> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Set<ResourceLocation> values = new LinkedHashSet<>();
            readResourceSet(tag.getList("values", Tag.TAG_STRING), values);
            out.put(tag.getUUID("subject"), values);
        }
    }

    private static void readStringMap(ListTag list, Map<UUID, Set<String>> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Set<String> values = new LinkedHashSet<>();
            readStringSet(tag.getList("values", Tag.TAG_STRING), values);
            out.put(tag.getUUID("subject"), values);
        }
    }

    private static ListTag writeSubjectResourceMap(Map<UUID, ResourceLocation> map) {
        ListTag list = new ListTag();
        map.forEach((subject, value) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("subject", subject);
            tag.putString("value", value.toString());
            list.add(tag);
        });
        return list;
    }

    private static void readSubjectResourceMap(ListTag list, Map<UUID, ResourceLocation> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            out.put(tag.getUUID("subject"), ResourceLocation.parse(tag.getString("value")));
        }
    }

    private static ListTag writeStringResourceMap(Map<String, ResourceLocation> map) {
        ListTag list = new ListTag();
        map.forEach((target, stage) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("target", target);
            tag.putString("stage", stage.toString());
            list.add(tag);
        });
        return list;
    }

    private static void readStringResourceMap(ListTag list, Map<String, ResourceLocation> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            out.put(tag.getString("target"), ResourceLocation.parse(tag.getString("stage")));
        }
    }

    private static ListTag writeQuestGateMap(Map<String, QuestGate> map) {
        ListTag list = new ListTag();
        map.forEach((target, gate) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("target", target);
            tag.putString("stage", gate.stage().toString());
            tag.putString("scope", gate.scope().name().toLowerCase(java.util.Locale.ROOT));
            list.add(tag);
        });
        return list;
    }

    private static void readQuestGateMap(ListTag list, Map<String, QuestGate> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String target = tag.getString("target");
            ResourceLocation stage = ResourceLocation.parse(tag.getString("stage"));
            StageScope scope = StageScope.parse(tag.getString("scope"), StageScope.LOCAL);
            out.put(target, new QuestGate(stage, scope));
        }
    }

    public record QuestGate(ResourceLocation stage, StageScope scope) {
    }
}
