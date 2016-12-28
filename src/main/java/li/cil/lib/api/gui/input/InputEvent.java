package li.cil.lib.api.gui.input;

import li.cil.lib.api.math.Vector2;

/**
 * Event sent to {@link li.cil.lib.api.gui.widget.Widget}s implementing the
 * {@link li.cil.lib.api.gui.widget.EventHandler} interface when there is some
 * user input that can be processed by the UI.
 */
public final class InputEvent {
    /**
     * The type of the event.
     */
    public enum Type {
        /**
         * Updated position of the pointer (mouse move).
         */
        POINTER,

        /**
         * Mouse input (left click, right click).
         */
        MOUSE,

        /**
         * Keyboard input (key pressed, key released).
         */
        KEYBOARD
    }

    /**
     * The event phase.
     */
    public enum Phase {
        /**
         * Starting event, such as mouse down and key pressed.
         */
        BEGIN,

        /**
         * Intermediate event, such as mouse move.
         */
        UPDATE,

        /**
         * Ending event, such as mouse up and key released.
         */
        END
    }

    // --------------------------------------------------------------------- //

    private final Type type;
    private final Phase phase;
    private final int button;
    private final Vector2 position;
    private final int key;
    private final char character;

    // --------------------------------------------------------------------- //

    private InputEvent(final Type type, final Phase phase, final int button, final Vector2 position, final int key, final char character) {
        this.type = type;
        this.phase = phase;
        this.button = button;
        this.position = position;
        this.key = key;
        this.character = character;
    }

    // --------------------------------------------------------------------- //

    public static InputEvent getPointerEvent(final Vector2 position) {
        return new InputEvent(Type.POINTER, Phase.UPDATE, 0, position, 0, '\0');
    }

    public static InputEvent getMouseEvent(final Phase phase, final int button, final Vector2 position) {
        return new InputEvent(Type.MOUSE, phase, button, position, 0, '\0');
    }

    public static InputEvent getKeyEvent(final Phase phase, final int key, final char character) {
        return new InputEvent(Type.KEYBOARD, phase, 0, Vector2.ZERO, key, character);
    }

    public InputEvent withPosition(final Vector2 position) {
        return new InputEvent(type, phase, button, position, key, character);
    }

    // --------------------------------------------------------------------- //

    /**
     * The type of the event.
     *
     * @return the type of the event.
     */
    public Type getType() {
        return type;
    }

    /**
     * The event phase.
     *
     * @return the phase of the event.
     */
    public Phase getPhase() {
        return phase;
    }

    /**
     * The mouse button of this event, if it is a mouse event.
     *
     * @return the mouse button.
     */
    public int getButton() {
        return button;
    }

    /**
     * The global mouse position, if this is a pointer or mouse event.
     *
     * @return the mouse position.
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * The key of this event, if it is a keyboard event.
     *
     * @return the key.
     */
    public int getKey() {
        return key;
    }

    /**
     * The character associated with the key, if this is a keyboard event.
     *
     * @return the character.
     */
    public char getCharacter() {
        return character;
    }
}
