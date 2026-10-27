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

        Font font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, width - 10));
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + Math.max(0, (height - 8) / 2);
        graphics.drawString(font, label, textX, textY, ink, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}