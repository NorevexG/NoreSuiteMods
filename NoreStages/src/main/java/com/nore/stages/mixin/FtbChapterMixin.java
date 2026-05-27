package com.nore.stages.mixin;

import com.nore.stages.compat.ftb.FtbQuestBridge;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Chapter.class, remap = false)
public class FtbChapterMixin {
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void norestages$hideLockedChapter(TeamData teamData, CallbackInfoReturnable<Boolean> cir) {
        Chapter chapter = (Chapter) (Object) this;
        if (!FtbQuestBridge.isVisible("chapter", chapter.getCodeString(), teamData)) {
            cir.setReturnValue(false);
        }
    }
}
