package com.santipdr.copyl.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Editable field that paints clear, unshadowed ink over the vanilla text shadow. */
final class DispatchEditBox extends EditBox {
    private final Font dispatchFont;
    private final Component hint;

    DispatchEditBox(Font font, int x, int y, int width, int height,
                    Component narration, Component hint) {
        super(font, x, y, width, height, narration);
        this.dispatchFont = font;
        this.hint = hint;
        setHint(hint);
    }

    void drawFlatText(GuiGraphics graphics) {
        if (!visible) return;

        int textY = getY() + Math.max(0, (height - 8) / 2);
        int innerLeft = getX() + 2;
        int innerRight = getX() + width - 2;
        int clearTop = Math.max(getY() + 1, textY - 1);
        int clearBottom = Math.min(getY() + height - 1, textY + 9);

        graphics.fill(getX() + 1, clearTop, getX() + width - 1, clearBottom,
                DispatchPalette.PAPER_HOVER);

        String value = getValue();
        int start = value.isEmpty() ? 0 : lowerBoundScreenX(value.length(), innerLeft);
        if (value.isEmpty()) {
            Component placeholder = DispatchText.fit(
                    dispatchFont, hint, Math.max(1, innerRight - innerLeft));
            DispatchText.draw(graphics, dispatchFont, placeholder, innerLeft, textY,
                    DispatchPalette.INK_MUTED);
        } else {
            int end = visibleEnd(value, start, innerLeft, innerRight);

            String selected = getHighlighted();
            int selectionStart = selected.isEmpty() ? -1
                    : locateSelection(value, selected, getCursorPosition());
            if (selectionStart >= 0) {
                int visibleStart = Math.max(start, selectionStart);
                int visibleEnd = Math.min(end, selectionStart + selected.length());
                if (visibleEnd > visibleStart) {
                    int selectionX = Math.max(innerLeft, customScreenX(value, start, visibleStart, innerLeft));
                    int selectionRight = Math.min(innerRight, customScreenX(value, start, visibleEnd, innerLeft));
                    graphics.fill(selectionX, clearTop, selectionRight, clearBottom,
                            DispatchPalette.GHOUL_TEAL);
                    drawRange(graphics, value, start, visibleStart, textY, DispatchPalette.INK);
                    drawRange(graphics, value, visibleStart, visibleEnd, textY, DispatchPalette.PAPER);
                    drawRange(graphics, value, visibleEnd, end, textY, DispatchPalette.INK);
                } else {
                    drawRange(graphics, value, start, end, textY, DispatchPalette.INK);
                }
            } else {
                drawRange(graphics, value, start, end, textY, DispatchPalette.INK);
            }
        }

        if (isFocused()) {
            int cursorX = customScreenX(value, start, getCursorPosition(), innerLeft);
            if (cursorX >= innerLeft && cursorX < innerRight) {
                graphics.fill(cursorX, clearTop + 1, cursorX + 1, clearBottom - 1,
                        DispatchPalette.INK);
            }
        }
    }

    private int lowerBoundScreenX(int valueLength, int targetX) {
        int low = 0;
        int high = valueLength;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (getScreenX(middle) < targetX) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int visibleEnd(String value, int start, int left, int right) {
        int position = start;
        int currentX = left;
        while (position < value.length()) {
            int codePoint = value.codePointAt(position);
            int next = position + Character.charCount(codePoint);
            int glyphWidth = DispatchText.width(dispatchFont, value.substring(position, next));
            if (currentX + glyphWidth > right + 1) break;
            currentX += glyphWidth;
            position = next;
        }
        return position;
    }

    private int customScreenX(String value, int start, int position, int left) {
        int bounded = Math.max(start, Math.min(value.length(), position));
        return left + DispatchText.width(dispatchFont, value.substring(start, bounded));
    }

    private static int locateSelection(String value, String selected, int cursor) {
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int candidate = value.indexOf(selected); candidate >= 0;
             candidate = value.indexOf(selected, candidate + 1)) {
            int distance = Math.min(Math.abs(candidate - cursor),
                    Math.abs(candidate + selected.length() - cursor));
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
                if (distance == 0) break;
            }
        }
        return bestDistance == 0 ? best : -1;
    }

    private void drawRange(GuiGraphics graphics, String value, int start, int end,
                          int y, int color) {
        if (end <= start) return;
        String valuePart = value.substring(start, end);
        int visibleStart = lowerBoundScreenX(value.length(), getX() + 2);
        int x = customScreenX(value, visibleStart, start, getX() + 2);
        DispatchText.draw(graphics, dispatchFont, Component.literal(valuePart), x, y, color);
    }
}