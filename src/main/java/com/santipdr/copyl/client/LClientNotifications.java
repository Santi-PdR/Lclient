package com.santipdr.copyl.client;

import com.santipdr.copyl.CopyL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/** Shared, non-chat notification surface for Lclient modules and critical gameplay state. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class LClientNotifications {
    private static final int HISTORY_LIMIT = 48;
    private static final long DUPLICATE_WINDOW_MS = 900L;

    private static final Deque<Notice> active = new ArrayDeque<>();
    private static final Deque<HistoryEntry> history = new ArrayDeque<>();

    private static boolean initializedStates;
    private static boolean lastQuickMessages;
    private static boolean lastLootEsp;
    private static boolean lastSmartOffhand;
    private static boolean lastRecon;
    private static boolean lastJourneyMap;
    private static String lastReconStatus = "";

    private static boolean healthCritical;
    private static boolean inventoryFull;
    private static String criticalHeldKey = "";
    private static String criticalArmorKey = "";

    private LClientNotifications() {
    }

    public enum Severity {
        INFO(0xFF72C5FF),
        SUCCESS(0xFF78D98A),
        WARNING(0xFFFFB86B),
        ERROR(0xFFFF7A7A);

        final int accent;

        Severity(int accent) {
            this.accent = accent;
        }
    }

    public record HistoryEntry(long timestamp, Severity severity, String title, String message) {
    }

    private static final class Notice {
        private final String key;
        private final Severity severity;
        private final String title;
        private final String message;
        private final long createdAt;
        private final long expiresAt;

        private Notice(String key, Severity severity, String title, String message, long createdAt, long expiresAt) {
            this.key = key;
            this.severity = severity;
            this.title = title;
            this.message = message;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    public static void info(String key, String title, String message) {
        push(key, Severity.INFO, title, message);
    }

    public static void success(String key, String title, String message) {
        push(key, Severity.SUCCESS, title, message);
    }

    public static void warning(String key, String title, String message) {
        push(key, Severity.WARNING, title, message);
    }

    public static void error(String key, String title, String message) {
        push(key, Severity.ERROR, title, message);
    }

    public static synchronized void push(String key, Severity severity, String title, String message) {
        LClientConfig config = LClientConfig.get();
        if (!config.notifications) return;

        String safeKey = sanitize(key, 48, "notice");
        String safeTitle = sanitize(title, 72, "Lclient");
        String safeMessage = sanitize(message, 180, "");
        long now = System.currentTimeMillis();
        long duration = Math.max(2, config.notificationDurationSeconds) * 1000L;

        Notice previousSameKey = null;
        for (Iterator<Notice> iterator = active.iterator(); iterator.hasNext(); ) {
            Notice notice = iterator.next();
            if (notice.expiresAt <= now) {
                iterator.remove();
                continue;
            }
            if (notice.key.equals(safeKey)) {
                previousSameKey = notice;
                iterator.remove();
            }
        }

        if (previousSameKey != null
                && now - previousSameKey.createdAt < DUPLICATE_WINDOW_MS
                && previousSameKey.title.equals(safeTitle)
                && previousSameKey.message.equals(safeMessage)) {
            active.addLast(new Notice(safeKey, severity, safeTitle, safeMessage,
                    previousSameKey.createdAt, now + duration));
            return;
        }

        active.addLast(new Notice(safeKey, severity, safeTitle, safeMessage, now, now + duration));
        history.addFirst(new HistoryEntry(now, severity, safeTitle, safeMessage));
        while (history.size() > HISTORY_LIMIT) history.removeLast();
    }

    public static synchronized List<HistoryEntry> historySnapshot() {
        return new ArrayList<>(history);
    }

    public static synchronized void clearHistory() {
        history.clear();
    }

    public static synchronized void clearActive() {
        active.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        if (minecraft.player == null || minecraft.level == null) {
            resetObservedState();
            return;
        }

        if (!initializedStates) {
            lastQuickMessages = config.quickMessages;
            lastLootEsp = config.lootEsp;
            lastSmartOffhand = config.smartOffhand;
            lastRecon = config.recon;
            lastJourneyMap = config.journeyMap;
            initializedStates = true;
        } else {
            notifyToggle("copyl-toggle", "CopyL", lastQuickMessages, config.quickMessages);
            notifyToggle("loot-toggle", "Loot ESP", lastLootEsp, config.lootEsp);
            notifyToggle("offhand-toggle", "Smart Offhand", lastSmartOffhand, config.smartOffhand);
            notifyToggle("recon-toggle", "Advanced Recon", lastRecon, config.recon);
            notifyToggle("journeymap-toggle", "JourneyMap+", lastJourneyMap, config.journeyMap);

            lastQuickMessages = config.quickMessages;
            lastLootEsp = config.lootEsp;
            lastSmartOffhand = config.smartOffhand;
            lastRecon = config.recon;
            lastJourneyMap = config.journeyMap;
        }

        String reconStatus = ReconController.getStatusText();
        if (reconStatus == null) reconStatus = "";
        if (!reconStatus.isBlank() && !reconStatus.equals(lastReconStatus)) {
            push("recon-status", classifyReconStatus(reconStatus), "Recon", reconStatus);
        }
        lastReconStatus = reconStatus;

        if (config.notifications && config.notificationGameplayAlerts) {
            observeGameplayAlerts(minecraft);
        } else {
            healthCritical = false;
            inventoryFull = false;
            criticalHeldKey = "";
            criticalArmorKey = "";
        }
    }

    private static void observeGameplayAlerts(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || !player.isAlive() || player.isSpectator()) return;

        float health = safeHealth(player);
        float maxHealth = safeMaxHealth(player);
        boolean criticalNow = Float.isFinite(health)
                && Float.isFinite(maxHealth)
                && maxHealth > 0.0F
                && health > 0.0F
                && health / maxHealth <= 0.25F;
        if (criticalNow && !healthCritical) {
            error("critical-health", "Vida crítica",
                    Math.round(health) + " / " + Math.round(maxHealth) + " HP");
        }
        healthCritical = criticalNow;

        boolean fullNow = safeInventoryFull(player);
        if (fullNow && !inventoryFull) {
            warning("inventory-full", "Inventario lleno", "No quedan slots libres en el inventario principal");
        }
        inventoryFull = fullNow;

        ItemStack held = safeMainHand(player);
        String heldKey = criticalDurabilityKey(held);
        if (!heldKey.isBlank() && !heldKey.equals(criticalHeldKey)) {
            warning("held-durability", "Durabilidad crítica",
                    safeItemName(held) + " · " + remainingPercent(held) + "%");
        }
        criticalHeldKey = heldKey;

        ItemStack worstArmor = worstCriticalArmor(player);
        String armorKey = criticalDurabilityKey(worstArmor);
        if (!armorKey.isBlank() && !armorKey.equals(criticalArmorKey)) {
            warning("armor-durability", "Armadura crítica",
                    safeItemName(worstArmor) + " · " + remainingPercent(worstArmor) + "%");
        }
        criticalArmorKey = armorKey;
    }

    private static float safeHealth(net.minecraft.world.entity.player.Player player) {
        try {
            return player.getHealth();
        } catch (RuntimeException | LinkageError ignored) {
            return Float.NaN;
        }
    }

    private static float safeMaxHealth(net.minecraft.world.entity.player.Player player) {
        try {
            return player.getMaxHealth();
        } catch (RuntimeException | LinkageError ignored) {
            return Float.NaN;
        }
    }

    private static boolean safeInventoryFull(net.minecraft.world.entity.player.Player player) {
        try {
            return player.getInventory().getFreeSlot() < 0;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static ItemStack safeMainHand(net.minecraft.world.entity.player.Player player) {
        try {
            return player.getMainHandItem();
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack worstCriticalArmor(net.minecraft.world.entity.player.Player player) {
        ItemStack worst = ItemStack.EMPTY;
        int worstPercent = 101;
        try {
            for (ItemStack stack : player.getArmorSlots()) {
                int percent = remainingPercent(stack);
                if (percent >= 0 && percent <= 10 && percent < worstPercent) {
                    worst = stack;
                    worstPercent = percent;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
        return worst;
    }

    private static String criticalDurabilityKey(ItemStack stack) {
        int percent = remainingPercent(stack);
        if (percent < 0 || percent > 10) return "";
        try {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return (id == null ? "item" : id.toString()) + ':' + stack.getDamageValue() + ':' + stack.getMaxDamage();
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    private static int remainingPercent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        try {
            if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return -1;
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            return Math.max(0, Math.min(100, Math.round(remaining * 100.0F / stack.getMaxDamage())));
        } catch (RuntimeException | LinkageError ignored) {
            return -1;
        }
    }

    private static String safeItemName(ItemStack stack) {
        try {
            String name = stack.getHoverName().getString();
            if (name != null && !name.isBlank()) return name;
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id == null ? "item" : id.getPath();
        } catch (RuntimeException | LinkageError ignored) {
            return "item";
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearActive();
        resetObservedState();
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        if (!config.notifications
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui
                || minecraft.screen != null) {
            return;
        }

        List<Notice> visible = collectVisible(config.notificationMaxVisible);
        if (visible.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int maxWidth = Math.min(270, Math.max(100, screenW - 24));
        int xRight = screenW - 8;
        int y = screenH - 40;
        long now = System.currentTimeMillis();

        for (int i = visible.size() - 1; i >= 0; i--) {
            Notice notice = visible.get(i);
            int width = Math.min(maxWidth, Math.max(100,
                    Math.max(minecraft.font.width(notice.title), minecraft.font.width(notice.message)) + 22));
            int height = notice.message.isBlank() ? 23 : 34;
            y -= height + 5;
            if (y < 8) break;

            long remaining = Math.max(0L, notice.expiresAt - now);
            long fadeWindow = Math.min(450L, Math.max(180L, (notice.expiresAt - notice.createdAt) / 4L));
            float fade = remaining < fadeWindow ? (float) remaining / (float) fadeWindow : 1.0F;
            int alpha = Math.max(35, Math.min(230, Math.round(220.0F * fade)));
            int bg = (alpha << 24) | 0x0B1118;
            int accent = (Math.max(60, Math.min(255, Math.round(255.0F * fade))) << 24)
                    | (notice.severity.accent & 0x00FFFFFF);

            int left = xRight - width;
            graphics.fill(left, y, xRight, y + height, bg);
            graphics.fill(left, y, left + 2, y + height, accent);
            graphics.fill(left + 2, y, xRight, y + 1, (Math.max(30, alpha / 2) << 24) | 0x6A8194);

            graphics.drawString(minecraft.font,
                    minecraft.font.plainSubstrByWidth(notice.title, Math.max(20, width - 14)),
                    left + 8, y + 6, 0xFFF2F6FA, false);
            if (!notice.message.isBlank()) {
                graphics.drawString(minecraft.font,
                        minecraft.font.plainSubstrByWidth(notice.message, Math.max(20, width - 14)),
                        left + 8, y + 19, 0xFFAAB8C5, false);
            }
        }
    }

    private static synchronized List<Notice> collectVisible(int maxVisible) {
        long now = System.currentTimeMillis();
        for (Iterator<Notice> iterator = active.iterator(); iterator.hasNext(); ) {
            if (iterator.next().expiresAt <= now) iterator.remove();
        }

        int limit = Math.max(1, Math.min(5, maxVisible));
        List<Notice> result = new ArrayList<>(Math.min(limit, active.size()));
        Iterator<Notice> iterator = active.descendingIterator();
        while (iterator.hasNext() && result.size() < limit) result.add(iterator.next());
        return result;
    }

    private static void notifyToggle(String key, String module, boolean before, boolean after) {
        if (before == after) return;
        if (after) success(key, module, "Activado");
        else info(key, module, "Desactivado");
    }

    private static Severity classifyReconStatus(String status) {
        String lower = status.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("waypoint")) return Severity.SUCCESS;
        if (lower.contains("error") || lower.contains("rechaz") || lower.contains("fall")) return Severity.ERROR;
        if (lower.contains("no ") || lower.contains("desactiv") || lower.contains("disponible")) return Severity.WARNING;
        return Severity.INFO;
    }

    private static void resetObservedState() {
        initializedStates = false;
        lastReconStatus = "";
        healthCritical = false;
        inventoryFull = false;
        criticalHeldKey = "";
        criticalArmorKey = "";
    }

    private static String sanitize(String value, int maxLength, String fallback) {
        String safe = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        if (safe.isBlank()) safe = fallback;
        return truncateUtf16Safely(safe, maxLength);
    }

    private static String truncateUtf16Safely(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        int end = maxChars;
        if (end > 0
                && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }
}
