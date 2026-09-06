package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class LClientSettingsScreen extends Screen {
    private final Screen parent;
    private Button keyButton;
    private boolean capturing;

    public LClientSettingsScreen(Screen parent) {
        super(Component.literal("Lclient — Configuración"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            capturing = true;
            b.setMessage(Component.literal("PULSA UNA TECLA"));
        }).bounds(cx - 110, height / 2 - 10, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Abrir ruleta"), b -> minecraft.setScreen(new LClientWheelScreen(this)))
                .bounds(cx - 110, height / 2 + 18, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx + 4, height / 2 + 18, 106, 20).build());
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
            LClientConfig.get().wheelKey = newKey;
            LClientConfig.get().save();
            capturing = false;
            keyButton.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 28, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Lclient no añade entradas a Opciones > Controles.", width / 2, 48, 0xFF9FB0C1);
        graphics.drawCenteredString(font, "Todos los módulos, incluido CopyL, viven dentro de la misma ruleta.", width / 2, 61, 0xFF9FB0C1);
        graphics.drawCenteredString(font, "Backspace/Delete deja la tecla global sin asignar.", width / 2, 74, 0xFF7F91A3);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        capturing = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
