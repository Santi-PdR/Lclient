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

/** Minimal and polished Mods > CopyL > Config surface. */
public final class CopyLSettingsScreen extends Screen {
    private final Screen parent;
    private Button keyButton;
    private boolean capturing;
    private String feedback = "";
    private long feedbackUntil;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public CopyLSettingsScreen(Screen parent) {
        super(Component.literal("CopyL"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(420, Math.max(240, width - 28));
        panelHeight = Math.min(190, Math.max(156, height - 70));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(42, (height - panelHeight) / 2);

        int innerLeft = panelLeft + 20;
        int innerWidth = panelWidth - 40;
        int buttonHeight = 20;
        int firstY = panelTop + 72;

        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            capturing = true;
            feedback = "";
            b.setMessage(Component.literal("PULSA UNA TECLA"));
        }).bounds(innerLeft, firstY, innerWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.literal("Editar mensajes"), b -> {
            if (minecraft != null) minecraft.setScreen(new MessageEditorScreen(this));
        }).bounds(innerLeft, firstY + 29, innerWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(innerLeft, firstY + 58, innerWidth, buttonHeight).build());
    }

    private Component keyLabel() {
        int key = CopyLConfig.get().openKey;
        String name = key < 0
                ? "Sin asignar"
                : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        return Component.literal("Abrir CopyL  •  " + name);
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
        feedback = newKey < 0 ? "Tecla de apertura eliminada" : "Tecla de apertura actualizada";
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
        graphics.fill(0, 0, width, height, 0xA805080D);

        graphics.fill(panelLeft + 3, panelTop + 3,
                panelLeft + panelWidth + 3, panelTop + panelHeight + 3,
                0x66000000);
        graphics.fill(panelLeft, panelTop,
                panelLeft + panelWidth, panelTop + panelHeight,
                0xE8121921);
        graphics.fill(panelLeft, panelTop,
                panelLeft + 4, panelTop + panelHeight,
                0xFF55B9E8);
        graphics.fill(panelLeft + 4, panelTop,
                panelLeft + panelWidth, panelTop + 1,
                0x554C6C80);

        graphics.drawString(font, "COPYL", panelLeft + 20, panelTop + 16, 0xFFFFFFFF, false);
        graphics.drawString(font, "Mensajes rápidos", panelLeft + 20, panelTop + 30, 0xFF7E9EB2, false);

        int configured = 0;
        int assigned = 0;
        MessageConfig messages = MessageConfig.getInstance();
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (!messages.getMessage(i).isBlank()) configured++;
            if (messages.getKeyCode(i) >= 0) assigned++;
        }

        String summary = configured + "/10 mensajes  •  " + assigned + " atajos";
        graphics.drawString(font,
                summary,
                panelLeft + panelWidth - 20 - font.width(summary),
                panelTop + 21,
                0xFFA7D9EF,
                false);

        graphics.drawString(font,
                "Todo se configura dentro de CopyL. No agrega teclas a Opciones > Controles.",
                panelLeft + 20,
                panelTop + 49,
                0xFF9AABB8,
                false);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (!feedback.isBlank() && System.currentTimeMillis() <= feedbackUntil) {
            graphics.drawCenteredString(font,
                    feedback,
                    width / 2,
                    panelTop + panelHeight - 15,
                    0xFF86D6FA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        capturing = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
