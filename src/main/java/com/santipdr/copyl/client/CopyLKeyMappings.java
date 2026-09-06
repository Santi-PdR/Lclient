package com.santipdr.copyl.client;

public final class CopyLKeyMappings {
    public static final int SLOT_COUNT = 10;

    private CopyLKeyMappings() {
    }

    public static int getKeyCode(int slot) {
        return MessageConfig.getInstance().getKeyCode(slot);
    }

    public static void setKeyCode(int slot, int keyCode) {
        MessageConfig.getInstance().setKeyCode(slot, keyCode);
    }
}
