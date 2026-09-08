package com.santipdr.copyl.client;

import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compact list of the nearest dropped-item groups already known to Loot ESP. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LootEspHud {
    private static final long SUMMARY_INTERVAL_MS = 160L;
    private static final int MAX_ROWS = 5;
    private static final int MAX_ENTITIES_CONSIDERED = 96;
    private static final String[] CARDINALS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};

    private static ClientLevel cachedLevel;
    private static long nextRefreshAt;
    private static List<Row> rows = List.of();

    private LootEspHud() {
    }

    private static final class MutableRow {
        private final String name;
        private int totalCount;
        private final double nearestDistance;
        private final double nearestX;
        private final double nearestY;
        private final double nearestZ;

        private MutableRow(String name, int totalCount, double nearestDistance, ItemEntity nearest) {
            this.name = name;
            this.totalCount = totalCount;
            this.nearestDistance = nearestDistance;
            this.nearestX = nearest.getX();
            this.nearestY = nearest.getY();
            this.nearestZ = nearest.getZ();
        }
    }

    private record Row(String name, int count, int distance, String direction, int deltaY) {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        if (!config.lootEsp
                || !config.lootEspHud
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui
                || minecraft.screen != null) {
            if (!config.lootEsp || !config.lootEspHud || minecraft.level == null) clear();
            return;
        }

        refreshIfNeeded(minecraft, config);
        if (rows.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int panelW = Math.min(252, Math.max(150, screenW / 4));
        panelW = Math.min(panelW, Math.max(80, screenW - 16));
        int height = 22 + rows.size() * 13;
        HudAnchor anchor = HudAnchor.fromConfig(config.lootHudAnchor, HudAnchor.TOP_LEFT);
        int left = anchor.left(screenW, panelW, 8);
        int top = anchor.top(screenH, height, 8);

        graphics.fill(left, top, left + panelW, top + height, 0xB00A1016);
        graphics.fill(left, top, left + 2, top + height, 0xFF6FC2FF);
        graphics.fill(left + 2, top, left + panelW, top + 1, 0x6072C5FF);
        graphics.drawString(minecraft.font, "LOOT CERCANO", left + 8, top + 6, 0xFFE9F3FA, false);

        int y = top + 19;
        for (Row row : rows) {
            String text = row.name + " x" + row.count + "  ·  " + row.distance + "m";
            if (config.lootEspHudDirection) {
                String vertical = row.deltaY > 0 ? "+" + row.deltaY : Integer.toString(row.deltaY);
                text += "  ·  " + row.direction + " ΔY " + vertical;
            }
            graphics.drawString(minecraft.font,
                    minecraft.font.plainSubstrByWidth(text, Math.max(20, panelW - 14)),
                    left + 8,
                    y,
                    0xFFAABAC7,
                    false);
            y += 13;
        }
    }

    private static void refreshIfNeeded(Minecraft minecraft, LClientConfig config) {
        long now = System.currentTimeMillis();
        if (cachedLevel == minecraft.level && now < nextRefreshAt) return;
        cachedLevel = minecraft.level;
        nextRefreshAt = now + SUMMARY_INTERVAL_MS;

        List<ItemEntity> source = LootEspRenderer.cachedItemsView();
        if (source.isEmpty()) {
            rows = List.of();
            return;
        }

        double rangeSq = (double) config.lootEspRange * config.lootEspRange;
        Map<String, MutableRow> grouped = new LinkedHashMap<>();
        int considered = 0;

        for (ItemEntity entity : source) {
            if (considered++ >= MAX_ENTITIES_CONSIDERED) break;
            if (entity == null
                    || entity.level() != minecraft.level
                    || entity.isRemoved()
                    || entity.getItem().isEmpty()
                    || entity.getItem().getCount() < config.lootEspMinStack) {
                continue;
            }

            double distanceSq = entity.distanceToSqr(minecraft.player);
            if (distanceSq > rangeSq) continue;
            double distance = Math.sqrt(distanceSq);
            ItemStack stack = entity.getItem();
            String key = safeItemKey(stack);
            MutableRow existing = grouped.get(key);
            if (existing == null) {
                if (grouped.size() >= MAX_ROWS) continue;
                grouped.put(key, new MutableRow(safeItemName(stack, key), stack.getCount(), distance, entity));
            } else {
                existing.totalCount += stack.getCount();
            }
        }

        if (grouped.isEmpty()) {
            rows = List.of();
            return;
        }

        List<Row> next = new ArrayList<>(grouped.size());
        for (MutableRow row : grouped.values()) {
            double dx = row.nearestX - minecraft.player.getX();
            double dz = row.nearestZ - minecraft.player.getZ();
            int deltaY = (int) Math.round(row.nearestY - minecraft.player.getY());
            next.add(new Row(
                    row.name,
                    row.totalCount,
                    Math.max(0, (int) Math.round(row.nearestDistance)),
                    cardinal(dx, dz),
                    deltaY
            ));
        }
        rows = List.copyOf(next);
    }

    private static String cardinal(double dx, double dz) {
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        if (yaw < 0.0D) yaw += 360.0D;
        int index = Math.floorMod((int) Math.round(yaw / 45.0D), CARDINALS.length);
        return CARDINALS[index];
    }

    private static String safeItemKey(ItemStack stack) {
        try {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id == null ? "item" : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "item";
        }
    }

    private static String safeItemName(ItemStack stack, String fallback) {
        try {
            String name = stack.getHoverName().getString();
            return name == null || name.isBlank() ? fallback : name;
        } catch (RuntimeException | LinkageError ignored) {
            return fallback;
        }
    }

    private static void clear() {
        cachedLevel = null;
        nextRefreshAt = 0L;
        rows = List.of();
    }
}
