package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;

    private final Screen parent;
    private final EditBox[] fields = new EditBox[CopyLKeyMappings.SLOT_COUNT];

    public MessageEditorScreen(Screen parent) {
        super(Component.translatable("screen.copyl.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MessageConfig config = MessageConfig.getInstance();

        int contentWidth = Math.min(this.width - 32, 540);
        int gap = 12;
        int columnWidth = (contentWidth - gap) / 2;
        int left = (this.width - contentWidth) / 2;
        int top = 38;
        int footerY = Math.max(top + 138, this.height - 28);
        int availableHeight = Math.max(130, footerY - top - 6);
        int rowStep = Math.max(26, Math.min(34, availableHeight / 5));

        for (int i = 0; i < fields.length; i++) {
            int column = i / 5;
            int row = i % 5;
            int x = left + column * (columnWidth + gap);
            int y = top + row * rowStep + 10;

            EditBox field = new EditBox(
                    this.font,
                    x,
                    y,
                    columnWidth,
                    18,
                    Component.translatable("screen.copyl.slot", i + 1)
            );
            field.setMaxLength(MAX_MESSAGE_LENGTH);
            field.setValue(config.getMessage(i));
            fields[i] = addRenderableWidget(field);
        }

        int buttonWidth = Math.min(150, (contentWidth - gap) / 2);
        int buttonY = Math.min(this.height - 24, top + rowStep * 5 + 12);
        int buttonLeft = this.width / 2 - buttonWidth - gap / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.save"), button -> saveAndClose())
                .bounds(buttonLeft, buttonY, buttonWidth, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> closeWithoutSaving())
                .bounds(this.width / 2 + gap / 2, buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.copyl.hint"),
                this.width / 2,
                24,
                0xA0A0A0
        );

        if (fields[0] != null) {
            for (int i = 0; i < fields.length; i++) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.copyl.slot", i + 1),
                        fields[i].getX(),
                        fields[i].getY() - 9,
                        0xD0D0D0
                );
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    private void saveAndClose() {
        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null) {
                config.setMessage(i, fields[i].getValue());
            }
        }
        config.save();
        closeToParent();
    }

    private void closeWithoutSaving() {
        closeToParent();
    }

    private void closeToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
