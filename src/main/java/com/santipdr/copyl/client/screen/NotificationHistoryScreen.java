package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientNotifications;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Session-only history for Lclient's non-chat notification center. */
public final class NotificationHistoryScreen extends Screen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Screen parent;
    private int page;

    public NotificationHistoryScreen(Screen parent) {
        super(Component.literal("Lclient — Historial de avisos"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<LClientNotifications.HistoryEntry> entries = LClientNotifications.historySnapshot();
        int pageSize = pageSize();
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        page = Math.max(0, Math.min(page, maxPage));

        int cx = width / 2;
        int buttonWidth = Math.min(300, Math.max(140, width - 24));
        int left = cx - buttonWidth / 2;
        int y = height - 27;
        int gap = 6;
        int nav = 34;
        int middle = Math.max(60, (buttonWidth - nav * 2 - gap * 2) / 2);
        int clearWidth = Math.max(1, buttonWidth - nav * 2 - middle - gap * 3);

        Button previous = addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (page > 0) {
                page--;
                rebuildWidgets();
            }
        }).bounds(left, y, nav, 20).build());
        previous.active = page > 0;

        addRenderableWidget(Button.builder(Component.literal("Limpiar"), b -> {
            LClientNotifications.clearHistory();
            page = 0;
            rebuildWidgets();
        }).bounds(left + nav + gap, y, clearWidth, 20).build()).active = !entries.isEmpty();

        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(left + nav + gap + clearWidth + gap, y, middle, 20).build());

        Button next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (page < maxPage) {
                page++;
                rebuildWidgets();
            }
        }).bounds(left + buttonWidth - nav, y, nav, 20).build());
        next.active = page < maxPage;
    }

    private int pageSize() {
        return Math.max(3, Math.min(10, (height - 74) / 28));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        List<LClientNotifications.HistoryEntry> entries = LClientNotifications.historySnapshot();
        int pageSize = pageSize();
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        page = Math.max(0, Math.min(page, maxPage));

        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        String subtitle = entries.isEmpty()
                ? "Todavía no hay avisos en esta sesión."
                : entries.size() + " avisos · página " + (page + 1) + "/" + (maxPage + 1);
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth(subtitle, Math.max(100, width - 20)),
                width / 2,
                25,
                0xFF94A7B8);

        int cardWidth = Math.min(520, Math.max(140, width - 20));
        int left = (width - cardWidth) / 2;
        int from = page * pageSize;
        int to = Math.min(entries.size(), from + pageSize);
        int y = 43;

        for (int i = from; i < to; i++) {
            LClientNotifications.HistoryEntry entry = entries.get(i);
            int height = 23;
            graphics.fill(left, y, left + cardWidth, y + height, 0xB00C1218);
            graphics.fill(left, y, left + 2, y + height, severityColor(entry.severity()));

            String time = TIME.format(Instant.ofEpochMilli(entry.timestamp()));
            String titleLine = time + "  ·  " + entry.title();
            graphics.drawString(font,
                    font.plainSubstrByWidth(titleLine, cardWidth - 14),
                    left + 7,
                    y + 4,
                    0xFFF0F5F9,
                    false);
            if (!entry.message().isBlank()) {
                graphics.drawString(font,
                        font.plainSubstrByWidth(entry.message(), cardWidth - 14),
                        left + 7,
                        y + 13,
                        0xFF91A3B3,
                        false);
            }
            y += 28;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static int severityColor(LClientNotifications.Severity severity) {
        return switch (severity) {
            case INFO -> 0xFF72C5FF;
            case SUCCESS -> 0xFF78D98A;
            case WARNING -> 0xFFFFB86B;
            case ERROR -> 0xFFFF7A7A;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
