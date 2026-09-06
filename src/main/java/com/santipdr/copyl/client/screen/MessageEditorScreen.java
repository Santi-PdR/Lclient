package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private final Screen parent;
    private final EditBox[] fields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final Button[] keyButtons = new Button[CopyLKeyMappings.SLOT_COUNT];
    private int bindingIndex = -1;

    public MessageEditorScreen(Screen parent) {
        super(Component.literal("Lclient — CopyL / Mensajes rápidos"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MessageConfig config = MessageConfig.getInstance();
        int contentWidth = Math.min(width - 28, 680);
        int columnWidth = (contentWidth - 12) / 2;
        int left = (width - contentWidth) / 2;
        int top = 58;
        int rowStep = 34;

        for (int i = 0; i < fields.length; i++) {
            int column = i / 5;
            int row = i % 5;
            int cardLeft = left + column * (columnWidth + 12);
            int y = top + row * rowStep;
            int keyWidth = 104;
            EditBox field = new EditBox(font, cardLeft, y, columnWidth - keyWidth - 6, 20, Component.literal("Mensaje " + (i + 1)));
            field.setMaxLength(MAX_MESSAGE_LENGTH);
            field.setValue(config.getMessage(i));
            field.setHint(Component.literal("Mensaje o /comando..."));
            fields[i] = addRenderableWidget(field);
            final int slot = i;
            keyButtons[i] = addRenderableWidget(Button.builder(keyLabel(i), b -> {
                bindingIndex = slot;
                updateKeyLabels();
            }).bounds(cardLeft + columnWidth - keyWidth, y, keyWidth, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> saveAndClose())
                .bounds(width / 2 - 108, top + rowStep * 5 + 8, 104, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> closeToParent())
                .bounds(width / 2 + 4, top + rowStep * 5 + 8, 104, 20).build());
    }

    private Component keyLabel(int slot) {
        if (bindingIndex == slot) return Component.literal("PULSA TECLA");
        int key = CopyLKeyMappings.getKeyCode(slot);
        return Component.literal(key < 0 ? "Sin tecla" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString());
    }

    private void updateKeyLabels() {
        for (int i = 0; i < keyButtons.length; i++) if (keyButtons[i] != null) keyButtons[i].setMessage(keyLabel(i));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingIndex = -1;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                CopyLKeyMappings.setKeyCode(bindingIndex, -1);
                bindingIndex = -1;
                MessageConfig.getInstance().save();
            } else {
                CopyLKeyMappings.setKeyCode(bindingIndex, keyCode);
                bindingIndex = -1;
                MessageConfig.getInstance().save();
            }
            updateKeyLabels();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "CopyL ahora es un módulo de la ruleta. Sus teclas no aparecen en Controles.", width / 2, 36, 0xFFAAB7C4);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void saveAndClose() {
        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < fields.length; i++) config.setMessage(i, fields[i].getValue());
        config.save();
        closeToParent();
    }

    @Override
    public void onClose() {
        if (bindingIndex >= 0) {
            bindingIndex = -1;
            updateKeyLabels();
            return;
        }
        saveAndClose();
    }

    private void closeToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
