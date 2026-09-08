package com.santipdr.copyl.client;

/** Four-corner anchor used by Lclient HUD panels. */
public enum HudAnchor {
    TOP_LEFT("Arriba izquierda"),
    TOP_RIGHT("Arriba derecha"),
    BOTTOM_LEFT("Abajo izquierda"),
    BOTTOM_RIGHT("Abajo derecha");

    private final String displayName;

    HudAnchor(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_RIGHT;
    }

    public boolean isLeft() {
        return this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    public int left(int screenWidth, int panelWidth, int margin) {
        return isLeft() ? margin : Math.max(margin, screenWidth - panelWidth - margin);
    }

    public int top(int screenHeight, int panelHeight, int margin) {
        return isTop() ? margin : Math.max(margin, screenHeight - panelHeight - margin);
    }

    public static HudAnchor fromConfig(int value, HudAnchor fallback) {
        HudAnchor[] values = values();
        return value >= 0 && value < values.length ? values[value] : fallback;
    }

    public static int next(int value, HudAnchor fallback) {
        HudAnchor current = fromConfig(value, fallback);
        return (current.ordinal() + 1) % values().length;
    }
}
