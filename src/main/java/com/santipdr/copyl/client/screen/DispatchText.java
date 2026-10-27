package com.santipdr.copyl.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared type and alignment rules for CEDA field-paper screens. */
final class DispatchText {
    private static final ResourceLocation FONT_ID = new ResourceLocation("copyl", "dispatch");

    private DispatchText() {
    }

    static Component styled(Component text) {
        return text.copy().withStyle(style -> style.withFont(FONT_ID));
    }

    static int width(Font font, Component text) {
        return font.width(styled(text));
    }

    static int width(Font font, String text) {
        return width(font, Component.literal(text));
    }

    static Component fit(Font font, Component text, int maxWidth) {
        int available = Math.max(1, maxWidth);
        if (width(font, text) <= available) return styled(text);

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
        return styled(Component.literal(result));
    }

    static void draw(GuiGraphics graphics, Font font, Component text,
                     int x, int y, int color) {
        graphics.drawString(font, styled(text), x, y, color, false);
    }

    static void drawCentered(GuiGraphics graphics, Font font, String text,
                             int centerX, int y, int color) {
        drawCentered(graphics, font, Component.literal(text), centerX, y, color);
    }

    static void drawCentered(GuiGraphics graphics, Font font, Component text,
                             int centerX, int y, int color) {
        Component label = styled(text);
        graphics.drawString(font, label, centerX - font.width(label) / 2, y, color, false);
    }
}
