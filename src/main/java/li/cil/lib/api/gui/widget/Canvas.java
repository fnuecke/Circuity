package li.cil.lib.api.gui.widget;

import li.cil.lib.api.gui.input.InputSystem;

/**
 * Implemented by specialized widgets which serve as the root node for a UI.
 *
 * @param <T> leaf type for builder pattern use.
 */
public interface Canvas<T extends Canvas> extends Container<T>, EventHandler<T> {
    /**
     * Global render settings for this UI.
     *
     * @return the render settings.
     */
    RenderSettings getRenderSettings();

    /**
     * The input system for this UI.
     *
     * @return the input system.
     */
    InputSystem getInputSystem();
}
