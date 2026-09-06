package com.santipdr.copyl.client;

import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LClientHud {
    private static final Deque<Notice> NOTICES = new ArrayDeque<>();
    private static final Deque<SoundPing> SOUNDS = new ArrayDeque<>();
    private static final Deque<CombatEntry> COMBAT = new ArrayDeque<>();

    private LClientHud() {
    }

    public static void notify(String text) {
        long now = System.currentTimeMillis();
        Iterator<Notice> iterator = NOTICES.iterator();
        while (iterator.hasNext()) {
            Notice notice = iterator.next();
            if (notice.text.equals(text) && now - notice.time < 1200L) {
                iterator.remove();
                notice.time = now;
                notice.count++;
                NOTICES.addFirst(notice);
                return;
            }
        }

        NOTICES.addFirst(new Notice(text, now));
        while (NOTICES.size() > 5) NOTICES.removeLast();
    }

    public static void recordCombat(String text) {
        COMBAT.addFirst(new CombatEntry(text, System.currentTimeMillis()));
        while (COMBAT.size() > 6) COMBAT.removeLast();
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        LClientConfig config = LClientConfig.get();
        Minecraft minecraft = Minecraft.getInstance();
        if (!config.soundRadar || minecraft.player == null || event.getSound() == null) return;

        SoundInstance sound = event.getSound();
        if (sound.getSource() == SoundSource.MUSIC
                || sound.getSource() == SoundSource.AMBIENT
                || sound.getSource() == SoundSource.WEATHER) {
            return;
        }

        String path = sound.getLocation().getPath().toLowerCase(Locale.ROOT);
        String kind = classify(path);
        if (kind == null) return;

        double dx = sound.getX() - minecraft.player.getX();
        double dy = sound.getY() - minecraft.player.getY();
        double dz = sound.getZ() - minecraft.player.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > config.soundRadarRange || distance < 1.5D) return;

        pushSound(sound.getX(), sound.getY(), sound.getZ(), kind, System.currentTimeMillis());
    }

    private static void pushSound(double x, double y, double z, String kind, long now) {
        Iterator<SoundPing> iterator = SOUNDS.iterator();
        while (iterator.hasNext()) {
            SoundPing ping = iterator.next();
            double dx = ping.x - x;
            double dy = ping.y - y;
            double dz = ping.z - z;
            if (ping.kind.equals(kind)
                    && now - ping.time < 450L
                    && dx * dx + dy * dy + dz * dz < 81.0D) {
                iterator.remove();
                break;
            }
        }

        SOUNDS.addFirst(new SoundPing(x, y, z, kind, now));
        while (SOUNDS.size() > 12) SOUNDS.removeLast();
    }

    private static String classify(String path) {
        if (containsAny(path,
                "gun", "shoot", "shot", "firearm", "rifle", "pistol", "revolver", "sniper",
                "bullet", "cannon", "machinegun", "machine_gun", "rocket", "missile", "shell",
                "bow", "crossbow", "arrow")) return "DISPARO";
        if (containsAny(path, "explode", "explosion", "grenade", "blast", "detonate")) return "EXPLOSIÓN";
        if (containsAny(path, "step", "footstep")) return "PASOS";
        if (containsAny(path, "door", "trapdoor", "gate", "chest", "barrel")) return "INTERACCIÓN";
        if (containsAny(path, "break", "place", "dig", "mine")) return "BLOQUE";
        if (containsAny(path, "hurt", "damage", "death")) return "DAÑO";
        return null;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        long now = System.currentTimeMillis();
        prune(now);
        LClientConfig config = LClientConfig.get();

        if (config.combatPanel) renderTargetAndCombat(graphics, minecraft, now);
        renderNotices(graphics, minecraft, now);
        if (config.soundRadar) renderSoundRadar(graphics, minecraft, now);
        if (config.recon) renderRecon(graphics, minecraft);
    }

    private static void renderTargetAndCombat(GuiGraphics graphics, Minecraft minecraft, long now) {
        int x = 8;
        int y = 8;
        int maxTextWidth = Math.max(120, minecraft.getWindow().getGuiScaledWidth() / 2 - 24);

        if (minecraft.hitResult instanceof EntityHitResult hit) {
            Entity entity = hit.getEntity();
            String title = "OBJETIVO: " + entity.getName().getString();
            String data = "Dist " + Math.round(minecraft.player.distanceTo(entity)) + "m | " + entity.blockPosition().toShortString();
            if (entity instanceof LivingEntity living) {
                data += " | " + Math.round(living.getHealth()) + "/" + Math.round(living.getMaxHealth()) + " HP";
            }

            title = minecraft.font.plainSubstrByWidth(title, maxTextWidth - 12);
            data = minecraft.font.plainSubstrByWidth(data, maxTextWidth - 12);
            int width = Math.min(maxTextWidth, Math.max(minecraft.font.width(title), minecraft.font.width(data)) + 12);
            graphics.fill(x, y, x + width, y + 31, 0xB010141B);
            graphics.fill(x, y, x + 2, y + 31, 0xFF67B7FF);
            graphics.drawString(minecraft.font, title, x + 7, y + 5, 0xFFE8F4FF, false);
            graphics.drawString(minecraft.font, data, x + 7, y + 18, 0xFFA8B7C6, false);
            y += 37;
        }

        int count = 0;
        for (CombatEntry entry : COMBAT) {
            if (count++ >= 3) break;
            int alpha = (int) (255 * Mth.clamp(1.0F - (now - entry.time) / 15000.0F, 0.15F, 1.0F));
            String text = minecraft.font.plainSubstrByWidth(entry.text, maxTextWidth);
            graphics.drawString(minecraft.font, text, x, y, (alpha << 24) | 0xFFB4B4, true);
            y += 11;
        }
    }

    private static void renderNotices(GuiGraphics graphics, Minecraft minecraft, long now) {
        int y = 8;
        int shown = 0;
        for (Notice notice : NOTICES) {
            if (shown++ >= 4) break;

            String text = notice.count > 1 ? notice.text + " ×" + notice.count : notice.text;
            int age = (int) (now - notice.time);
            float fade = age > 5200 ? Mth.clamp((6500 - age) / 1300.0F, 0.0F, 1.0F) : 1.0F;
            int alpha = Math.round(210 * fade);
            int width = Math.min(minecraft.font.width(text) + 12, minecraft.getWindow().getGuiScaledWidth() - 16);
            int x = minecraft.getWindow().getGuiScaledWidth() - width - 8;
            String clipped = minecraft.font.plainSubstrByWidth(text, width - 12);

            graphics.fill(x, y, x + width, y + 20, (alpha << 24) | 0x121720);
            graphics.fill(x + width - 2, y, x + width, y + 20, (Math.round(255 * fade) << 24) | 0x67B7FF);
            graphics.drawString(minecraft.font, clipped, x + 6, y + 6, (Math.round(255 * fade) << 24) | 0xE8F4FF, false);
            y += 24;
        }
    }

    private static void renderSoundRadar(GuiGraphics graphics, Minecraft minecraft, long now) {
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int y = 30;
        int shown = 0;

        for (SoundPing ping : SOUNDS) {
            if (shown++ >= 5) break;

            double dx = ping.x - minecraft.player.getX();
            double dy = ping.y - minecraft.player.getY();
            double dz = ping.z - minecraft.player.getZ();
            int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            double bearing = Math.toDegrees(Math.atan2(-dx, dz));
            float relative = Mth.wrapDegrees((float) bearing - minecraft.player.getYRot());
            String vertical = dy > 4.0D ? "▲ " : dy < -4.0D ? "▼ " : "";
            String text = arrow(relative) + " " + vertical + ping.kind + " " + distance + "m";
            int width = minecraft.font.width(text) + 10;
            int age = (int) (now - ping.time);
            float fade = Mth.clamp(1.0F - age / 3200.0F, 0.15F, 1.0F);

            graphics.fill(centerX - width / 2, y - 3, centerX + width / 2, y + 9,
                    (Math.round(142 * fade) << 24) | 0x0B0F14);
            graphics.drawCenteredString(minecraft.font, text, centerX, y,
                    (Math.round(255 * fade) << 24) | 0xF4E6C1);
            y += 14;
        }
    }

    private static String arrow(float relative) {
        if (relative >= -22.5F && relative < 22.5F) return "↑";
        if (relative >= 22.5F && relative < 67.5F) return "↖";
        if (relative >= 67.5F && relative < 112.5F) return "←";
        if (relative >= 112.5F && relative < 157.5F) return "↙";
        if (relative >= -67.5F && relative < -22.5F) return "↗";
        if (relative >= -112.5F && relative < -67.5F) return "→";
        if (relative >= -157.5F && relative < -112.5F) return "↘";
        return "↓";
    }

    private static void renderRecon(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.hitResult == null) return;
        String text = null;
        if (minecraft.hitResult instanceof BlockHitResult block) {
            text = "Recon " + block.getBlockPos().toShortString();
        } else if (minecraft.hitResult instanceof EntityHitResult entity) {
            text = "Recon " + entity.getEntity().blockPosition().toShortString();
        }

        if (text != null) {
            int x = minecraft.getWindow().getGuiScaledWidth() / 2;
            int y = minecraft.getWindow().getGuiScaledHeight() / 2 + 18;
            graphics.drawCenteredString(minecraft.font, text, x, y, 0xFFB9D8FF);
        }
    }

    private static void prune(long now) {
        NOTICES.removeIf(n -> now - n.time > 6500L);
        SOUNDS.removeIf(s -> now - s.time > 3200L);
        COMBAT.removeIf(c -> now - c.time > 15000L);
    }

    private static final class Notice {
        private final String text;
        private long time;
        private int count = 1;

        private Notice(String text, long time) {
            this.text = text;
            this.time = time;
        }
    }

    private record SoundPing(double x, double y, double z, String kind, long time) {
    }

    private record CombatEntry(String text, long time) {
    }
}
