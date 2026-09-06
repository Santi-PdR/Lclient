package com.santipdr.copyl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Dropped-item ESP rendered entirely by Lclient.
 *
 * The old implementation toggled Minecraft's glowing flag and could be
 * overwritten by entity renderers/mods. This one renders a dedicated line box
 * for every matching ItemEntity and flushes it while depth testing is disabled.
 */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LootEspRenderer {
    private LootEspRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        if (!config.lootEsp
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui) {
            return;
        }

        double range = config.lootEspRange;
        double rangeSq = range * range;
        List<ItemEntity> items = minecraft.level.getEntitiesOfClass(
                ItemEntity.class,
                minecraft.player.getBoundingBox().inflate(range),
                item -> !item.isRemoved()
                        && !item.getItem().isEmpty()
                        && item.getItem().getCount() >= config.lootEspMinStack
                        && item.distanceToSqr(minecraft.player) <= rangeSq
        );
        if (items.isEmpty()) return;

        var camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType lineType = RenderType.lines();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        try {
            VertexConsumer lines = buffers.getBuffer(lineType);
            float pulse = 0.78F + 0.22F * (float) Math.sin(System.currentTimeMillis() / 210.0D);

            for (ItemEntity item : items) {
                int count = item.getItem().getCount();

                // Stack size is readable at a glance without adding labels everywhere.
                float red;
                float green;
                float blue;
                if (count >= 32) {
                    red = 1.0F;
                    green = 0.82F;
                    blue = 0.28F;
                } else if (count >= 16) {
                    red = 0.45F;
                    green = 0.92F;
                    blue = 0.72F;
                } else {
                    red = 0.38F;
                    green = 0.74F;
                    blue = 1.0F;
                }

                LevelRenderer.renderLineBox(
                        poses,
                        lines,
                        item.getBoundingBox().inflate(0.11D).move(-camera.x, -camera.y, -camera.z),
                        red,
                        green,
                        blue,
                        pulse
                );
            }

            // Flush before restoring depth so the ESP batch is actually drawn
            // under the state selected above.
            buffers.endBatch(lineType);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
}
