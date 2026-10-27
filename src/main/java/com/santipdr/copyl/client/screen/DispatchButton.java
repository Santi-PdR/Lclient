package com.santipdr.copyl.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/** A still paper control with crisp ink and no Minecraft drop shadow. */
final class DispatchButton extends AbstractButton {
    enum Style { PRIMARY, SECONDARY, DANGER }

    private final Runnable action;
    private final Style style;
    private Component cachedLabel;
    private String cachedSource;
    private Font cachedFont;
    private int cachedLabelWidth = -1;

    DispatchButton(int x, int y, int width, int height, Component label,
                   Runnable action, Style style) {
        super(x, y, width, height, label);
        this.action = Objects.requireNonNull(action);
        this.style = Objects.requireNonNull(style);
    }

    @Override
    public void onPress() {
        if (active) action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isHoveredOrFocused();
        int edge;
        int fill;
        int ink;

        if (style == Style.PRIMARY) {
            edge = DispatchPalette.BUTTON_PRIMARY_EDGE;
            fill = hovered ? DispatchPalette.BUTTON_PRIMARY_HOVER : DispatchPalette.BUTTON_PRIMARY;
            ink = DispatchPalette.PAPER;
        } else if (style == Style.DANGER) {
            edge = DispatchPalette.BUTTON_DANGER_INK;
            fill = hovered ? DispatchPalette.PAPER_HOVER : DispatchPalette.BUTTON_DANGER;
            ink = DispatchPalette.BUTTON_DANGER_INK;
        } else {
            edge = DispatchPalette.BUTTON_EDGE;
            fill = hovered ? DispatchPalette.BUTTON_HOVER : DispatchPalette.BUTTON_PAPER;
            ink = DispatchPalette.INK;
        }

        if (!active) {
            fill = DispatchPalette.BUTTON_DISABLED;
            ink = DispatchPalette.INK_MUTED;
        }

        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, edge);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        }
        if (height > 4 && width > 4 && style != Style.DANGER) {
            int highlight = style == Style.PRIMARY ? 0x554A777A : 0x66FFFFFF;
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, highlight);
        }

        if (active && isFocused()) drawFocusRing(graphics, x, y, width, height);

        Font font = Minecraft.getInstance().font;
        int maxLabelWidth = Math.max(1, width - 12);
        Component label = fittedLabel(font, maxLabelWidth);
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + Math.max(0, (height - 9) / 2);
        graphics.drawString(font, label, textX, textY, ink, false);
    }

    private Component fittedLabel(Font font, int maxWidth) {
        String source = getMessage().getString();
        if (cachedLabel == null || font != cachedFont || maxWidth != cachedLabelWidth
                || !source.equals(cachedSource)) {
            cachedSource = source;
            cachedFont = font;
            cachedLabelWidth = maxWidth;
            cachedLabel = DispatchText.fit(font, getMessage(), maxWidth);
        }
        return cachedLabel;
    }

    private static void drawFocusRing(GuiGraphics graphics, int x, int y, int width, int height) {
        int outer = DispatchPalette.FOCUS_EDGE;
        int inner = DispatchPalette.FOCUS_HIGHLIGHT;

        graphics.fill(x - 2, y - 2, x + width + 2, y - 1, outer);
        graphics.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, outer);
        graphics.fill(x - 2, y - 1, x - 1, y + height + 1, outer);
        graphics.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, outer);

        graphics.fill(x - 1, y - 1, x + width + 1, y, inner);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, inner);
        graphics.fill(x - 1, y, x, y + height, inner);
        graphics.fill(x + width, y, x + width + 1, y + height, inner);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}