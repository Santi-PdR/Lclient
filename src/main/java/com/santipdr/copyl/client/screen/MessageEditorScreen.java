package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;

public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int ACCENT = 0xFF6FB7FF;
    private static final int PANEL = 0xB0121720;
    private static final int CARD = 0xC019202B;
    private static final int CARD_BORDER = 0x804B607A;
    private static final int TEXT_PRIMARY = 0xFFF2F5F8;
    private static final int TEXT_SECONDARY = 0xFFAAB5C2;
    private static final int TEXT_MUTED = 0xFF7F8B99;

    private final Screen parent;
    private final EditBox[] fields = new EditBox[CopyLKeyMappings.SLOT_COUNT];

    private int contentLeft;
    private int contentWidth;
    private int columnWidth;
    private int top;
    private int rowStep;

    public MessageEditorScreen(Screen parent) {
        super(Component.translatable("screen.copyl.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MessageConfig config = MessageConfig.getInstance();

        contentWidth = Math.min(this.width - 28, 620);
        int gap = 12;
        columnWidth = (contentWidth - gap) / 2;
        contentLeft = (this.width - contentWidth) / 2;
        top = 58;

        int footerSpace = 56;
        int usableHeight = Math.max(145, this.height - top - footerSpace);
        rowStep = Math.max(27, Math.min(32, usableHeight / 5));

        for (int i = 0; i < fields.length; i++) {
            int column = i / 5;
            int row = i % 5;
            int x = contentLeft + column * (columnWidth + gap);
            int y = top + row * rowStep + 11;

            EditBox field = new EditBox(
                    this.font,
                    x + 6,
                    y,
                    columnWidth - 12,
                    18,
                    Component.translatable("screen.copyl.slot", i + 1)
            );
            field.setMaxLength(MAX_MESSAGE_LENGTH);
            field.setValue(config.getMessage(i));
            field.setHint(Component.translatable("screen.copyl.placeholder"));
            fields[i] = addRenderableWidget(field);
        }

        int buttonGap = 8;
        int buttonWidth = Math.min(130, (contentWidth - buttonGap * 2) / 3);
        int totalButtonsWidth = buttonWidth * 3 + buttonGap * 2;
        int buttonLeft = this.width / 2 - totalButtonsWidth / 2;
        int buttonY = Math.min(this.height - 26, top + rowStep * 5 + 8);

        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.save"), button -> saveAndClose())
                .bounds(buttonLeft, buttonY, buttonWidth, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.controls"), button -> openControls())
                .bounds(buttonLeft + buttonWidth + buttonGap, buttonY, buttonWidth, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> closeWithoutSaving())
                .bounds(buttonLeft + (buttonWidth + buttonGap) * 2, buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int panelTop = 8;
        int panelBottom = Math.min(this.height - 8, top + rowStep * 5 + 36);
        guiGraphics.fill(contentLeft - 8, panelTop, contentLeft + contentWidth + 8, panelBottom, PANEL);
        guiGraphics.fill(contentLeft - 8, panelTop, contentLeft + contentWidth + 8, panelTop + 2, ACCENT);

        guiGraphics.drawString(this.font, this.title, contentLeft, 14, TEXT_PRIMARY, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.copyl.subtitle"),
                contentLeft,
                27,
                TEXT_SECONDARY,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.copyl.controls_hint"),
                contentLeft,
                39,
                TEXT_MUTED,
                false
        );

        if (fields[0] != null) {
            for (int i = 0; i < fields.length; i++) {
                renderSlot(guiGraphics, i);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderSlot(GuiGraphics guiGraphics, int index) {
        EditBox field = fields[index];
        int cardLeft = field.getX() - 6;
        int cardTop = field.getY() - 11;
        int cardRight = cardLeft + columnWidth;
        int cardBottom = field.getY() + field.getHeight() + 3;

        guiGraphics.fill(cardLeft, cardTop, cardRight, cardBottom, CARD_BORDER);
        guiGraphics.fill(cardLeft + 1, cardTop + 1, cardRight - 1, cardBottom - 1, CARD);

        Component slotLabel = Component.translatable("screen.copyl.slot", index + 1);
        guiGraphics.drawString(this.font, slotLabel, cardLeft + 6, cardTop + 3, TEXT_SECONDARY, false);

        Component keyMessage = CopyLKeyMappings.SLOTS[index].getTranslatedKeyMessage();
        String keyText = keyMessage.getString();
        int keyWidth = this.font.width(keyText) + 8;
        int keyLeft = cardRight - keyWidth - 5;
        int keyTop = cardTop + 1;

        guiGraphics.fill(keyLeft, keyTop, cardRight - 4, keyTop + 10, 0xAA0D1118);
        guiGraphics.drawString(this.font, keyMessage, keyLeft + 4, keyTop + 1, ACCENT, false);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    private void openControls() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ControlsScreen(this, this.minecraft.options));
        }
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
