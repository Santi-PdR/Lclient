package com.santipdr.copyl.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Reliable dropped-item ESP. Unlike the old client-side glowing flag, these
 * boxes are rendered by Lclient itself and deliberately ignore depth.
 */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LootEspRenderer {
    private static final RenderType LOOT_LINES = RenderType.create(
            "lclient_loot_esp_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            512,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    private LootEspRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        if (!config.lootEsp || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;

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
        VertexConsumer lines = buffers.getBuffer(LOOT_LINES);

        float pulse = 0.82F + 0.18F * (float) Math.sin(System.currentTimeMillis() / 220.0D);
        for (ItemEntity item : items) {
            int count = item.getItem().getCount();
            float r = count >= 32 ? 1.0F : 0.30F;
            float g = count >= 16 ? 0.88F : 0.78F;
            float b = count >= 32 ? 0.35F : 1.0F;

            LevelRenderer.renderLineBox(
                    poses,
                    lines,
                    item.getBoundingBox().inflate(0.10D).move(-camera.x, -camera.y, -camera.z),
                    r,
                    g,
                    b,
                    pulse
            );
        }

        buffers.endBatch(LOOT_LINES);
    }
}
