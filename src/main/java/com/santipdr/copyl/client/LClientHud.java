package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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

        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        if (screenW >= 520 && screenH >= 150 && hit instanceof EntityHitResult entityHit) {
            renderTargetPanel(graphics, minecraft, entityHit.getEntity());
        }
    }

    private static void renderReconFrame(GuiGraphics graphics, Minecraft minecraft, HitResult hit, LClientConfig config) {
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        int horizontalLimit = Math.max(38, (screenW - 24) / 2);
        int verticalLimit = Math.max(24, (screenH - 72) / 2);
        int halfW = Math.min(horizontalLimit, Math.min(112, Math.max(58, screenW / 7)));
        int halfH = Math.min(verticalLimit, Math.min(68, Math.max(38, screenH / 7)));
        int arm = Math.min(18, Math.max(10, Math.min(halfW, halfH) / 3));
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

        int textWidth = Math.max(80, screenW - 20);
        String header = "RECON  " + ReconController.getZoomText()
                + "  ·  " + ReconController.getZoomFov() + "°"
                + "  ·  " + config.reconRange + "m";
        drawCenteredClipped(graphics, minecraft, header, cx, Math.max(3, cy - halfH - 17), textWidth, 0xFFE6F2FF);

        String target = targetText(minecraft, hit);
        if (!target.isBlank()) {
            drawCenteredClipped(graphics, minecraft, target, cx,
                    Math.min(screenH - 23, cy + halfH + 8),
                    Math.max(80, Math.min(390, screenW - 20)),
                    0xFFE4EEF8);
        }

        int footerY = Math.min(screenH - 10, cy + halfH + 22);
        String status = ReconController.getStatusText();
        if (!status.isBlank()) {
            drawCenteredClipped(graphics, minecraft, status, cx, footerY, textWidth, 0xFF8ED8FF);
        } else {
            String hint = "Rueda: zoom  ·  " + keyName(config.reconWaypointKey) + ": waypoint";
            drawCenteredClipped(graphics, minecraft, hint, cx, footerY, textWidth, 0xFF8394A5);
        }
    }

    private static String targetText(Minecraft minecraft, HitResult hit) {
        if (hit == null) return "";
        double distance = ReconController.targetDistance(minecraft, hit);

        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            StringBuilder text = new StringBuilder(ReconController.safeEntityLabel(entity))
                    .append("  ·  ")
                    .append(entity.blockPosition().toShortString())
                    .append("  ·  ")
                    .append(Math.round(distance)).append("m");
            if (entity instanceof LivingEntity living) {
                float health = safeHealth(living);
                float maxHealth = safeMaxHealth(living);
                if (Float.isFinite(health) && Float.isFinite(maxHealth) && maxHealth > 0.0F) {
                    text.append("  ·  ")
                            .append(Math.round(health))
                            .append('/')
                            .append(Math.round(maxHealth))
                            .append(" HP");
                }
            }
            return text.toString();
        }

        if (hit instanceof BlockHitResult blockHit) {
            var pos = ReconController.targetBlockPos(blockHit);
            if (pos == null) return "";
            if (hit.getType() == HitResult.Type.MISS) {
                return "Dirección  ·  " + pos.toShortString() + "  ·  " + Math.round(distance) + "m";
            }
            return safeBlockLabel(minecraft, pos) + "  ·  " + pos.toShortString() + "  ·  " + Math.round(distance) + "m";
        }
        return "";
    }

    private static void renderTargetPanel(GuiGraphics graphics, Minecraft minecraft, Entity entity) {
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int panelW = Math.min(246, Math.max(184, screenW / 5));
        panelW = Math.min(panelW, screenW - 16);
        int x = Math.max(8, screenW - panelW - 8);
        int y = 10;

        String title = ReconController.safeEntityLabel(entity);
        String type = safeEntityType(entity);
        String distance = Math.round(minecraft.player.distanceTo(entity)) + " m";
        LivingEntity living = entity instanceof LivingEntity l ? l : null;
        float health = living == null ? Float.NaN : safeHealth(living);
        float maxHealth = living == null ? Float.NaN : safeMaxHealth(living);
        boolean validHealth = Float.isFinite(health) && Float.isFinite(maxHealth) && maxHealth > 0.0F;
        String hp = validHealth ? Math.round(health) + " / " + Math.round(maxHealth) + " HP" : null;
        String equipment = living == null ? "" : safeEquipmentSummary(living);

        int equipmentOffset = 46;
        int hpOffset = equipment.isBlank() ? 46 : 59;
        int barOffset = hpOffset + 13;
        int height;
        if (validHealth) height = barOffset + 8;
        else if (!equipment.isBlank()) height = equipmentOffset + 16;
        else height = 48;

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
                minecraft.font.plainSubstrByWidth(distance + "  ·  " + entity.blockPosition().toShortString(), panelW - 14),
                x + 8, y + 33, 0xFFB8C8D7, false);

        if (!equipment.isBlank()) {
            graphics.drawString(minecraft.font,
                    minecraft.font.plainSubstrByWidth(equipment, panelW - 14),
                    x + 8, y + equipmentOffset, 0xFFD4C9A8, false);
        }

        if (validHealth) {
            graphics.drawString(minecraft.font, hp, x + 8, y + hpOffset, 0xFFE8B0B0, false);
            int barX = x + 8;
            int barY = y + barOffset;
            int barW = panelW - 16;
            float healthRatio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
            graphics.fill(barX, barY, barX + barW, barY + 4, 0x80364141);
            graphics.fill(barX, barY, barX + Math.round(barW * healthRatio), barY + 4, 0xD8E07B7B);
        }
    }

    private static String safeEquipmentSummary(LivingEntity living) {
        try {
            ItemStack main = living.getMainHandItem();
            ItemStack off = living.getOffhandItem();
            int armorPieces = 0;
            for (ItemStack armor : living.getArmorSlots()) {
                if (armor != null && !armor.isEmpty()) armorPieces++;
            }

            String held = !main.isEmpty() ? safeItemName(main) : !off.isEmpty() ? safeItemName(off) : "Manos vacías";
            String handPrefix = !main.isEmpty() ? "Mano: " : !off.isEmpty() ? "Offhand: " : "";
            String armor = armorPieces > 0 ? " · Armadura " + armorPieces + "/4" : "";
            return handPrefix + held + armor;
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    private static String safeItemName(ItemStack stack) {
        try {
            String name = stack.getHoverName().getString();
            if (name != null && !name.isBlank()) return name;
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id == null ? "item" : id.getPath();
        } catch (RuntimeException | LinkageError ignored) {
            return "item";
        }
    }

    private static String safeEntityType(Entity entity) {
        try {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id == null ? "entity" : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "entity";
        }
    }

    private static String safeBlockLabel(Minecraft minecraft, net.minecraft.core.BlockPos pos) {
        try {
            var id = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(pos).getBlock());
            return id == null ? "block" : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "block";
        }
    }

    private static float safeHealth(LivingEntity living) {
        try {
            return living.getHealth();
        } catch (RuntimeException | LinkageError ignored) {
            return Float.NaN;
        }
    }

    private static float safeMaxHealth(LivingEntity living) {
        try {
            return living.getMaxHealth();
        } catch (RuntimeException | LinkageError ignored) {
            return Float.NaN;
        }
    }

    private static void drawCenteredClipped(
            GuiGraphics graphics,
            Minecraft minecraft,
            String text,
            int cx,
            int y,
            int maxWidth,
            int color
    ) {
        graphics.drawCenteredString(
                minecraft.font,
                minecraft.font.plainSubstrByWidth(text, Math.max(40, maxWidth)),
                cx,
                y,
                color
        );
    }

    private static String keyName(int key) {
        return key < 0
                ? "Sin tecla"
                : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }
}
