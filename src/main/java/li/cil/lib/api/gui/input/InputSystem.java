package li.cil.lib.api.gui.input;

import li.cil.lib.api.math.Vector2;

public interface InputSystem {
    /**
     * Get the current mouse position.
     *
     * @return the current mouse position.
     */
    Vector2 getMousePosition();

    /**
     * Check whether the specified mouse button is currently being pressed.
     *
     * @param index the mouse button to test for. 0 = left, 1 = right.
     * @return <code>true</code> if the button is pressed; <code>false</code> otherwise.
     */
    boolean getMouseDown(final int index);

    /**
     * Check whether the specified mouse button has been released this frame.
     *
     * @param index the mouse button to test for. 0 = left, 1 = right.
     * @return <code>true</code> if the button has been released; <code>false</code> otherwise.
     */
    boolean getMouseUp(final int index);

    /**
     * Check whether the specified key is currently being pressed.
     *
     * @param id the key to test for. See {@link net.java.games.input.Keyboard}.
     * @return <code>true</code> if the key is pressed; <code>false</code> otherwise.
     */
    boolean getKeyDown(final int id);

    /**
     * Check whether the specified key has been released this frame.
     *
     * @param id the key to test for. See {@link net.java.games.input.Keyboard}.
     * @return <code>true</code> if the key has been released; <code>false</code> otherwise.
     */
    boolean getKeyUp(final int id);

    /**
     * Updates the input system.
     * <p>
     * This must be called once per input frame, to ensure mouse/key up states
     * are correctly cleared.
     */
    void update();
}
