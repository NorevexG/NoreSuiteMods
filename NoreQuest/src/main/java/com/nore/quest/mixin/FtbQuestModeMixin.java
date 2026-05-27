package com.nore.quest.mixin;

import com.nore.quest.api.QuestCompletionMode;
import com.nore.quest.ftb.FtbQuestModeAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ftb.mods.ftbquests.quest.Quest", remap = false)
public abstract class FtbQuestModeMixin {
    private static final NameMap<QuestCompletionMode> NOREQUEST_MODE_NAME_MAP = NameMap
            .of(QuestCompletionMode.SOLO, QuestCompletionMode.values())
            .id(mode -> mode.name().toLowerCase())
            .baseNameKey("norequest.completion_mode")
            .create();

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void norequest$readCompletionMode(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        FtbQuestModeAccess.read(this, tag);
    }

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void norequest$writeCompletionMode(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        FtbQuestModeAccess.write(this, tag);
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void norequest$readCompletionModeNet(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        FtbQuestModeAccess.set(this, buffer.readEnum(QuestCompletionMode.class));
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void norequest$writeCompletionModeNet(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeEnum(FtbQuestModeAccess.get(this));
    }

    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void norequest$addCompletionModeConfig(ConfigGroup group, CallbackInfo ci) {
        ConfigGroup noreQuestGroup = group.getOrCreateSubgroup("norequest")
                .setNameKey("norequest.category");

        noreQuestGroup.addEnum("completion_mode", FtbQuestModeAccess.get(this),
                        mode -> FtbQuestModeAccess.set(this, mode),
                        NOREQUEST_MODE_NAME_MAP, QuestCompletionMode.SOLO)
                .setNameKey("norequest.completion_mode")
                .setOrder(0);
    }
}
