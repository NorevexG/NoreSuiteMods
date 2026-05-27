package com.nore.stages.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nore.stages.NoreStages;
import com.nore.stages.NoreStagesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

public final class ThreatIndicatorRenderer {
    private static final ResourceLocation WARNING = ResourceLocation.fromNamespaceAndPath(NoreStages.MODID, "textures/gui/threat_warning.png");
    private static final ResourceLocation DEADLY = ResourceLocation.fromNamespaceAndPath(NoreStages.MODID, "textures/gui/threat_deadly.png");
    private static final float SCALE = 0.025F;
    private static final float WARNING_HALF_SIZE = 17.0F;
    private static final float DEADLY_HALF_SIZE = 19.0F;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_DISTANCE_SQR = 64.0D * 64.0D;

    private ThreatIndicatorRenderer() {
    }

    public static void render(RenderLivingEvent.Post<?, ?> event) {
        if (!NoreStagesConfig.showEnemyRankIndicators()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.isInvisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getEntityRenderDispatcher().distanceToSqr(entity) > MAX_DISTANCE_SQR) {
            return;
        }
        NoreStagesClientState.Threat threat = NoreStagesClientState.threat(entity.getType());
        if (threat == NoreStagesClientState.Threat.NONE) {
            return;
        }
        ResourceLocation texture = threat == NoreStagesClientState.Threat.DEADLY ? DEADLY : WARNING;
        renderIcon(event.getPoseStack(), event.getMultiBufferSource(), entity, texture, threat);
    }

    private static void renderIcon(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity, ResourceLocation texture, NoreStagesClientState.Threat threat) {
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.34D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-SCALE, -SCALE, SCALE);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertex = buffer.getBuffer(RenderType.text(texture));
        float halfSize = threat == NoreStagesClientState.Threat.DEADLY ? DEADLY_HALF_SIZE : WARNING_HALF_SIZE;
        drawQuad(poseStack, vertex, matrix, halfSize);
        poseStack.popPose();
    }

    private static void drawQuad(PoseStack poseStack, VertexConsumer vertex, Matrix4f matrix, float halfSize) {
        vertex.addVertex(matrix, -halfSize, -halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        vertex.addVertex(matrix, halfSize, -halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        vertex.addVertex(matrix, halfSize, halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        vertex.addVertex(matrix, -halfSize, halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }
}
