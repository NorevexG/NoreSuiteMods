package com.nore.stages.mixin;

import com.nore.stages.compat.ftb.FtbQuestBridge;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Quest.class, remap = false)
public class FtbQuestMixin {
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void norestages$hideLockedQuest(TeamData teamData, CallbackInfoReturnable<Boolean> cir) {
        Quest quest = (Quest) (Object) this;
        if (!FtbQuestBridge.isVisible("quest", quest.getCodeString(), teamData)) {
            cir.setReturnValue(false);
        }
    }
}
