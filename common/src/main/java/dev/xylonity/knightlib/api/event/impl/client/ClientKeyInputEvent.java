package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;

/**
 * Fired when a key is pressed, released, or repeated while no screen is open
 */
public final class ClientKeyInputEvent extends KnightLibEvent {

    private final int key;
    private final int scanCode;
    private final int action;
    private final int modifiers;

    public ClientKeyInputEvent(int key, int scanCode, int action, int modifiers) {
        this.key = key;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
    }

    /**
     * GLFW key code
     */
    public int getKey() {
        return key;
    }

    /**
     * Platform-specific scan code
     */
    public int getScanCode() {
        return scanCode;
    }

    /**
     * GLFW action: GLFW_PRESS (1), GLFW_RELEASE (0), GLFW_REPEAT (2)
     */
    public int getAction() {
        return action;
    }

    /**
     * Bitfield of GLFW modifier keys (Shift, Ctrl...)
     */
    public int getModifiers() {
        return modifiers;
    }

}