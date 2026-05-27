package com.nore.stages.network;

import com.nore.stages.NoreStages;
import com.nore.stages.client.NoreStagesClientState;
import com.nore.stages.compat.norequest.NoreQuestCompat;
import com.nore.stages.registry.StageDataRegistry;
import com.nore.stages.rule.StageRestriction;
import com.nore.stages.service.StageService;
import com.nore.stages.stage.StageScope;
import com.nore.stages.team.StageSubjectResolver;
import com.nore.stages.world.NoreStagesSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = NoreStages.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NoreStagesNetwork {
    private NoreStagesNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(LockedContentPayload.TYPE, LockedContentPayload.STREAM_CODEC, NoreStagesNetwork::handleLockedContent);
    }

    public static void sendSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildLockedContent(player));
    }

    public static void refreshClientIntegrations() {
        try {
            Class.forName("com.nore.stages.client.NoreStagesClientState")
                    .getDeclaredMethod("refreshClientIntegrations")
                    .invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void handleLockedContent(LockedContentPayload payload, IPayloadContext context) {
        NoreStagesClientState.setLockedContent(payload);
    }

    private static LockedContentPayload buildLockedContent(ServerPlayer player) {
        Map<ResourceLocation, List<String>> lockedItems = new LinkedHashMap<>();
        List<ResourceLocation> lockedRecipes = new ArrayList<>();
        for (StageRestriction restriction : StageDataRegistry.recipeRestrictions()) {
            if (StageService.hasStage(player, restriction.stage(), restriction.scope())) {
                continue;
            }
            Optional<RecipeHolder<?>> recipe = player.server.getRecipeManager().byKey(restriction.target());
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack result = recipe.get().value().getResultItem(player.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            lockedRecipes.add(restriction.target());
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
            String stageName = StageDataRegistry.stage(restriction.stage())
                    .map(stage -> stage.displayName())
                    .orElse(restriction.stage().toString());
            lockedItems.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(stageName);
        }
        List<LockedItem> items = lockedItems.entrySet().stream()
                .map(entry -> new LockedItem(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        NoreStagesSavedData data = NoreStagesSavedData.get(player.level());
        Set<ResourceLocation> activeStages = new LinkedHashSet<>(data.globalStages());
        activeStages.addAll(data.localStages(StageSubjectResolver.localSubject(player)));
        Set<ResourceLocation> directStages = new LinkedHashSet<>(data.globalStages());
        directStages.addAll(data.directLocalStages(StageSubjectResolver.localSubject(player)));
        List<FtbQuestGate> ftbQuestGates = data.ftbQuestGates().entrySet().stream()
                .map(entry -> new FtbQuestGate(entry.getKey(), entry.getValue().stage(), NoreQuestCompat.isSoloQuest(player, entry.getKey())))
                .toList();
        List<RankRule> rankRules = StageDataRegistry.entityRankRules().stream()
                .map(rule -> new RankRule(rule.entity(), rule.rank(), rule.stage(), rule.scope()))
                .toList();
        return new LockedContentPayload(
                items,
                List.copyOf(lockedRecipes),
                List.copyOf(activeStages),
                List.copyOf(directStages),
                ftbQuestGates,
                StageDataRegistry.ranks(),
                StageService.effectiveRank(player),
                StageDataRegistry.rankData().warningDifference(),
                StageDataRegistry.rankData().deadlyDifference(),
                rankRules);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NoreStages.MODID, path);
    }

    public record LockedContentPayload(List<LockedItem> items, List<ResourceLocation> recipes, List<ResourceLocation> stages, List<ResourceLocation> directStages, List<FtbQuestGate> ftbQuestGates, List<ResourceLocation> ranks, ResourceLocation playerRank, int warningDifference, int deadlyDifference, List<RankRule> rankRules) implements CustomPacketPayload {
        public static final Type<LockedContentPayload> TYPE = new Type<>(id("locked_content"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LockedContentPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> payload.write(buf),
                LockedContentPayload::read
        );

        private static LockedContentPayload read(FriendlyByteBuf buf) {
            return new LockedContentPayload(
                    buf.readList(LockedItem::read),
                    buf.readList(FriendlyByteBuf::readResourceLocation),
                    buf.readList(FriendlyByteBuf::readResourceLocation),
                    buf.readList(FriendlyByteBuf::readResourceLocation),
                    buf.readList(FtbQuestGate::read),
                    buf.readList(FriendlyByteBuf::readResourceLocation),
                    readNullableResource(buf),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readList(RankRule::read));
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeCollection(items, LockedItem::write);
            buf.writeCollection(recipes, FriendlyByteBuf::writeResourceLocation);
            buf.writeCollection(stages, FriendlyByteBuf::writeResourceLocation);
            buf.writeCollection(directStages, FriendlyByteBuf::writeResourceLocation);
            buf.writeCollection(ftbQuestGates, FtbQuestGate::write);
            buf.writeCollection(ranks, FriendlyByteBuf::writeResourceLocation);
            writeNullableResource(buf, playerRank);
            buf.writeVarInt(warningDifference);
            buf.writeVarInt(deadlyDifference);
            buf.writeCollection(rankRules, RankRule::write);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RankRule(ResourceLocation entity, ResourceLocation rank, ResourceLocation stage, StageScope scope) {
        private static RankRule read(FriendlyByteBuf buf) {
            return new RankRule(buf.readResourceLocation(), buf.readResourceLocation(), readNullableResource(buf), StageScope.parse(buf.readUtf(), StageScope.GLOBAL));
        }

        private static void write(FriendlyByteBuf buf, RankRule rule) {
            buf.writeResourceLocation(rule.entity);
            buf.writeResourceLocation(rule.rank);
            writeNullableResource(buf, rule.stage);
            buf.writeUtf(rule.scope.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static ResourceLocation readNullableResource(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    private static void writeNullableResource(FriendlyByteBuf buf, ResourceLocation id) {
        buf.writeBoolean(id != null);
        if (id != null) {
            buf.writeResourceLocation(id);
        }
    }

    public record LockedItem(ResourceLocation item, List<String> stages) {
        private static LockedItem read(FriendlyByteBuf buf) {
            return new LockedItem(buf.readResourceLocation(), buf.readList(FriendlyByteBuf::readUtf));
        }

        private static void write(FriendlyByteBuf buf, LockedItem item) {
            buf.writeResourceLocation(item.item);
            buf.writeCollection(item.stages, FriendlyByteBuf::writeUtf);
        }
    }

    public record FtbQuestGate(String target, ResourceLocation stage, boolean directOnly) {
        private static FtbQuestGate read(FriendlyByteBuf buf) {
            return new FtbQuestGate(buf.readUtf(), buf.readResourceLocation(), buf.readBoolean());
        }

        private static void write(FriendlyByteBuf buf, FtbQuestGate gate) {
            buf.writeUtf(gate.target);
            buf.writeResourceLocation(gate.stage);
            buf.writeBoolean(gate.directOnly);
        }
    }
}
