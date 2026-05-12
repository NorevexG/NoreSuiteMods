package com.nore.quest.mixin;

import dev.architectury.event.EventResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ftb.mods.ftbteams.client.FTBTeamsClient", remap = false)
public abstract class FtbTeamsClientMixin {
    @Shadow
    public static KeyMapping openTeamsKey;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void norequest$blockOpenTeamsKey(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers, CallbackInfoReturnable<EventResult> cir) {
        if (openTeamsKey != null && openTeamsKey.isDown()) {
            cir.setReturnValue(EventResult.interruptTrue());
        }
    }

    @Inject(method = "openMyTeamGui", at = @At("HEAD"), cancellable = true, remap = false)
    private static void norequest$blockTeamScreen(@Coerce Object properties, @Coerce Object permissions, CallbackInfo ci) {
        ci.cancel();
    }
}
