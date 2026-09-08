package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.HudAnchor;
import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Simple in-game editor for the three persistent Lclient HUD surfaces. */
public final class HudLayoutScreen extends Screen {
    private final Screen parent;
    private Button notificationsButton;
    private Button lootButton;
    private Button reconButton;
    private Button lootDirectionButton;
    private Button reconTelemetryButton;

    public HudLayoutScreen(Screen parent) {
        super(Component.literal("Lclient — Distribución HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        boolean tight = height < 230;
        int buttonWidth = Math.min(260, Math.max(120, width - 24));
        int buttonHeight = tight ? 17 : 20;
        int step = tight ? 19 : 24;
        int top = tight ? 35 : Math.max(48, height / 2 - 72);
        int left = cx - buttonWidth / 2;

        notificationsButton = addRenderableWidget(Button.builder(notificationsLabel(), b -> {
            LClientConfig c = LClientConfig.get();
            c.notificationHudAnchor = HudAnchor.next(c.notificationHudAnchor, HudAnchor.BOTTOM_RIGHT);
            c.save();
            refreshLabels();
        }).bounds(left, top, buttonWidth, buttonHeight).build());

        lootButton = addRenderableWidget(Button.builder(lootLabel(), b -> {
            LClientConfig c = LClientConfig.get();
            c.lootHudAnchor = HudAnchor.next(c.lootHudAnchor, HudAnchor.TOP_LEFT);
            c.save();
            refreshLabels();
        }).bounds(left, top + step, buttonWidth, buttonHeight).build());

        reconButton = addRenderableWidget(Button.builder(reconLabel(), b -> {
            LClientConfig c = LClientConfig.get();
            c.reconHudAnchor = HudAnchor.next(c.reconHudAnchor, HudAnchor.TOP_RIGHT);
            c.save();
            refreshLabels();
        }).bounds(left, top + step * 2, buttonWidth, buttonHeight).build());

        lootDirectionButton = addRenderableWidget(Button.builder(lootDirectionLabel(), b -> {
            LClientConfig c = LClientConfig.get();
            c.lootEspHudDirection = !c.lootEspHudDirection;
            c.save();
            refreshLabels();
        }).bounds(left, top + step * 3, buttonWidth, buttonHeight).build());

        reconTelemetryButton = addRenderableWidget(Button.builder(reconTelemetryLabel(), b -> {
            LClientConfig c = LClientConfig.get();
            c.reconTelemetry = !c.reconTelemetry;
            c.save();
            refreshLabels();
        }).bounds(left, top + step * 4, buttonWidth, buttonHeight).build());

        int actionY = top + step * 5 + (tight ? 1 : 5);
        int gap = 8;
        int half = Math.max(54, (buttonWidth - gap) / 2);
        addRenderableWidget(Button.builder(Component.literal("Restaurar"), b -> {
                    LClientConfig c = LClientConfig.get();
                    c.notificationHudAnchor = HudAnchor.BOTTOM_RIGHT.ordinal();
                    c.lootHudAnchor = HudAnchor.TOP_LEFT.ordinal();
                    c.reconHudAnchor = HudAnchor.TOP_RIGHT.ordinal();
                    c.lootEspHudDirection = true;
                    c.reconTelemetry = true;
                    c.save();
                    refreshLabels();
                })
                .bounds(left, actionY, half, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(left + half + gap, actionY, Math.max(1, buttonWidth - half - gap), buttonHeight).build());

        refreshLabels();
    }

    private Component notificationsLabel() {
        LClientConfig c = LClientConfig.get();
        return Component.literal("Avisos: " + HudAnchor.fromConfig(c.notificationHudAnchor, HudAnchor.BOTTOM_RIGHT).displayName());
    }

    private Component lootLabel() {
        LClientConfig c = LClientConfig.get();
        return Component.literal("Loot HUD: " + HudAnchor.fromConfig(c.lootHudAnchor, HudAnchor.TOP_LEFT).displayName());
    }

    private Component reconLabel() {
        LClientConfig c = LClientConfig.get();
        return Component.literal("Recon Panel: " + HudAnchor.fromConfig(c.reconHudAnchor, HudAnchor.TOP_RIGHT).displayName());
    }

    private Component lootDirectionLabel() {
        return Component.literal("Dirección/altura de loot: " + yesNo(LClientConfig.get().lootEspHudDirection));
    }

    private Component reconTelemetryLabel() {
        return Component.literal("Telemetría de Recon: " + yesNo(LClientConfig.get().reconTelemetry));
    }

    private void refreshLabels() {
        if (notificationsButton != null) notificationsButton.setMessage(notificationsLabel());
        if (lootButton != null) lootButton.setMessage(lootLabel());
        if (reconButton != null) reconButton.setMessage(reconLabel());
        if (lootDirectionButton != null) lootDirectionButton.setMessage(lootDirectionLabel());
        if (reconTelemetryButton != null) reconTelemetryButton.setMessage(reconTelemetryLabel());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2;
        graphics.drawCenteredString(font, title, cx, 9, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth("Cada panel conserva su esquina entre sesiones.", Math.max(80, width - 20)),
                cx,
                23,
                0xFF93A6B6);

        if (width >= 420 && height >= 250) renderPreview(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics) {
        LClientConfig c = LClientConfig.get();
        drawPreviewCard(graphics,
                HudAnchor.fromConfig(c.lootHudAnchor, HudAnchor.TOP_LEFT),
                92, 28, "LOOT", 0xFF6FC2FF);
        drawPreviewCard(graphics,
                HudAnchor.fromConfig(c.reconHudAnchor, HudAnchor.TOP_RIGHT),
                104, 34, "RECON", 0xFF72C5FF);
        drawPreviewCard(graphics,
                HudAnchor.fromConfig(c.notificationHudAnchor, HudAnchor.BOTTOM_RIGHT),
                112, 30, "AVISO", 0xFFFFB86B);
    }

    private void drawPreviewCard(GuiGraphics graphics, HudAnchor anchor, int cardW, int cardH, String text, int accent) {
        int margin = 8;
        int x = anchor.left(width, cardW, margin);
        int y = anchor.top(height, cardH, margin);
        graphics.fill(x, y, x + cardW, y + cardH, 0xA80A1016);
        graphics.fill(x, y, x + 2, y + cardH, accent);
        graphics.drawString(font, text, x + 8, y + 9, 0xFFDCE8F1, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static String yesNo(boolean value) {
        return value ? "SÍ" : "NO";
    }
}
