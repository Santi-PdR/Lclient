package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int MAX_NAME_LENGTH = 24;

    private final Screen parent;
    private final EditBox[] nameFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final EditBox[] messageFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final Button[] keyButtons = new Button[CopyLKeyMappings.SLOT_COUNT];
    private int bindingIndex = -1;
    private String warning = "";
    private long warningUntil;

    public MessageEditorScreen(Screen parent) {
        super(Component.literal("Lclient — CopyL"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MessageConfig config = MessageConfig.getInstance();
        int contentWidth = Math.min(width - 24, 760);
        int columnWidth = (contentWidth - 12) / 2;
        int left = (width - contentWidth) / 2;
        int top = 68;
        int rowStep = 34;

        for (int i = 0; i < messageFields.length; i++) {
            int column = i / 5;
            int row = i % 5;
            int cardLeft = left + column * (columnWidth + 12);
            int y = top + row * rowStep;
            int keyWidth = 86;
            int nameWidth = Math.min(92, Math.max(70, columnWidth / 4));
            int messageWidth = Math.max(90, columnWidth - nameWidth - keyWidth - 10);

            EditBox name = new EditBox(font, cardLeft, y, nameWidth, 20, Component.literal("Nombre " + (i + 1)));
            name.setMaxLength(MAX_NAME_LENGTH);
            name.setValue(config.getName(i));
            name.setHint(Component.literal("Nombre"));
            nameFields[i] = addRenderableWidget(name);

            EditBox message = new EditBox(font, cardLeft + nameWidth + 5, y, messageWidth, 20,
                    Component.literal("Mensaje " + (i + 1)));
            message.setMaxLength(MAX_MESSAGE_LENGTH);
            message.setValue(config.getMessage(i));
            message.setHint(Component.literal("Mensaje o /comando..."));
            messageFields[i] = addRenderableWidget(message);

            final int slot = i;
            keyButtons[i] = addRenderableWidget(Button.builder(keyLabel(i), b -> {
                bindingIndex = slot;
                warning = "";
                updateKeyLabels();
            }).bounds(cardLeft + columnWidth - keyWidth, y, keyWidth, 20).build());
        }

        int bottom = top + rowStep * 5 + 7;
        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> saveAndClose())
                .bounds(width / 2 - 108, bottom, 104, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> closeToParent())
                .bounds(width / 2 + 4, bottom, 104, 20).build());
    }

    private Component keyLabel(int slot) {
        if (bindingIndex == slot) return Component.literal("PULSA TECLA");
        int key = CopyLKeyMappings.getKeyCode(slot);
        return Component.literal(key < 0 ? "Sin tecla" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString());
    }

    private void updateKeyLabels() {
        for (int i = 0; i < keyButtons.length; i++) {
            if (keyButtons[i] != null) keyButtons[i].setMessage(keyLabel(i));
        }
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
            } else if (isReservedKey(keyCode)) {
                showWarning("Esa tecla está reservada por Lclient (ruleta, Loot ESP o Recon).");
                bindingIndex = -1;
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

    private boolean isReservedKey(int keyCode) {
        LClientConfig c = LClientConfig.get();
        return keyCode == c.wheelKey
                || keyCode == c.lootEspToggleKey
                || keyCode == c.reconZoomKey
                || keyCode == c.reconWaypointKey;
    }

    private void showWarning(String text) {
        warning = text;
        warningUntil = System.currentTimeMillis() + 3500L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                "10 slots con nombre + tecla única. Un atajo reasignado se elimina automáticamente del slot anterior.",
                width / 2,
                34,
                0xFFAAB7C4);
        graphics.drawCenteredString(font,
                "Variables: {pos} {x} {y} {z} {dim} {hp} {food} {name}",
                width / 2,
                48,
                0xFF7F93A6);

        if (!warning.isBlank() && System.currentTimeMillis() <= warningUntil) {
            graphics.drawCenteredString(font, warning, width / 2, height - 16, 0xFFFFB28A);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void saveAndClose() {
        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < messageFields.length; i++) {
            config.setName(i, nameFields[i].getValue());
            config.setMessage(i, messageFields[i].getValue());
        }
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
