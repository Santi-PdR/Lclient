package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD kept deliberately minimal: it only exists while Advanced Recon is zooming. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LClientHud {
    private LClientHud() {
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        if (!config.recon
                || !ReconController.isZoomActive()
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        HitResult hit = ReconController.getTargetHit(minecraft);
        renderReconFrame(graphics, minecraft, hit, config);
        if (hit instanceof EntityHitResult entityHit) {
            renderTargetPanel(graphics, minecraft, entityHit.getEntity());
        }
    }

    private static void renderReconFrame(GuiGraphics graphics, Minecraft minecraft, HitResult hit, LClientConfig config) {
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        int halfW = Math.min(112, Math.max(78, screenW / 7));
        int halfH = Math.min(68, Math.max(48, screenH / 7));
        int arm = 18;
        int accent = 0xD8B9D8FF;
        int soft = 0x806B8299;

        graphics.fill(cx - halfW, cy - halfH, cx - halfW + arm, cy - halfH + 1, accent);
        graphics.fill(cx - halfW, cy - halfH, cx - halfW + 1, cy - halfH + arm, accent);
        graphics.fill(cx + halfW - arm, cy - halfH, cx + halfW, cy - halfH + 1, accent);
        graphics.fill(cx + halfW - 1, cy - halfH, cx + halfW, cy - halfH + arm, accent);
        graphics.fill(cx - halfW, cy + halfH - 1, cx - halfW + arm, cy + halfH, accent);
        graphics.fill(cx - halfW, cy + halfH - arm, cx - halfW + 1, cy + halfH, accent);
        graphics.fill(cx + halfW - arm, cy + halfH - 1, cx + halfW, cy + halfH, accent);
        graphics.fill(cx + halfW - 1, cy + halfH - arm, cx + halfW, cy + halfH, accent);

        graphics.fill(cx - 8, cy, cx - 2, cy + 1, soft);
        graphics.fill(cx + 3, cy, cx + 9, cy + 1, soft);
        graphics.fill(cx, cy - 8, cx + 1, cy - 2, soft);
        graphics.fill(cx, cy + 3, cx + 1, cy + 9, soft);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xE8EAF5FF);

        String header = "RECON  " + ReconController.getZoomText()
                + "  ·  " + ReconController.getZoomFov() + "°"
                + "  ·  " + config.reconRange + "m";
        graphics.drawCenteredString(minecraft.font, header, cx, cy - halfH - 17, 0xFFE6F2FF);

        String target = targetText(minecraft, hit);
        if (!target.isBlank()) {
            int maxWidth = Math.min(390, screenW - 40);
            target = minecraft.font.plainSubstrByWidth(target, maxWidth);
            graphics.drawCenteredString(minecraft.font, target, cx, cy + halfH + 8, 0xFFE4EEF8);
        }

        String status = ReconController.getStatusText();
        if (!status.isBlank()) {
            graphics.drawCenteredString(minecraft.font, status, cx, cy + halfH + 22, 0xFF8ED8FF);
        } else {
            String hint = "Rueda: zoom  ·  " + keyName(config.reconWaypointKey) + ": waypoint";
            graphics.drawCenteredString(minecraft.font, hint, cx, cy + halfH + 22, 0xFF8394A5);
        }
    }

    private static String targetText(Minecraft minecraft, HitResult hit) {
        if (hit == null) return "";
        double distance = ReconController.targetDistance(minecraft, hit);

        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            return entity.getName().getString() + "  ·  "
                    + entity.blockPosition().toShortString() + "  ·  "
                    + Math.round(distance) + "m";
        }

        if (hit instanceof BlockHitResult blockHit) {
            var pos = ReconController.targetBlockPos(blockHit);
            if (pos == null) return "";
            if (hit.getType() == HitResult.Type.MISS) {
                return "Dirección  ·  " + pos.toShortString() + "  ·  " + Math.round(distance) + "m";
            }
            String blockId = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(pos).getBlock()).toString();
            return blockId + "  ·  " + pos.toShortString() + "  ·  " + Math.round(distance) + "m";
        }
        return "";
    }

    private static void renderTargetPanel(GuiGraphics graphics, Minecraft minecraft, Entity entity) {
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int panelW = Math.min(226, Math.max(174, screenW / 5));
        int x = screenW - panelW - 12;
        int y = 14;

        String title = entity.getName().getString();
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String distance = Math.round(minecraft.player.distanceTo(entity)) + " m";
        LivingEntity living = entity instanceof LivingEntity l ? l : null;
        String hp = living == null ? null : Math.round(living.getHealth()) + " / " + Math.round(living.getMaxHealth()) + " HP";

        int height = living == null ? 48 : 69;
        graphics.fill(x, y, x + panelW, y + height, 0xC00C1118);
        graphics.fill(x, y, x + 2, y + height, 0xFF72C5FF);
        graphics.fill(x + 2, y, x + panelW, y + 1, 0x6072C5FF);

        graphics.drawString(minecraft.font,
                minecraft.font.plainSubstrByWidth(title, panelW - 14),
                x + 8, y + 7, 0xFFF1F7FC, false);
        graphics.drawString(minecraft.font,
                minecraft.font.plainSubstrByWidth(type, panelW - 14),
                x + 8, y + 20, 0xFF8EA0B1, false);
        graphics.drawString(minecraft.font,
                distance + "  ·  " + entity.blockPosition().toShortString(),
                x + 8, y + 33, 0xFFB8C8D7, false);
        if (living != null) {
            graphics.drawString(minecraft.font, hp, x + 8, y + 46, 0xFFE8B0B0, false);
            int barX = x + 8;
            int barY = y + 59;
            int barW = panelW - 16;
            float healthRatio = living.getMaxHealth() <= 0.0F ? 0.0F
                    : Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F);
            graphics.fill(barX, barY, barX + barW, barY + 4, 0x80364141);
            graphics.fill(barX, barY, barX + Math.round(barW * healthRatio), barY + 4, 0xD8E07B7B);
        }
    }

    private static String keyName(int key) {
        return key < 0
                ? "Sin tecla"
                : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }
}
