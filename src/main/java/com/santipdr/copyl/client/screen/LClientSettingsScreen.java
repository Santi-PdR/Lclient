package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.LClientNotifications;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class LClientSettingsScreen extends Screen {
    private final Screen parent;
    private Button keyButton;
    private Button notificationsButton;
    private Button durationButton;
    private Button visibleButton;
    private boolean capturing;
    private String warning = "";
    private long warningUntil;

    public LClientSettingsScreen(Screen parent) {
        super(Component.literal("Lclient — Configuración"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        boolean compact = height < 270;
        int buttonWidth = Math.min(250, Math.max(120, width - 24));
        int top = compact ? 51 : height / 2 - 66;
        int buttonHeight = compact ? 18 : 20;
        int step = buttonHeight + (compact ? 4 : 7);
        int left = cx - buttonWidth / 2;

        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            capturing = true;
            warning = "";
            b.setMessage(Component.literal("PULSA UNA TECLA"));
        }).bounds(left, top, buttonWidth, buttonHeight).build());

        notificationsButton = addRenderableWidget(Button.builder(notificationsLabel(), b -> {
            LClientConfig config = LClientConfig.get();
            config.notifications = !config.notifications;
            config.save();
            if (!config.notifications) LClientNotifications.clearActive();
            refreshLabels();
        }).bounds(left, top + step, buttonWidth, buttonHeight).build());

        durationButton = addRenderableWidget(Button.builder(durationLabel(), b -> {
            LClientConfig config = LClientConfig.get();
            config.notificationDurationSeconds = cycle(config.notificationDurationSeconds, 2, 3, 4, 5, 7, 10);
            config.save();
            refreshLabels();
        }).bounds(left, top + step * 2, buttonWidth, buttonHeight).build());

        visibleButton = addRenderableWidget(Button.builder(visibleLabel(), b -> {
            LClientConfig config = LClientConfig.get();
            config.notificationMaxVisible = cycle(config.notificationMaxVisible, 1, 2, 3, 4, 5);
            config.save();
            refreshLabels();
        }).bounds(left, top + step * 3, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.literal("Historial de avisos"), b -> {
                    if (minecraft != null) minecraft.setScreen(new NotificationHistoryScreen(this));
                })
                .bounds(left, top + step * 4, buttonWidth, buttonHeight).build());

        int gap = 8;
        int half = Math.max(54, (buttonWidth - gap) / 2);
        int actionsY = top + step * 5 + (compact ? 1 : 3);
        addRenderableWidget(Button.builder(Component.literal("Abrir ruleta"), b -> {
                    if (minecraft != null) minecraft.setScreen(new LClientWheelScreen(this));
                })
                .bounds(left, actionsY, half, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(left + half + gap, actionsY, Math.max(1, buttonWidth - half - gap), buttonHeight).build());
    }

    private Component keyLabel() {
        int key = LClientConfig.get().wheelKey;
        String name = key < 0 ? "Sin asignar" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        return Component.literal("Tecla de la ruleta: " + name);
    }

    private Component notificationsLabel() {
        return Component.literal("Centro de notificaciones: " + (LClientConfig.get().notifications ? "ACTIVADO" : "DESACTIVADO"));
    }

    private Component durationLabel() {
        return Component.literal("Duración de avisos: " + LClientConfig.get().notificationDurationSeconds + " s");
    }

    private Component visibleLabel() {
        return Component.literal("Avisos simultáneos: " + LClientConfig.get().notificationMaxVisible);
    }

    private void refreshLabels() {
        if (keyButton != null && !capturing) keyButton.setMessage(keyLabel());
        if (notificationsButton != null) notificationsButton.setMessage(notificationsLabel());
        if (durationButton != null) durationButton.setMessage(durationLabel());
        if (visibleButton != null) visibleButton.setMessage(visibleLabel());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = false;
                keyButton.setMessage(keyLabel());
                return true;
            }

            int newKey = (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) ? -1 : keyCode;
            if (newKey >= 0 && conflictsWithModule(newKey)) {
                warning = "Esa tecla ya está usada por Loot ESP o Recon.";
                warningUntil = System.currentTimeMillis() + 3200L;
                capturing = false;
                keyButton.setMessage(keyLabel());
                return true;
            }

            if (newKey >= 0) clearCopyLConflict(newKey);
            LClientConfig config = LClientConfig.get();
            config.wheelKey = newKey;
            config.save();
            capturing = false;
            keyButton.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean conflictsWithModule(int keyCode) {
        LClientConfig c = LClientConfig.get();
        return keyCode == c.lootEspToggleKey
                || keyCode == c.reconZoomKey
                || keyCode == c.reconWaypointKey;
    }

    private void clearCopyLConflict(int keyCode) {
        MessageConfig messages = MessageConfig.getInstance();
        boolean changed = false;
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (messages.getKeyCode(i) == keyCode) {
                messages.setKeyCode(i, -1);
                changed = true;
            }
        }
        if (changed) messages.save();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int cx = width / 2;
        boolean compact = height < 270;
        int panelW = Math.min(460, Math.max(120, width - 20));
        int panelTop = compact ? 6 : 14;
        int panelBottom = compact ? 44 : 90;
        graphics.fill(cx - panelW / 2, panelTop, cx + panelW / 2, panelBottom, 0xB00D131A);
        graphics.fill(cx - panelW / 2, panelTop, cx + panelW / 2, panelTop + 2, 0xFF6FC2FF);

        graphics.drawCenteredString(font, title, cx, panelTop + 9, 0xFFFFFFFF);
        int maxTextWidth = Math.max(80, panelW - 14);
        if (compact) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth("Entrada global + avisos; los módulos siguen dentro de la ruleta.", maxTextWidth),
                    cx,
                    panelTop + 25,
                    0xFF9FB1C0);
        } else {
            graphics.drawCenteredString(font,
                    "La configuración global controla la entrada a Lclient y su HUD de avisos.",
                    cx,
                    panelTop + 29,
                    0xFFB1C0CD);
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth("CopyL, Loot ESP, Smart Offhand, Recon y JourneyMap+ siguen administrándose desde la ruleta.", maxTextWidth),
                    cx,
                    panelTop + 45,
                    0xFF8799AA);
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth("El historial vive sólo durante la sesión y no escribe spam adicional al disco.", maxTextWidth),
                    cx,
                    panelTop + 61,
                    0xFF718596);
        }

        if (!warning.isBlank() && System.currentTimeMillis() <= warningUntil) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(warning, Math.max(80, width - 20)),
                    cx,
                    height - 10,
                    0xFFFFB28A);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        capturing = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static int cycle(int current, int... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }
}
