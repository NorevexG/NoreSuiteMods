package com.nore.quest.mixin;

import com.nore.quest.ftb.QuestShareService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ftb.mods.ftbquests.quest.task.ItemTask", remap = false)
public abstract class FtbItemTaskCompletionMixin {
    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void norequest$beforeSubmitCompletion(@Coerce Object teamData, ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        QuestShareService.beforeFtbSubmitCompletion(teamData, this);
    }

    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void norequest$shareSubmitCompletion(@Coerce Object teamData, ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        QuestShareService.shareFromFtbSubmitCompletion(player, teamData, this);
    }
}
