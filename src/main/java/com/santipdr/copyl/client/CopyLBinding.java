package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Persistent raw input binding used by CopyL without registering vanilla KeyMappings.
 * Keyboard codes keep their GLFW value; mouse buttons live in a separate encoded range.
 */
public final class CopyLBinding {
    public static final int UNBOUND = -1;
    private static final int MOUSE_BASE = 1000;

    private CopyLBinding() {
    }

    public static int sanitize(int binding) {
        if (binding == UNBOUND) return UNBOUND;
        if (isKeyboard(binding) || isMouse(binding)) return binding;
        return UNBOUND;
    }

    public static boolean isKeyboard(int binding) {
        return binding >= GLFW.GLFW_KEY_SPACE && binding <= GLFW.GLFW_KEY_LAST;
    }

    public static boolean isMouse(int binding) {
        int button = binding - MOUSE_BASE;
        return button >= GLFW.GLFW_MOUSE_BUTTON_1 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST;
    }

    public static int encodeMouse(int button) {
        if (button < GLFW.GLFW_MOUSE_BUTTON_1 || button > GLFW.GLFW_MOUSE_BUTTON_LAST) return UNBOUND;
        return MOUSE_BASE + button;
    }

    public static int mouseButton(int binding) {
        return isMouse(binding) ? binding - MOUSE_BASE : -1;
    }

    public static boolean isDown(Minecraft minecraft, int binding) {
        if (minecraft == null || minecraft.getWindow() == null) return false;
        long window = minecraft.getWindow().getWindow();
        if (isKeyboard(binding)) return InputConstants.isKeyDown(window, binding);
        if (isMouse(binding)) {
            return GLFW.glfwGetMouseButton(window, mouseButton(binding)) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    public static Component displayName(int binding) {
        if (isKeyboard(binding)) {
            return InputConstants.Type.KEYSYM.getOrCreate(binding).getDisplayName();
        }
        if (isMouse(binding)) {
            return Component.translatable("screen.copyl.mouse_button", mouseButton(binding) + 1);
        }
        return Component.translatable("screen.copyl.unbound");
    }
}
