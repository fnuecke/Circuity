package li.cil.lib.api.gui.widget;

import li.cil.lib.api.gui.input.InputEvent;
import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.math.Rect;

/**
 * Implemented by widgets that can process user input, such as buttons.
 *
 * @param <T> leaf type for builder pattern use.
 */
public interface EventHandler<T extends EventHandler> extends Widget<T> {
    /**
     * The bounds of this widget in global coordinate space.
     * <p>
     * This is used to filter spatial events, such as mouse events. The widget
     * will only have {@link #processInput(InputEvent)} called in case the
     * coordinate lies within these bounds.
     * <p>
     * This allows capturing input outside the area assigned to a widget by
     * a {@link Layout}, which can be useful for dynamic elements.
     *
     * @return the bounds of the widget.
     */
    Rect getGlobalBounds();

    /**
     * Called when an input event has been raised.
     * <p>
     * Event handlers will be called in the order in which they appear in the
     * widget hierarchy, until an event handler consumes the event, indicated
     * by it returning <code>true</code> from this method.
     *
     * @param event the event to process.
     * @return <code>true</code> if the event was consumed; <code>false</code> otherwise.
     */
    boolean processInput(final InputEvent event);
}
