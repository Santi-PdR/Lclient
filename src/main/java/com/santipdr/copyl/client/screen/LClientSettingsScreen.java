package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class LClientSettingsScreen extends Screen {
    private final Screen parent;
    private Button keyButton;
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
        boolean compact = height < 220;
        int buttonWidth = Math.min(240, Math.max(180, width - 24));
        int top = compact ? 66 : height / 2 - 18;
        int buttonHeight = compact ? 18 : 20;

        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            capturing = true;
            warning = "";
            b.setMessage(Component.literal("PULSA UNA TECLA"));
        }).bounds(cx - buttonWidth / 2, top, buttonWidth, buttonHeight).build());

        int gap = 8;
        int half = (buttonWidth - gap) / 2;
        addRenderableWidget(Button.builder(Component.literal("Abrir ruleta"), b -> {
                    if (minecraft != null) minecraft.setScreen(new LClientWheelScreen(this));
                })
                .bounds(cx - buttonWidth / 2, top + buttonHeight + 10, half, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx - buttonWidth / 2 + half + gap, top + buttonHeight + 10, half, buttonHeight).build());
    }

    private Component keyLabel() {
        int key = LClientConfig.get().wheelKey;
        String name = key < 0 ? "Sin asignar" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        return Component.literal("Tecla de la ruleta: " + name);
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
        boolean compact = height < 220;
        int panelW = Math.min(440, Math.max(180, width - 24));
        int panelTop = compact ? 8 : 18;
        int panelBottom = compact ? 56 : 96;
        graphics.fill(cx - panelW / 2, panelTop, cx + panelW / 2, panelBottom, 0xB00D131A);
        graphics.fill(cx - panelW / 2, panelTop, cx + panelW / 2, panelTop + 2, 0xFF6FC2FF);

        graphics.drawCenteredString(font, title, cx, panelTop + 10, 0xFFFFFFFF);
        int maxTextWidth = Math.max(160, panelW - 14);
        if (compact) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth("Configura la entrada a Lclient; el resto vive dentro de la ruleta.", maxTextWidth),
                    cx,
                    panelTop + 27,
                    0xFF9FB1C0);
        } else {
            graphics.drawCenteredString(font, "La configuración global controla la entrada a Lclient.", cx, 50, 0xFFB1C0CD);
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth("CopyL, Loot ESP, Smart Offhand, Recon y JourneyMap+ viven dentro de la ruleta.", maxTextWidth),
                    cx,
                    64,
                    0xFF8799AA);
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth("Las teclas reservadas se protegen para evitar dobles acciones accidentales.", maxTextWidth),
                    cx,
                    78,
                    0xFF718596);
        }

        if (!warning.isBlank() && System.currentTimeMillis() <= warningUntil) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(warning, Math.max(160, width - 20)),
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
}
