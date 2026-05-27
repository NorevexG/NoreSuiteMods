package com.nore.stages.mixin;

import com.nore.stages.service.WorldEventGateService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raids.class)
public abstract class RaidsMixin {
    @Inject(method = "createOrExtendRaid", at = @At("HEAD"), cancellable = true)
    private void norestages$suppressLockedRaid(ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
        if (WorldEventGateService.shouldSuppressVanillaRaid(player, pos)) {
            cir.setReturnValue(null);
        }
    }
}
