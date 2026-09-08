package com.santipdr.copyl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dropped-item ESP rendered entirely by Lclient.
 *
 * Only ItemEntity instances already present in the client level are rendered.
 * The short-lived entity cache is explicitly released on logout/disable so it
 * can never keep an old ClientLevel alive after leaving a world or server.
 */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LootEspRenderer {
    private static final long SCAN_INTERVAL_MS = 100L;
    /** Prevent pathological drop piles/farms from turning ESP into a render bottleneck. */
    private static final int MAX_RENDERED_ITEMS = 256;

    private static final float[] COLOR_LARGE = {1.0F, 0.82F, 0.28F};
    private static final float[] COLOR_MEDIUM = {0.45F, 0.92F, 0.72F};
    private static final float[] COLOR_SMALL = {0.38F, 0.74F, 1.0F};

    private static ClientLevel cachedLevel;
    private static List<ItemEntity> cachedItems = Collections.emptyList();
    private static long nextScanAt;
    private static int cachedRange = -1;
    private static int cachedMinStack = -1;

    private LootEspRenderer() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearCache();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();

        if (minecraft.player == null || minecraft.level == null) {
            clearCache();
            return;
        }
        if (!config.lootEsp) {
            clearCache();
            return;
        }
        if (minecraft.options.hideGui) return;

        refreshCacheIfNeeded(minecraft, config);
        if (cachedItems.isEmpty()) return;

        double range = config.lootEspRange;
        double rangeSq = range * range;
        var camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType lineType = LClientRenderTypes.lootEspLines();
        VertexConsumer lines = buffers.getBuffer(lineType);
        float pulse = 0.80F + 0.20F * (float) Math.sin(System.currentTimeMillis() / 190.0D);

        for (ItemEntity item : cachedItems) {
            if (!isRenderable(item, minecraft, config, rangeSq)) continue;

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

    private static boolean isRenderable(ItemEntity item, Minecraft minecraft, LClientConfig config, double rangeSq) {
        return item != null
                && item.level() == minecraft.level
                && !item.isRemoved()
                && !item.getItem().isEmpty()
                && item.getItem().getCount() >= config.lootEspMinStack
                && item.distanceToSqr(minecraft.player) <= rangeSq;
    }

    private static void refreshCacheIfNeeded(Minecraft minecraft, LClientConfig config) {
        long now = System.currentTimeMillis();
        boolean levelChanged = cachedLevel != minecraft.level;
        boolean settingsChanged = cachedRange != config.lootEspRange || cachedMinStack != config.lootEspMinStack;
        if (!levelChanged && !settingsChanged && now < nextScanAt) return;

        cachedLevel = minecraft.level;
        cachedRange = config.lootEspRange;
        cachedMinStack = config.lootEspMinStack;
        nextScanAt = now + SCAN_INTERVAL_MS;

        double range = config.lootEspRange;
        double rangeSq = range * range;
        List<ItemEntity> found = minecraft.level.getEntitiesOfClass(
                ItemEntity.class,
                minecraft.player.getBoundingBox().inflate(range),
                item -> !item.isRemoved()
                        && !item.getItem().isEmpty()
                        && item.getItem().getCount() >= config.lootEspMinStack
                        && item.distanceToSqr(minecraft.player) <= rangeSq
        );

        if (found.isEmpty()) {
            cachedItems = Collections.emptyList();
            return;
        }

        // Keep the cache distance-sorted once per scan. Both the world renderer
        // and the compact HUD can now reuse the same ordering without another
        // entity scan or a per-frame sort.
        found.sort(Comparator
                .comparingDouble((ItemEntity item) -> item.distanceToSqr(minecraft.player))
                .thenComparing((ItemEntity item) -> -item.getItem().getCount()));

        if (found.size() > MAX_RENDERED_ITEMS) {
            cachedItems = new ArrayList<>(found.subList(0, MAX_RENDERED_ITEMS));
        } else {
            cachedItems = new ArrayList<>(found);
        }
    }

    /** Read-only-by-convention view reused by the HUD on the render thread. */
    static List<ItemEntity> cachedItemsView() {
        return cachedItems;
    }

    public static void clearCache() {
        cachedLevel = null;
        cachedItems = Collections.emptyList();
        nextScanAt = 0L;
        cachedRange = -1;
        cachedMinStack = -1;
    }

    private static float[] colorForStack(int count) {
        if (count >= 32) return COLOR_LARGE;
        if (count >= 16) return COLOR_MEDIUM;
        return COLOR_SMALL;
    }
}
