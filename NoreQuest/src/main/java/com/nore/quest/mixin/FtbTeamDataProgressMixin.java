package com.nore.quest.mixin;

import com.nore.quest.ftb.QuestShareService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ftb.mods.ftbquests.quest.TeamData", remap = false)
public abstract class FtbTeamDataProgressMixin {
    @Inject(method = "setProgress", at = @At("HEAD"), remap = false)
    private void norequest$beforeProgress(@Coerce Object task, long progress, CallbackInfo ci) {
        QuestShareService.beforeFtbProgress(this, task);
    }

    @Inject(method = "setProgress", at = @At("TAIL"), remap = false)
    private void norequest$shareProgress(@Coerce Object task, long progress, CallbackInfo ci) {
        QuestShareService.shareFromFtbProgress(this, task);
    }

    @Inject(method = "markTaskCompleted", at = @At("TAIL"), remap = false)
    private void norequest$shareCompletion(@Coerce Object task, CallbackInfo ci) {
        QuestShareService.shareFromFtbCompletion(this, task);
    }
}
