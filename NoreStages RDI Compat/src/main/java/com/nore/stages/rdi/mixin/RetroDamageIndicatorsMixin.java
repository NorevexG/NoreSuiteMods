package com.nore.stages.rdi.mixin;

import com.github.alexmodguy.retrodamageindicators.RetroDamageIndicators;
import com.nore.stages.rdi.client.RdiDangerBadgeRenderer;
import com.nore.stages.rdi.client.RdiHudThemeTextures;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RetroDamageIndicators.class, remap = false)
public abstract class RetroDamageIndicatorsMixin {
    @ModifyArg(
            method = "onPreRenderGuiElement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"),
            index = 0
    )
    private static ResourceLocation norestagesRdiCompat$replaceMainHudTexture(ResourceLocation texture) {
        return RdiHudThemeTextures.replaceMainHudTexture(texture);
    }

    @Inject(method = "onPreRenderGuiElement", at = @At("TAIL"))
    private static void norestagesRdiCompat$renderDangerBadge(RenderGuiLayerEvent.Pre event, CallbackInfo ci) {
        RdiDangerBadgeRenderer.render(event, RetroDamageIndicatorsAccessor.norestagesRdiCompat$getDamageIndicatorEntity());
    }
}
