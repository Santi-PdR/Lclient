package com.santipdr.copyl.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared no-shadow text alignment for the field-paper screens. */
final class DispatchText {
    private DispatchText() {
    }

    static void drawCentered(GuiGraphics graphics, Font font, String text,
                             int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    static void drawCentered(GuiGraphics graphics, Font font, Component text,
                             int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }
}