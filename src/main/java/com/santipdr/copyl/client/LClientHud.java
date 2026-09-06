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
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LClientHud {
    private static final int MAX_NOTICES = 50;
    private static final int MAX_COMBAT = 40;
    private static final Deque<Notice> NOTICES = new ArrayDeque<>();
    private static final Deque<SoundPing> SOUNDS = new ArrayDeque<>();
    private static final Deque<CombatEntry> COMBAT = new ArrayDeque<>();
    private static final Map<String, Long> SELF_SOUND_UNTIL = new HashMap<>();
    private static int unreadNotices;
    private static int unreadCombat;

    private LClientHud() {
    }

    public static synchronized void notify(String text) {
        long now = System.currentTimeMillis();
        Iterator<Notice> iterator = NOTICES.iterator();
        while (iterator.hasNext()) {
            Notice notice = iterator.next();
            if (notice.text.equals(text) && now - notice.time < 2500L) {
                iterator.remove();
                notice.time = now;
                notice.count++;
                NOTICES.addFirst(notice);
                unreadNotices++;
                return;
            }
        }

        NOTICES.addFirst(new Notice(text, now));
        while (NOTICES.size() > MAX_NOTICES) NOTICES.removeLast();
        unreadNotices++;
    }

    public static synchronized void recordCombat(String text) {
        COMBAT.addFirst(new CombatEntry(text, System.currentTimeMillis()));
        while (COMBAT.size() > MAX_COMBAT) COMBAT.removeLast();
        unreadCombat++;
    }

    public static synchronized List<NoticeView> noticeSnapshot() {
        List<NoticeView> result = new ArrayList<>(NOTICES.size());
        for (Notice notice : NOTICES) {
            result.add(new NoticeView(notice.text, notice.count, notice.time));
        }
        return result;
    }

    public static synchronized List<CombatView> combatSnapshot() {
        List<CombatView> result = new ArrayList<>(COMBAT.size());
        for (CombatEntry entry : COMBAT) {
            result.add(new CombatView(entry.text, entry.time));
        }
        return result;
    }

    public static synchronized int getUnreadCenterCount() {
        return unreadNotices + unreadCombat;
    }

    public static synchronized void markCenterRead() {
        unreadNotices = 0;
        unreadCombat = 0;
    }

    public static synchronized void clearNotices() {
        NOTICES.clear();
        unreadNotices = 0;
    }

    public static synchronized void clearCombat() {
        COMBAT.clear();
        unreadCombat = 0;
    }

    @SubscribeEvent
    public static void onLevelSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        LClientConfig config = LClientConfig.get();
        if (!config.soundRadar || !config.soundRadarIgnoreSelf || !event.getLevel().isClientSide()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getEntity() != minecraft.player || event.getSound() == null) return;

        String id = event.getSound().value().getLocation().toString();
        synchronized (SELF_SOUND_UNTIL) {
            SELF_SOUND_UNTIL.put(id, System.currentTimeMillis() + 500L);
        }
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
        if (distance > config.soundRadarRange) return;

        if (config.soundRadarIgnoreSelf && isLikelySelfSound(sound, distance)) return;
        pushSound(sound.getX(), sound.getY(), sound.getZ(), kind, System.currentTimeMillis());
    }

    private static boolean isLikelySelfSound(SoundInstance sound, double distance) {
        long now = System.currentTimeMillis();
        String id = sound.getLocation().toString();
        synchronized (SELF_SOUND_UNTIL) {
            SELF_SOUND_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
            Long until = SELF_SOUND_UNTIL.get(id);
            if (until != null && until >= now) return true;
        }

        // Fallback for modded first-person sounds that are emitted directly from the camera
        // without an AtEntity event.
        return distance < 1.8D;
    }

    private static synchronized void pushSound(double x, double y, double z, String kind, long now) {
        Iterator<SoundPing> iterator = SOUNDS.iterator();
        while (iterator.hasNext()) {
            SoundPing ping = iterator.next();
            double dx = ping.x - x;
            double dy = ping.y - y;
            double dz = ping.z - z;
            if (ping.kind.equals(kind)
                    && now - ping.time < 500L
                    && dx * dx + dy * dy + dz * dz < 100.0D) {
                iterator.remove();
                break;
            }
        }

        SOUNDS.addFirst(new SoundPing(x, y, z, kind, now));
        while (SOUNDS.size() > 14) SOUNDS.removeLast();
    }

    private static String classify(String path) {
        if (containsAny(path,
                "gun", "shoot", "shot", "firearm", "rifle", "pistol", "revolver", "sniper",
                "bullet", "cannon", "machinegun", "machine_gun", "rocket", "missile", "shell",
                "bow", "crossbow", "arrow", "burst", "automatic", "semi_auto", "semi-auto")) return "DISPARO";
        if (containsAny(path, "explode", "explosion", "grenade", "blast", "detonate", "bomb")) return "EXPLOSIÓN";
        if (containsAny(path, "step", "footstep", "walk", "run")) return "PASOS";
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
        pruneSounds(now);
        LClientConfig config = LClientConfig.get();

        if (config.combatPanel) renderTargetPanel(graphics, minecraft);
        if (config.soundRadar) renderSoundRadar(graphics, minecraft, now);
        if (config.recon && CopyLClientEvents.isReconZoomActive()) renderReconZoom(graphics, minecraft);
    }

    private static void renderTargetPanel(GuiGraphics graphics, Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof EntityHitResult hit)) return;

        int x = 8;
        int y = 8;
        int maxTextWidth = Math.max(120, minecraft.getWindow().getGuiScaledWidth() / 2 - 24);
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
    }

    private static void renderSoundRadar(GuiGraphics graphics, Minecraft minecraft, long now) {
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenW / 2;
        int centerY = screenH / 2;
        int radiusX = Math.min(118, Math.max(72, screenW / 5));
        int radiusY = Math.min(72, Math.max(46, screenH / 6));

        List<SoundPing> snapshot;
        synchronized (LClientHud.class) {
            snapshot = new ArrayList<>(SOUNDS);
        }

        int shown = 0;
        for (SoundPing ping : snapshot) {
            if (shown++ >= 8) break;

            double dx = ping.x - minecraft.player.getX();
            double dy = ping.y - minecraft.player.getY();
            double dz = ping.z - minecraft.player.getZ();
            int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            double bearing = Math.toDegrees(Math.atan2(-dx, dz));
            float relative = Mth.wrapDegrees((float) bearing - minecraft.player.getYRot());
            double radians = Math.toRadians(relative);

            int x = centerX - (int) Math.round(Math.sin(radians) * radiusX);
            int y = centerY - (int) Math.round(Math.cos(radians) * radiusY);
            String vertical = dy > 4.0D ? "▲ " : dy < -4.0D ? "▼ " : "";
            String text = vertical + shortSoundName(ping.kind) + " " + distance + "m";

            int age = (int) (now - ping.time);
            float fade = Mth.clamp(1.0F - age / 2600.0F, 0.0F, 1.0F);
            if (fade <= 0.0F) continue;

            int width = minecraft.font.width(text) + 10;
            int alpha = Math.round(190 * fade);
            int textAlpha = Math.round(255 * fade);
            int accent = soundAccent(ping.kind);

            graphics.fill(x - width / 2, y - 8, x + width / 2, y + 7, (alpha << 24) | 0x0B0F14);
            graphics.fill(x - width / 2, y + 6, x + width / 2, y + 7,
                    (textAlpha << 24) | (accent & 0x00FFFFFF));
            graphics.drawCenteredString(minecraft.font, text, x, y - 4,
                    (textAlpha << 24) | 0x00F7F7F7);
        }
    }

    private static String shortSoundName(String kind) {
        return switch (kind) {
            case "INTERACCIÓN" -> "INTERAC.";
            case "EXPLOSIÓN" -> "EXPLOSIÓN";
            default -> kind;
        };
    }

    private static int soundAccent(String kind) {
        return switch (kind) {
            case "DISPARO" -> 0xF3D57A;
            case "EXPLOSIÓN" -> 0xF29A72;
            case "PASOS" -> 0xE8EEF4;
            case "DAÑO" -> 0xE98B8B;
            case "BLOQUE" -> 0x9BCB9B;
            default -> 0x8CCBFF;
        };
    }

    private static void renderReconZoom(GuiGraphics graphics, Minecraft minecraft) {
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        // Lightweight binocular/optic brackets. The actual zoom comes from a temporary FOV change.
        int halfW = 74;
        int halfH = 44;
        int arm = 15;
        int accent = 0xD0B9D8FF;
        graphics.fill(centerX - halfW, centerY - halfH, centerX - halfW + arm, centerY - halfH + 1, accent);
        graphics.fill(centerX - halfW, centerY - halfH, centerX - halfW + 1, centerY - halfH + arm, accent);
        graphics.fill(centerX + halfW - arm, centerY - halfH, centerX + halfW, centerY - halfH + 1, accent);
        graphics.fill(centerX + halfW - 1, centerY - halfH, centerX + halfW, centerY - halfH + arm, accent);
        graphics.fill(centerX - halfW, centerY + halfH - 1, centerX - halfW + arm, centerY + halfH, accent);
        graphics.fill(centerX - halfW, centerY + halfH - arm, centerX - halfW + 1, centerY + halfH, accent);
        graphics.fill(centerX + halfW - arm, centerY + halfH - 1, centerX + halfW, centerY + halfH, accent);
        graphics.fill(centerX + halfW - 1, centerY + halfH - arm, centerX + halfW, centerY + halfH, accent);

        String target = null;
        if (minecraft.hitResult instanceof BlockHitResult block) {
            double distance = minecraft.player.distanceToSqr(block.getBlockPos().getX() + 0.5D,
                    block.getBlockPos().getY() + 0.5D,
                    block.getBlockPos().getZ() + 0.5D);
            target = block.getBlockPos().toShortString() + " · " + Math.round(Math.sqrt(distance)) + "m";
        } else if (minecraft.hitResult instanceof EntityHitResult entity) {
            target = entity.getEntity().getName().getString() + " · "
                    + entity.getEntity().blockPosition().toShortString() + " · "
                    + Math.round(minecraft.player.distanceTo(entity.getEntity())) + "m";
        }

        LClientConfig config = LClientConfig.get();
        graphics.drawCenteredString(minecraft.font, "RECON · FOV " + config.reconZoomFov,
                centerX, centerY - halfH - 14, 0xFFB9D8FF);
        if (target != null) {
            String clipped = minecraft.font.plainSubstrByWidth(target, 240);
            graphics.drawCenteredString(minecraft.font, clipped, centerX, centerY + halfH + 7, 0xFFE3F0FF);
        }
    }

    private static synchronized void pruneSounds(long now) {
        SOUNDS.removeIf(sound -> now - sound.time > 2600L);
    }

    public record NoticeView(String text, int count, long time) {
    }

    public record CombatView(String text, long time) {
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
