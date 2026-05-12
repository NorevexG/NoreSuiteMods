package com.nore.quest.mixin;

import com.nore.quest.ftb.QuestShareService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ftb.mods.ftbquests.quest.task.Task", remap = false)
public abstract class FtbTaskSubmitActorMixin {
    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void norequest$beginSubmitActor(@Coerce Object teamData, ServerPlayer player, CallbackInfo ci) {
        QuestShareService.beginActorContext(player);
    }

    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void norequest$endSubmitActor(@Coerce Object teamData, ServerPlayer player, CallbackInfo ci) {
        QuestShareService.endActorContext(player);
    }

    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void norequest$beginSubmitActor(@Coerce Object teamData, ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        QuestShareService.beginActorContext(player);
    }

    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void norequest$endSubmitActor(@Coerce Object teamData, ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        QuestShareService.endActorContext(player);
    }
}
