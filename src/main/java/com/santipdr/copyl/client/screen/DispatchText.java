package com.santipdr.copyl.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared type and alignment rules for CEDA field-paper screens. */
final class DispatchText {
    private DispatchText() {
    }

    /** Use plain Minecraft text so an unavailable custom font cannot become tofu boxes. */
    static Component plain(Component text) {
        return Component.literal(text.getString());
    }

    static int width(Font font, Component text) {
        return font.width(plain(text));
    }

    static int width(Font font, String text) {
        return width(font, Component.literal(text));
    }

    static Component fit(Font font, Component text, int maxWidth) {
        int available = Math.max(1, maxWidth);
        if (width(font, text) <= available) return plain(text);

        String source = text.getString();
        StringBuilder prefix = new StringBuilder(source.length());
        String ellipsis = "…";
        for (int offset = 0; offset < source.length();) {
            int codePoint = source.codePointAt(offset);
            int next = offset + Character.charCount(codePoint);
            String candidate = prefix.toString() + new String(Character.toChars(codePoint)) + ellipsis;
            if (width(font, candidate) > available) break;
            prefix.appendCodePoint(codePoint);
            offset = next;
        }

        while (prefix.length() > 0 && width(font, prefix + ellipsis) > available) {
            int last = prefix.codePointBefore(prefix.length());
            prefix.setLength(prefix.length() - Character.charCount(last));
        }
        String result = prefix.length() == 0 && width(font, ellipsis) > available
                ? "" : prefix + ellipsis;
        return plain(Component.literal(result));
    }

    static void draw(GuiGraphics graphics, Font font, Component text,
                     int x, int y, int color) {
        graphics.drawString(font, plain(text), x, y, color, false);
    }

    static void drawCentered(GuiGraphics graphics, Font font, String text,
                             int centerX, int y, int color) {
        drawCentered(graphics, font, Component.literal(text), centerX, y, color);
    }

    static void drawCentered(GuiGraphics graphics, Font font, Component text,
                             int centerX, int y, int color) {
        Component label = plain(text);
        graphics.drawString(font, label, centerX - font.width(label) / 2, y, color, false);
    }
}
