package com.nore.stages.mixin;

import com.nore.stages.service.WorldEventGateService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin {
    @Shadow
    private int nextTick;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void norestages$scalePatrolFrequency(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies, CallbackInfoReturnable<Integer> cir) {
        if (!spawnEnemies) {
            return;
        }
        if (nextTick <= 0 && WorldEventGateService.shouldSuppress(level, WorldEventGateService.PATROL)) {
            nextTick = 12000 + level.random.nextInt(1200);
            cir.setReturnValue(0);
        }
    }
}
