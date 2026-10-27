package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.CopyLBinding;
import com.santipdr.copyl.client.CopyLConfig;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.CopyLProfileCodec;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Polished Mods > CopyL > Config surface. */
public final class CopyLSettingsScreen extends Screen {
    private static final long IMPORT_CONFIRM_MS = 4000L;

    private final Screen parent;
    private DispatchButton bindingButton;
    private boolean capturing;
    private Component feedback = Component.empty();
    private int feedbackColor = DispatchPalette.SUCCESS;
    private long feedbackUntil;
    private long importConfirmUntil;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private boolean compactLayout;
    private int contentInset;

    public CopyLSettingsScreen(Screen parent) {
        super(Component.translatable("screen.copyl.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(440, Math.max(1, width - 16));
        panelHeight = Math.min(220, Math.max(1, height - 16));
        panelLeft = Math.max(0, (width - panelWidth) / 2);
        panelTop = Math.max(0, (height - panelHeight) / 2);
        compactLayout = panelWidth < 320 || panelHeight < 180;
        contentInset = Math.min(compactLayout ? 8 : 20, Math.max(4, (panelWidth - 112) / 2));

        int innerLeft = panelLeft + contentInset;
        int innerWidth = Math.max(1, panelWidth - contentInset * 2);
        int buttonHeight = compactLayout ? 16 : 20;
        int rowStep = buttonHeight + (compactLayout ? 4 : 5);
        int firstY = panelTop + (compactLayout ? 28 : 66);

        bindingButton = addRenderableWidget(new DispatchButton(innerLeft, firstY, innerWidth, buttonHeight,
                bindingLabel(), () -> {
                    capturing = true;
                    feedback = Component.empty();
                    bindingButton.setMessage(Component.translatable("screen.copyl.press_binding"));
                }, DispatchButton.Style.SECONDARY));

        addRenderableWidget(new DispatchButton(innerLeft, firstY + rowStep, innerWidth, buttonHeight,
                Component.translatable("screen.copyl.edit_messages"),
                () -> { if (minecraft != null) minecraft.setScreen(new MessageEditorScreen(this)); },
                DispatchButton.Style.PRIMARY));

        int gap = compactLayout ? 4 : 6;
        int half = Math.max(1, (innerWidth - gap) / 2);
        Component copyLabel = Component.translatable(compactLayout
                ? "screen.copyl.backup_copy_short" : "screen.copyl.backup_copy");
        Component importLabel = Component.translatable(compactLayout
                ? "screen.copyl.backup_import_short" : "screen.copyl.backup_import");
        DispatchButton copyButton = addRenderableWidget(new DispatchButton(innerLeft,
                firstY + rowStep * 2, half, buttonHeight, copyLabel,
                this::copyBackup, DispatchButton.Style.SECONDARY));
        DispatchButton importButton = addRenderableWidget(new DispatchButton(innerLeft + half + gap,
                firstY + rowStep * 2, Math.max(1, innerWidth - half - gap), buttonHeight,
                importLabel, this::importBackup, DispatchButton.Style.SECONDARY));
        if (compactLayout) {
            copyButton.setTooltip(Tooltip.create(Component.translatable("screen.copyl.backup_copy")));
            importButton.setTooltip(Tooltip.create(Component.translatable("screen.copyl.backup_import")));
        }

        addRenderableWidget(new DispatchButton(innerLeft, firstY + rowStep * 3, innerWidth, buttonHeight,
                Component.translatable("screen.copyl.back"), this::onClose, DispatchButton.Style.SECONDARY));
    }

    private Component bindingLabel() {
        return Component.translatable("screen.copyl.open_binding", CopyLBinding.displayName(CopyLConfig.get().openKey));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturing) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            capturing = false;
            bindingButton.setMessage(bindingLabel());
            return true;
        }

        int binding = (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)
                ? CopyLBinding.UNBOUND
                : CopyLBinding.sanitize(keyCode);
        if (binding >= 0 || binding == CopyLBinding.UNBOUND) applyOpenBinding(binding);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (capturing) {
            int binding = CopyLBinding.encodeMouse(button);
            if (binding >= 0) applyOpenBinding(binding);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyOpenBinding(int newBinding) {
        CopyLConfig config = CopyLConfig.get();
        int oldBinding = config.openKey;
        config.openKey = newBinding;

        if (!config.save()) {
            config.openKey = oldBinding;
            capturing = false;
            bindingButton.setMessage(bindingLabel());
            showFeedback(Component.translatable("screen.copyl.config_save_failed"), DispatchPalette.ERROR);
            return;
        }

        boolean conflictsCleared = newBinding < 0 || MessageConfig.getInstance().clearBindingAndSave(newBinding);
        capturing = false;
        bindingButton.setMessage(bindingLabel());

        if (!conflictsCleared) {
            showFeedback(Component.translatable("screen.copyl.conflict_save_failed"), DispatchPalette.WARNING);
        } else if (newBinding < 0) {
            showFeedback(Component.translatable("screen.copyl.open_removed"), DispatchPalette.SUCCESS);
        } else {
            showFeedback(Component.translatable("screen.copyl.open_updated"), DispatchPalette.SUCCESS);
        }
    }

    private void copyBackup() {
        if (minecraft == null) return;
        try {
            minecraft.keyboardHandler.setClipboard(CopyLProfileCodec.exportProfile());
            importConfirmUntil = 0L;
            showFeedback(Component.translatable("screen.copyl.backup_copied"), DispatchPalette.SUCCESS);
        } catch (Exception exception) {
            showFeedback(Component.translatable("screen.copyl.backup_copy_failed"), DispatchPalette.ERROR);
        }
    }

    private void importBackup() {
        if (minecraft == null) return;

        long now = System.currentTimeMillis();
        if (now > importConfirmUntil) {
            importConfirmUntil = now + IMPORT_CONFIRM_MS;
            showFeedback(Component.translatable("screen.copyl.backup_import_confirm"), DispatchPalette.WARNING);
            return;
        }
        importConfirmUntil = 0L;

        String clipboard;
        try {
            clipboard = minecraft.keyboardHandler.getClipboard();
        } catch (Exception exception) {
            showFeedback(Component.translatable("screen.copyl.backup_invalid"), DispatchPalette.ERROR);
            return;
        }

        CopyLProfileCodec.ImportResult result = CopyLProfileCodec.importProfile(clipboard);
        if (result == CopyLProfileCodec.ImportResult.SUCCESS) {
            bindingButton.setMessage(bindingLabel());
            showFeedback(Component.translatable("screen.copyl.backup_imported"), DispatchPalette.SUCCESS);
        } else if (result == CopyLProfileCodec.ImportResult.INVALID) {
            showFeedback(Component.translatable("screen.copyl.backup_invalid"), DispatchPalette.ERROR);
        } else {
            showFeedback(Component.translatable("screen.copyl.backup_save_failed"), DispatchPalette.ERROR);
        }
    }

    private void showFeedback(Component text, int color) {
        feedback = text;
        feedbackColor = color;
        feedbackUntil = System.currentTimeMillis() + 3500L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, DispatchPalette.NIGHT);

        graphics.fill(panelLeft + 3, panelTop + 3,
                panelLeft + panelWidth + 3, panelTop + panelHeight + 3,
                DispatchPalette.PANEL_SHADOW);
        graphics.fill(panelLeft, panelTop,
                panelLeft + panelWidth, panelTop + panelHeight,
                DispatchPalette.PAPER);
        graphics.fill(panelLeft, panelTop,
                panelLeft + 4, panelTop + panelHeight,
                DispatchPalette.BLOOD);
        graphics.fill(panelLeft + 4, panelTop,
                panelLeft + panelWidth, panelTop + 1,
                DispatchPalette.PAPER_RULE);
        graphics.fill(panelLeft + 5, panelTop + 2,
                panelLeft + panelWidth - 8, panelTop + 3,
                0x44FFFFFF);
        graphics.fill(panelLeft + panelWidth - 2, panelTop + 3,
                panelLeft + panelWidth - 1, panelTop + panelHeight - 4,
                DispatchPalette.PAPER_RULE);
        graphics.fill(panelLeft + 10, panelTop + panelHeight - 3,
                panelLeft + panelWidth - 10, panelTop + panelHeight - 2,
                DispatchPalette.PAPER_RULE);

        int headerX = panelLeft + contentInset;
        graphics.drawString(font, Component.translatable("screen.copyl.brand"),
                headerX, panelTop + (compactLayout ? 5 : 12), DispatchPalette.INK, false);

        int configured = 0;
        int assigned = 0;
        MessageConfig messages = MessageConfig.getInstance();
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (!messages.getMessage(i).isBlank()) configured++;
            if (messages.getKeyCode(i) >= 0) assigned++;
        }

        Component summary = Component.translatable("screen.copyl.editor_summary", configured, assigned);
        boolean feedbackVisible = feedback != null && System.currentTimeMillis() <= feedbackUntil;
        if (compactLayout) {
            String status = feedbackVisible ? feedback.getString() : summary.getString();
            String clipped = font.plainSubstrByWidth(status, Math.max(40, panelWidth - contentInset * 2));
            graphics.drawString(font, clipped, headerX, panelTop + 18,
                    feedbackVisible ? feedbackColor : DispatchPalette.GHOUL_TEAL, false);
        } else {
            graphics.drawString(font,
                    Component.translatable("screen.copyl.header_subtitle"),
                    headerX,
                    panelTop + 26,
                    DispatchPalette.INK_MUTED,
                    false);
            graphics.drawString(font, summary, headerX, panelTop + 43, DispatchPalette.GHOUL_TEAL, false);

            graphics.drawString(font,
                    Component.translatable("screen.copyl.config_hint"),
                    headerX, panelTop + 54, DispatchPalette.INK_MUTED, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (feedbackVisible && !compactLayout) {
            String clipped = font.plainSubstrByWidth(feedback.getString(), Math.max(80, panelWidth - 30));
            DispatchText.drawCentered(graphics, font, clipped, width / 2,
                    panelTop + panelHeight - 12, feedbackColor);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        capturing = false;
        importConfirmUntil = 0L;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
