package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

/** Transactional editor for CopyL quick-message slots. */
public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int MAX_NAME_LENGTH = 24;
    private static final int COMPACT_PAGE_SIZE = 5;

    private final Screen parent;
    private final EditBox[] nameFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final EditBox[] messageFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final Button[] keyButtons = new Button[CopyLKeyMappings.SLOT_COUNT];

    private final String[] draftNames = new String[CopyLKeyMappings.SLOT_COUNT];
    private final String[] draftMessages = new String[CopyLKeyMappings.SLOT_COUNT];
    private final int[] draftKeys = new int[CopyLKeyMappings.SLOT_COUNT];

    private int bindingIndex = -1;
    private int page;
    private String warning = "";
    private long warningUntil;

    public MessageEditorScreen(Screen parent) {
        super(Component.literal("Lclient — CopyL"));
        this.parent = parent;

        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            draftNames[i] = config.getName(i);
            draftMessages[i] = config.getMessage(i);
            draftKeys[i] = config.getKeyCode(i);
        }
    }

    @Override
    protected void init() {
        Arrays.fill(nameFields, null);
        Arrays.fill(messageFields, null);
        Arrays.fill(keyButtons, null);

        boolean compact = useCompactLayout();
        boolean tiny = useTinyLayout();
        boolean tight = height < 220;
        int pageSize = pageSize();
        int maxPage = Math.max(0, (CopyLKeyMappings.SLOT_COUNT - 1) / pageSize);
        page = Math.max(0, Math.min(page, maxPage));

        int fieldHeight = tight ? 18 : 20;
        int rowStep = tiny ? (tight ? 41 : 45) : compact ? (tight ? 23 : 28) : 34;
        int top = tiny ? (tight ? 38 : 53) : compact ? (tight ? 35 : 50) : 76;

        int contentWidth;
        if (tiny) contentWidth = Math.min(320, Math.max(120, width - 16));
        else if (compact) contentWidth = Math.min(500, Math.max(180, width - 20));
        else contentWidth = Math.min(width - 24, 760);
        int left = (width - contentWidth) / 2;
        int columnWidth = compact ? contentWidth : (contentWidth - 12) / 2;

        int firstSlot = compact ? page * pageSize : 0;
        int lastSlot = compact
                ? Math.min(CopyLKeyMappings.SLOT_COUNT, firstSlot + pageSize)
                : CopyLKeyMappings.SLOT_COUNT;

        for (int i = firstSlot; i < lastSlot; i++) {
            int local = compact ? i - firstSlot : i;
            int column = compact ? 0 : local / 5;
            int row = compact ? local : local % 5;
            int cardLeft = left + column * (columnWidth + 12);
            int y = top + row * rowStep;
            final int slot = i;

            if (tiny) {
                int keyWidth = Math.min(82, Math.max(48, columnWidth / 3));
                int nameWidth = Math.max(40, columnWidth - keyWidth - 5);

                EditBox name = new EditBox(font, cardLeft, y, nameWidth, fieldHeight,
                        Component.literal("Nombre " + (i + 1)));
                name.setMaxLength(MAX_NAME_LENGTH);
                name.setValue(draftNames[i]);
                name.setHint(Component.literal("Nombre"));
                nameFields[i] = addRenderableWidget(name);

                keyButtons[i] = addRenderableWidget(Button.builder(keyLabel(i), b -> {
                    captureFields();
                    bindingIndex = slot;
                    warning = "";
                    updateKeyLabels();
                }).bounds(cardLeft + nameWidth + 5, y, keyWidth, fieldHeight).build());

                EditBox message = new EditBox(font, cardLeft, y + fieldHeight + 3, columnWidth, fieldHeight,
                        Component.literal("Mensaje " + (i + 1)));
                message.setMaxLength(MAX_MESSAGE_LENGTH);
                message.setValue(draftMessages[i]);
                message.setHint(Component.literal("Mensaje o /comando..."));
                messageFields[i] = addRenderableWidget(message);
            } else {
                int keyWidth = compact
                        ? Math.min(76, Math.max(54, columnWidth / 4))
                        : 86;
                int nameWidth = compact
                        ? Math.min(92, Math.max(58, columnWidth / 4))
                        : Math.min(92, Math.max(70, columnWidth / 4));
                int messageWidth = Math.max(50, columnWidth - nameWidth - keyWidth - 10);

                EditBox name = new EditBox(font, cardLeft, y, nameWidth, fieldHeight,
                        Component.literal("Nombre " + (i + 1)));
                name.setMaxLength(MAX_NAME_LENGTH);
                name.setValue(draftNames[i]);
                name.setHint(Component.literal("Nombre"));
                nameFields[i] = addRenderableWidget(name);

                EditBox message = new EditBox(font, cardLeft + nameWidth + 5, y, messageWidth, fieldHeight,
                        Component.literal("Mensaje " + (i + 1)));
                message.setMaxLength(MAX_MESSAGE_LENGTH);
                message.setValue(draftMessages[i]);
                message.setHint(Component.literal("Mensaje o /comando..."));
                messageFields[i] = addRenderableWidget(message);

                keyButtons[i] = addRenderableWidget(Button.builder(keyLabel(i), b -> {
                    captureFields();
                    bindingIndex = slot;
                    warning = "";
                    updateKeyLabels();
                }).bounds(cardLeft + columnWidth - keyWidth, y, keyWidth, fieldHeight).build());
            }
        }

        if (compact) {
            int pagerY = tight ? 18 : 31;
            Button previous = addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                if (page > 0) {
                    captureFields();
                    bindingIndex = -1;
                    page--;
                    rebuildWidgets();
                }
            }).bounds(width / 2 - 62, pagerY, 28, 16).build());
            previous.active = page > 0;

            Button next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                if (page < maxPage) {
                    captureFields();
                    bindingIndex = -1;
                    page++;
                    rebuildWidgets();
                }
            }).bounds(width / 2 + 34, pagerY, 28, 16).build());
            next.active = page < maxPage;
        }

        int visibleRows = compact ? pageSize : 5;
        int bottom = top + rowStep * visibleRows + (tight ? 1 : 5);
        int actionHeight = tight ? 18 : 20;
        int actionWidth = Math.min(112, Math.max(66, (contentWidth - 8) / 2));
        addRenderableWidget(Button.builder(Component.literal("Guardar cambios"), b -> saveAndClose())
                .bounds(width / 2 - actionWidth - 4, bottom, actionWidth, actionHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> cancelAndClose())
                .bounds(width / 2 + 4, bottom, actionWidth, actionHeight).build());
    }

    private boolean useCompactLayout() {
        return width < 620 || height < 280;
    }

    private boolean useTinyLayout() {
        return useCompactLayout() && width < 340;
    }

    private int pageSize() {
        if (!useCompactLayout()) return CopyLKeyMappings.SLOT_COUNT;
        if (useTinyLayout()) return height < 220 ? 2 : 3;
        return COMPACT_PAGE_SIZE;
    }

    private Component keyLabel(int slot) {
        if (bindingIndex == slot) return Component.literal("PULSA TECLA");
        int key = draftKeys[slot];
        return Component.literal(key < 0 ? "Sin tecla" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString());
    }

    private void updateKeyLabels() {
        for (int i = 0; i < keyButtons.length; i++) {
            if (keyButtons[i] != null) keyButtons[i].setMessage(keyLabel(i));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingIndex = -1;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                draftKeys[bindingIndex] = -1;
                bindingIndex = -1;
            } else if (isReservedKey(keyCode)) {
                showWarning("Esa tecla está reservada por Lclient (ruleta, Loot ESP o Recon).");
                bindingIndex = -1;
            } else {
                assignDraftKey(bindingIndex, keyCode);
                bindingIndex = -1;
            }
            updateKeyLabels();
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void assignDraftKey(int slot, int keyCode) {
        for (int i = 0; i < draftKeys.length; i++) {
            if (i != slot && draftKeys[i] == keyCode) draftKeys[i] = -1;
        }
        draftKeys[slot] = keyCode;
    }

    private boolean isReservedKey(int keyCode) {
        LClientConfig c = LClientConfig.get();
        return keyCode == c.wheelKey
                || keyCode == c.lootEspToggleKey
                || keyCode == c.reconZoomKey
                || keyCode == c.reconWaypointKey;
    }

    private void showWarning(String text) {
        warning = text;
        warningUntil = System.currentTimeMillis() + 3500L;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        captureFields();
        bindingIndex = -1;
        super.resize(minecraft, width, height);
    }

    private void captureFields() {
        for (int i = 0; i < draftNames.length; i++) {
            if (nameFields[i] != null) draftNames[i] = nameFields[i].getValue();
            if (messageFields[i] != null) draftMessages[i] = messageFields[i].getValue();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        boolean compact = useCompactLayout();
        boolean tight = height < 220;
        int pageSize = pageSize();
        int maxPage = Math.max(0, (CopyLKeyMappings.SLOT_COUNT - 1) / pageSize);

        graphics.drawCenteredString(font, title, width / 2, tight ? 5 : 10, 0xFFFFFFFF);
        if (compact) {
            int first = page * pageSize + 1;
            int last = Math.min(CopyLKeyMappings.SLOT_COUNT, first + pageSize - 1);
            String summary = "Slots " + first + "–" + last + " · Ctrl+Enter guarda · Esc cancela";
            if (!tight) {
                graphics.drawCenteredString(font,
                        font.plainSubstrByWidth(summary, Math.max(80, width - 20)),
                        width / 2,
                        21,
                        0xFFAAB7C4);
            }
            graphics.drawCenteredString(font,
                    (page + 1) + "/" + (maxPage + 1),
                    width / 2,
                    tight ? 21 : 34,
                    0xFF7F93A6);
        } else {
            graphics.drawCenteredString(font,
                    "10 slots con nombre + tecla única. Guardar aplica todo; Cancelar descarta todo.",
                    width / 2,
                    28,
                    0xFFAAB7C4);
            graphics.drawCenteredString(font,
                    "Jugador: {pos} {x} {y} {z} {dim} {hp} {food} {name}",
                    width / 2,
                    43,
                    0xFF7F93A6);
            graphics.drawCenteredString(font,
                    "Objetivo: {target} {targetdist} {targetpos} {targetx} {targety} {targetz}",
                    width / 2,
                    56,
                    0xFF71879A);
        }

        if (!warning.isBlank() && System.currentTimeMillis() <= warningUntil) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(warning, Math.max(80, width - 20)),
                    width / 2,
                    height - 10,
                    0xFFFFB28A);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void saveAndClose() {
        captureFields();
        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < draftNames.length; i++) {
            config.setName(i, draftNames[i]);
            config.setMessage(i, draftMessages[i]);
        }
        for (int i = 0; i < draftKeys.length; i++) config.setKeyCode(i, draftKeys[i]);
        config.save();
        closeToParent();
    }

    private void cancelAndClose() {
        bindingIndex = -1;
        closeToParent();
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    private void closeToParent() {
        Arrays.fill(nameFields, null);
        Arrays.fill(messageFields, null);
        Arrays.fill(keyButtons, null);
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
