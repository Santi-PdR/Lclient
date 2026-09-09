package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.CopyLBinding;
import com.santipdr.copyl.client.CopyLConfig;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Polished Mods > CopyL > Config surface. */
public final class CopyLSettingsScreen extends Screen {
    private final Screen parent;
    private Button bindingButton;
    private boolean capturing;
    private Component feedback = Component.empty();
    private int feedbackColor = 0xFF86D6FA;
    private long feedbackUntil;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public CopyLSettingsScreen(Screen parent) {
        super(Component.translatable("screen.copyl.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(440, Math.max(220, width - 24));
        panelHeight = Math.min(205, Math.max(168, height - 40));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(20, (height - panelHeight) / 2);

        int innerLeft = panelLeft + 20;
        int innerWidth = panelWidth - 40;
        int buttonHeight = 20;
        int firstY = panelTop + 80;

        bindingButton = addRenderableWidget(Button.builder(bindingLabel(), b -> {
            capturing = true;
            feedback = Component.empty();
            b.setMessage(Component.translatable("screen.copyl.press_binding"));
        }).bounds(innerLeft, firstY, innerWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.edit_messages"), b -> {
            if (minecraft != null) minecraft.setScreen(new MessageEditorScreen(this));
        }).bounds(innerLeft, firstY + 27, innerWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.back"), b -> onClose())
                .bounds(innerLeft, firstY + 54, innerWidth, buttonHeight).build());
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
            showFeedback(Component.translatable("screen.copyl.config_save_failed"), 0xFFFF7777);
            return;
        }

        boolean conflictsCleared = newBinding < 0 || MessageConfig.getInstance().clearBindingAndSave(newBinding);
        capturing = false;
        bindingButton.setMessage(bindingLabel());

        if (!conflictsCleared) {
            showFeedback(Component.translatable("screen.copyl.conflict_save_failed"), 0xFFFFB777);
        } else if (newBinding < 0) {
            showFeedback(Component.translatable("screen.copyl.open_removed"), 0xFF86D6FA);
        } else {
            showFeedback(Component.translatable("screen.copyl.open_updated"), 0xFF86D6FA);
        }
    }

    private void showFeedback(Component text, int color) {
        feedback = text;
        feedbackColor = color;
        feedbackUntil = System.currentTimeMillis() + 3000L;
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
                0xEA121921);
        graphics.fill(panelLeft, panelTop,
                panelLeft + 4, panelTop + panelHeight,
                0xFF55B9E8);
        graphics.fill(panelLeft + 4, panelTop,
                panelLeft + panelWidth, panelTop + 1,
                0x665E859B);

        graphics.drawString(font, "COPYL", panelLeft + 20, panelTop + 14, 0xFFFFFFFF, false);
        graphics.drawString(font,
                Component.translatable("screen.copyl.header_subtitle"),
                panelLeft + 20,
                panelTop + 28,
                0xFF7E9EB2,
                false);

        int configured = 0;
        int assigned = 0;
        MessageConfig messages = MessageConfig.getInstance();
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (!messages.getMessage(i).isBlank()) configured++;
            if (messages.getKeyCode(i) >= 0) assigned++;
        }

        Component summary = Component.translatable("screen.copyl.editor_summary", configured, assigned);
        graphics.drawString(font,
                summary,
                panelLeft + 20,
                panelTop + 46,
                0xFFA7D9EF,
                false);

        String hint = font.plainSubstrByWidth(
                Component.translatable("screen.copyl.config_hint").getString(),
                Math.max(80, panelWidth - 40));
        graphics.drawString(font, hint, panelLeft + 20, panelTop + 59, 0xFF9AABB8, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (feedback != null && System.currentTimeMillis() <= feedbackUntil) {
            String clipped = font.plainSubstrByWidth(feedback.getString(), Math.max(80, panelWidth - 30));
            graphics.drawCenteredString(font,
                    clipped,
                    width / 2,
                    panelTop + panelHeight - 13,
                    feedbackColor);
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
