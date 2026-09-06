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
        int cy = height / 2;

        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            capturing = true;
            b.setMessage(Component.literal("PULSA UNA TECLA"));
        }).bounds(cx - 120, cy - 18, 240, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Abrir ruleta"), b -> minecraft.setScreen(new LClientWheelScreen(this)))
                .bounds(cx - 120, cy + 12, 116, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx + 4, cy + 12, 116, 20).build());
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

        int cx = width / 2;
        int panelW = Math.min(420, width - 32);
        graphics.fill(cx - panelW / 2, 18, cx + panelW / 2, 92, 0xB00D131A);
        graphics.fill(cx - panelW / 2, 18, cx + panelW / 2, 20, 0xFF6FC2FF);

        graphics.drawCenteredString(font, title, cx, 30, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "La configuración global sólo controla la entrada a Lclient.", cx, 50, 0xFFB1C0CD);
        graphics.drawCenteredString(font, "CopyL, Loot ESP, Smart Offhand, Recon y JourneyMap+ se configuran desde la ruleta.", cx, 64, 0xFF8799AA);
        graphics.drawCenteredString(font, "Lclient no añade entradas a Opciones > Controles.", cx, 78, 0xFF718596);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        capturing = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
