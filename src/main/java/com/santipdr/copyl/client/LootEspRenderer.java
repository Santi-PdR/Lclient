package com.santipdr.copyl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Dropped-item ESP rendered entirely by Lclient.
 *
 * It only renders ItemEntity instances already present in the client level.
 * The dedicated render type uses NO_DEPTH_TEST, so boxes and optional beacon
 * markers remain visible behind terrain without touching vanilla entity glow.
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
        RenderType lineType = LClientRenderTypes.lootEspLines();
        VertexConsumer lines = buffers.getBuffer(lineType);
        float pulse = 0.80F + 0.20F * (float) Math.sin(System.currentTimeMillis() / 190.0D);

        for (ItemEntity item : items) {
            int count = item.getItem().getCount();
            float[] color = colorForStack(count);

            AABB itemBox = item.getBoundingBox().inflate(0.13D).move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(
                    poses,
                    lines,
                    itemBox,
                    color[0],
                    color[1],
                    color[2],
                    pulse
            );

            if (config.lootEspBeacon) {
                double cx = item.getX() - camera.x;
                double baseY = item.getY() - camera.y;
                double cz = item.getZ() - camera.z;
                AABB beacon = new AABB(
                        cx - 0.035D,
                        baseY - 0.08D,
                        cz - 0.035D,
                        cx + 0.035D,
                        baseY + 1.85D,
                        cz + 0.035D
                );
                LevelRenderer.renderLineBox(
                        poses,
                        lines,
                        beacon,
                        color[0],
                        color[1],
                        color[2],
                        0.52F + 0.28F * pulse
                );
            }
        }

        buffers.endBatch(lineType);
    }

    private static float[] colorForStack(int count) {
        if (count >= 32) return new float[]{1.0F, 0.82F, 0.28F};
        if (count >= 16) return new float[]{0.45F, 0.92F, 0.72F};
        return new float[]{0.38F, 0.74F, 1.0F};
    }
}
