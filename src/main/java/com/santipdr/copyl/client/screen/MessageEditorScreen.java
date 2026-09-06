package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int ACCENT = 0xFF6FB7FF;
    private static final int ACCENT_SOFT = 0xFF9DD0FF;
    private static final int PANEL = 0xB0121720;
    private static final int CARD = 0xC019202B;
    private static final int CARD_HOVER = 0xE0222D3B;
    private static final int CARD_BORDER = 0x804B607A;
    private static final int CARD_BORDER_HOVER = 0xD06FB7FF;
    private static final int TEXT_PRIMARY = 0xFFF2F5F8;
    private static final int TEXT_SECONDARY = 0xFFAAB5C2;
    private static final int TEXT_MUTED = 0xFF7F8B99;
    private static final int KEY_BUTTON_WIDTH = 92;

    private final Screen parent;
    private final EditBox[] fields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final Button[] keyButtons = new Button[CopyLKeyMappings.SLOT_COUNT];
    private final float[] hoverProgress = new float[CopyLKeyMappings.SLOT_COUNT];
    private final boolean[] wasHovering = new boolean[CopyLKeyMappings.SLOT_COUNT];

    private int contentLeft;
    private int contentWidth;
    private int columnWidth;
    private int top;
    private int rowStep;
    private int bindingIndex = -1;
    private long openedAtMs;
    private long lastFrameMs;

    public MessageEditorScreen(Screen parent) {
        super(Component.translatable("screen.copyl.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MessageConfig config = MessageConfig.getInstance();

        openedAtMs = Util.getMillis();
        lastFrameMs = openedAtMs;
        bindingIndex = -1;

        contentWidth = Math.min(this.width - 28, 650);
        int gap = 12;
        columnWidth = (contentWidth - gap) / 2;
        contentLeft = (this.width - contentWidth) / 2;
        top = 62;

        int footerSpace = 58;
        int usableHeight = Math.max(155, this.height - top - footerSpace);
        rowStep = Math.max(29, Math.min(34, usableHeight / 5));

        for (int i = 0; i < fields.length; i++) {
            int column = i / 5;
            int row = i % 5;
            int cardLeft = contentLeft + column * (columnWidth + gap);
            int fieldY = top + row * rowStep + 12;
            int keyX = cardLeft + columnWidth - KEY_BUTTON_WIDTH - 6;
            int fieldWidth = Math.max(90, keyX - (cardLeft + 6) - 6);

            EditBox field = new EditBox(
                    this.font,
                    cardLeft + 6,
                    fieldY,
                    fieldWidth,
                    18,
                    Component.translatable("screen.copyl.slot", i + 1)
            );
            field.setMaxLength(MAX_MESSAGE_LENGTH);
            field.setValue(config.getMessage(i));
            field.setHint(Component.translatable("screen.copyl.placeholder"));
            fields[i] = addRenderableWidget(field);

            final int slot = i;
            Button keyButton = Button.builder(getKeyButtonLabel(i), button -> beginKeyCapture(slot))
                    .bounds(keyX, fieldY, KEY_BUTTON_WIDTH, 18)
                    .build();
            keyButton.setTooltip(Tooltip.create(Component.translatable("screen.copyl.key.tooltip")));
            keyButtons[i] = addRenderableWidget(keyButton);
        }

        int buttonGap = 8;
        int buttonWidth = Math.min(138, (contentWidth - buttonGap * 2) / 3);
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

        long now = Util.getMillis();
        float deltaSeconds = Math.min(0.05F, Math.max(0.0F, (now - lastFrameMs) / 1000.0F));
        lastFrameMs = now;

        float openProgress = Mth.clamp((now - openedAtMs) / 360.0F, 0.0F, 1.0F);
        openProgress = 1.0F - (float) Math.pow(1.0F - openProgress, 3.0F);

        int panelTop = 8;
        int panelBottom = Math.min(this.height - 8, top + rowStep * 5 + 38);
        guiGraphics.fill(contentLeft - 8, panelTop, contentLeft + contentWidth + 8, panelBottom, PANEL);

        int accentWidth = Math.round((contentWidth + 16) * openProgress);
        guiGraphics.fill(contentLeft - 8, panelTop, contentLeft - 8 + accentWidth, panelTop + 2, ACCENT);

        guiGraphics.drawString(this.font, this.title, contentLeft, 14, TEXT_PRIMARY, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.copyl.subtitle"),
                contentLeft,
                28,
                TEXT_SECONDARY,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.copyl.controls_hint"),
                contentLeft,
                41,
                TEXT_MUTED,
                false
        );

        if (fields[0] != null) {
            for (int i = 0; i < fields.length; i++) {
                renderSlot(guiGraphics, i, mouseX, mouseY, deltaSeconds, now);
            }
        }

        if (bindingIndex >= 0) {
            Component capture = Component.translatable("screen.copyl.key.capture", bindingIndex + 1);
            guiGraphics.drawCenteredString(this.font, capture, this.width / 2, panelBottom - 13, ACCENT_SOFT);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderSlot(GuiGraphics guiGraphics, int index, int mouseX, int mouseY, float deltaSeconds, long now) {
        EditBox field = fields[index];
        int cardLeft = field.getX() - 6;
        int cardTop = field.getY() - 12;
        int cardRight = cardLeft + columnWidth;
        int cardBottom = field.getY() + field.getHeight() + 4;

        boolean hovered = mouseX >= cardLeft && mouseX < cardRight && mouseY >= cardTop && mouseY < cardBottom;
        boolean active = hovered || field.isFocused() || bindingIndex == index;
        float target = active ? 1.0F : 0.0F;
        float speed = Math.min(1.0F, deltaSeconds * 12.0F);
        hoverProgress[index] += (target - hoverProgress[index]) * speed;

        if (hovered && !wasHovering[index] && now - openedAtMs > 250L) {
            playUiSound(1.65F);
        }
        wasHovering[index] = hovered;

        int border = mixColor(CARD_BORDER, CARD_BORDER_HOVER, hoverProgress[index]);
        int card = mixColor(CARD, CARD_HOVER, hoverProgress[index]);

        guiGraphics.fill(cardLeft, cardTop, cardRight, cardBottom, border);
        guiGraphics.fill(cardLeft + 1, cardTop + 1, cardRight - 1, cardBottom - 1, card);

        Component slotLabel = Component.translatable("screen.copyl.slot", index + 1);
        guiGraphics.drawString(this.font, slotLabel, cardLeft + 6, cardTop + 3, TEXT_SECONDARY, false);

        if (bindingIndex == index) {
            int markerRight = cardRight - 5;
            guiGraphics.fill(markerRight - 3, cardTop + 3, markerRight, cardBottom - 3, ACCENT);
        }
    }

    private void beginKeyCapture(int index) {
        bindingIndex = bindingIndex == index ? -1 : index;
        updateKeyButtonLabels();
        playUiSound(bindingIndex >= 0 ? 1.20F : 0.90F);
    }

    private Component getKeyButtonLabel(int index) {
        if (bindingIndex == index) {
            return Component.translatable("screen.copyl.key.waiting");
        }
        return CopyLKeyMappings.SLOTS[index].getTranslatedKeyMessage();
    }

    private void updateKeyButtonLabels() {
        for (int i = 0; i < keyButtons.length; i++) {
            if (keyButtons[i] != null) {
                keyButtons[i].setMessage(getKeyButtonLabel(i));
            }
        }
    }

    private void assignKey(InputConstants.Key key) {
        if (bindingIndex < 0 || this.minecraft == null) {
            return;
        }

        CopyLKeyMappings.SLOTS[bindingIndex].setKey(key);
        KeyMapping.resetMapping();
        this.minecraft.options.save();
        bindingIndex = -1;
        updateKeyButtonLabels();
        playUiSound(1.45F);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingIndex = -1;
                updateKeyButtonLabels();
                playUiSound(0.85F);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                assignKey(InputConstants.UNKNOWN);
                return true;
            }

            assignKey(InputConstants.getKey(keyCode, scanCode));
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindingIndex >= 0) {
            assignKey(InputConstants.Type.MOUSE.getOrCreate(button));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (bindingIndex >= 0) {
            bindingIndex = -1;
            updateKeyButtonLabels();
            return;
        }
        saveAndClose();
    }

    private void openControls() {
        if (this.minecraft != null) {
            saveMessages();
            this.minecraft.setScreen(new ControlsScreen(this, this.minecraft.options));
        }
    }

    private void saveAndClose() {
        saveMessages();
        closeToParent();
    }

    private void saveMessages() {
        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null) {
                config.setMessage(i, fields[i].getValue());
            }
        }
        config.save();
    }

    private void closeWithoutSaving() {
        closeToParent();
    }

    private void closeToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void playUiSound(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    private static int mixColor(int from, int to, float amount) {
        amount = Mth.clamp(amount, 0.0F, 1.0F);

        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * amount);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * amount);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * amount);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
