package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public final class NotificationCenterScreen extends Screen {
    private final Screen parent;
    private boolean combatMode;
    private int scrollOffset;
    private Button modeButton;
    private Button clearButton;

    public NotificationCenterScreen(Screen parent) {
        super(Component.literal("Centro de Lclient"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LClientHud.markCenterRead();
        int cx = width / 2;
        int bottom = height - 31;

        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> {
            combatMode = !combatMode;
            scrollOffset = 0;
            refreshLabels();
        }).bounds(cx - 206, bottom, 126, 20).build());

        clearButton = addRenderableWidget(Button.builder(clearLabel(), b -> {
            if (combatMode) LClientHud.clearCombat();
            else LClientHud.clearNotices();
            scrollOffset = 0;
            refreshLabels();
        }).bounds(cx - 74, bottom, 106, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Ajustes Combat"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new ModuleSettingsScreen(this, LClientWheelScreen.Module.COMBAT));
            }
        }).bounds(cx + 38, bottom, 106, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(cx + 150, bottom, 56, 20).build());
    }

    private Component modeLabel() {
        return Component.literal(combatMode ? "Vista: COMBATE" : "Vista: AVISOS");
    }

    private Component clearLabel() {
        return Component.literal(combatMode ? "Limpiar combate" : "Limpiar avisos");
    }

    private void refreshLabels() {
        if (modeButton != null) modeButton.setMessage(modeLabel());
        if (clearButton != null) clearButton.setMessage(clearLabel());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(620, width - 28);
        int left = (width - panelWidth) / 2;
        int right = left + panelWidth;
        int top = 20;
        int bottom = height - 43;

        graphics.fill(left, top, right, bottom, 0xE010151D);
        graphics.fill(left, top, right, top + 2, 0xFF67B7FF);
        graphics.drawCenteredString(font, "LCLIENT · CENTRO DE NOTIFICACIONES", width / 2, top + 10, 0xFFF2F6FA);

        int noticeCount = LClientHud.noticeSnapshot().size();
        int combatCount = LClientHud.combatSnapshot().size();
        String summary = noticeCount + " avisos · " + combatCount + " eventos de combate";
        graphics.drawCenteredString(font, summary, width / 2, top + 24, 0xFF8FA0B0);

        int listTop = top + 42;
        int listBottom = bottom - 8;
        if (combatMode) renderCombatList(graphics, left + 12, right - 12, listTop, listBottom);
        else renderNoticeList(graphics, left + 12, right - 12, listTop, listBottom);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderNoticeList(GuiGraphics graphics, int left, int right, int top, int bottom) {
        List<LClientHud.NoticeView> entries = LClientHud.noticeSnapshot();
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, "No hay avisos guardados.", width / 2, top + 18, 0xFF83919E);
            return;
        }

        int y = top;
        int index = 0;
        int contentWidth = Math.max(80, right - left - 18);
        for (LClientHud.NoticeView entry : entries) {
            if (index++ < scrollOffset) continue;
            String count = entry.count() > 1 ? " ×" + entry.count() : "";
            String heading = age(entry.time()) + count;
            List<FormattedCharSequence> lines = font.split(Component.literal(entry.text()), contentWidth);
            int lineCount = Math.min(2, lines.size());
            int rowHeight = 17 + lineCount * 10;
            if (y + rowHeight > bottom) break;

            graphics.fill(left, y, right, y + rowHeight - 3, 0x9A151C24);
            graphics.fill(left, y, left + 2, y + rowHeight - 3, 0xFF67B7FF);
            graphics.drawString(font, heading, left + 8, y + 5, 0xFF7F91A3, false);
            int textY = y + 15;
            for (int i = 0; i < lineCount; i++) {
                graphics.drawString(font, lines.get(i), left + 8, textY, 0xFFE4EDF5, false);
                textY += 10;
            }
            y += rowHeight;
        }
    }

    private void renderCombatList(GuiGraphics graphics, int left, int right, int top, int bottom) {
        List<LClientHud.CombatView> entries = LClientHud.combatSnapshot();
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, "No hay eventos de combate guardados.", width / 2, top + 18, 0xFF83919E);
            return;
        }

        int y = top;
        int index = 0;
        int contentWidth = Math.max(80, right - left - 18);
        for (LClientHud.CombatView entry : entries) {
            if (index++ < scrollOffset) continue;
            List<FormattedCharSequence> lines = font.split(Component.literal(entry.text()), contentWidth);
            int lineCount = Math.min(3, lines.size());
            int rowHeight = 17 + lineCount * 10;
            if (y + rowHeight > bottom) break;

            graphics.fill(left, y, right, y + rowHeight - 3, 0x9A151C24);
            graphics.fill(left, y, left + 2, y + rowHeight - 3, 0xFFE27D7D);
            graphics.drawString(font, age(entry.time()), left + 8, y + 5, 0xFF9C8585, false);
            int textY = y + 15;
            for (int i = 0; i < lineCount; i++) {
                graphics.drawString(font, lines.get(i), left + 8, textY, 0xFFFFDADA, false);
                textY += 10;
            }
            y += rowHeight;
        }
    }

    private static String age(long time) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - time) / 1000L);
        if (seconds < 60L) return "hace " + seconds + " s";
        long minutes = seconds / 60L;
        if (minutes < 60L) return "hace " + minutes + " min";
        return "hace " + (minutes / 60L) + " h";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int size = combatMode ? LClientHud.combatSnapshot().size() : LClientHud.noticeSnapshot().size();
        if (delta > 0) scrollOffset--;
        else if (delta < 0) scrollOffset++;
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, size - 1));
        return true;
    }

    @Override
    public void onClose() {
        LClientHud.markCenterRead();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
