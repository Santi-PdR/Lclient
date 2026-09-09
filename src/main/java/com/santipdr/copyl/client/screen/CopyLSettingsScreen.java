package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLConfig;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Minimal Mods > CopyL > Config surface. */
public final class CopyLSettingsScreen extends Screen {
    private final Screen parent;
    private Button keyButton;
    private boolean capturing;
    private String feedback = "";
    private long feedbackUntil;

    public CopyLSettingsScreen(Screen parent) {
        super(Component.literal("CopyL — Configuración"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int buttonW = Math.min(280, Math.max(130, width - 24));
        int left = cx - buttonW / 2;
        int h = height < 210 ? 18 : 20;
        int step = h + 7;
        int top = Math.max(48, height / 2 - 46);

        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            capturing = true;
            feedback = "";
            b.setMessage(Component.literal("PULSA UNA TECLA"));
        }).bounds(left, top, buttonW, h).build());

        addRenderableWidget(Button.builder(Component.literal("Editar mensajes CopyL"), b -> {
            if (minecraft != null) minecraft.setScreen(new MessageEditorScreen(this));
        }).bounds(left, top + step, buttonW, h).build());

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(left, top + step * 2, buttonW, h).build());
    }

    private Component keyLabel() {
        int key = CopyLConfig.get().openKey;
        String name = key < 0
                ? "Sin asignar"
                : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        return Component.literal("Tecla para abrir CopyL: " + name);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturing) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            capturing = false;
            keyButton.setMessage(keyLabel());
            return true;
        }

        int newKey = (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)
                ? -1
                : keyCode;

        if (newKey >= 0) clearMessageConflict(newKey);
        CopyLConfig config = CopyLConfig.get();
        config.openKey = newKey;
        config.save();
        capturing = false;
        keyButton.setMessage(keyLabel());
        feedback = newKey < 0 ? "Tecla de apertura eliminada." : "Tecla actualizada.";
        feedbackUntil = System.currentTimeMillis() + 2200L;
        return true;
    }

    private static void clearMessageConflict(int keyCode) {
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
        graphics.drawCenteredString(font, title, cx, 18, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth("CopyL es ahora la única función del mod. Sus teclas no aparecen en Opciones > Controles.", Math.max(100, width - 24)),
                cx,
                34,
                0xFFA8B8C6);

        if (!feedback.isBlank() && System.currentTimeMillis() <= feedbackUntil) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(feedback, Math.max(100, width - 24)),
                    cx,
                    height - 16,
                    0xFF8FD6FF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        capturing = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
