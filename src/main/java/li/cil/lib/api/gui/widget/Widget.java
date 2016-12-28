package li.cil.lib.api.gui.widget;

import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.math.Vector2;

import javax.annotation.Nullable;

/**
 * Base class for all UI widgets.
 * <p>
 * Widgets are individual elements of a UI, and have different, specialized
 * behaviors. For example, there are {@link Container} widgets, which are
 * used to establish a hierarchy of widgets, labels, buttons and more.
 * <p>
 * The hierarchy of widgets is determined by {@link Container}s, with every
 * widget being child of a container, with the sole exception of {@link Canvas}
 * widgets, which serve as root nodes in the tree of widgets.
 * <p>
 * This hierarchy determines how widgets are layouted by {@link Layout}s, and
 * in which order events are processed by widgets (in particular for click
 * events, in case two widgets overlap).
 *
 * @param <T> leaf type for builder pattern use.
 */
public interface Widget<T extends Widget> {
    /**
     * Get the container containing this widget, if any.
     *
     * @return the parent of this widget; <code>null</code> if there is none.
     */
    @Nullable
    Container getParent();

    /**
     * Set the parent of this widget, or <code>null</code> to remove it from
     * its current container widget.
     *
     * @param value the new parent of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setParent(@Nullable final Container value);

    /**
     * Get the horizontal position of the widget relative to its parent.
     *
     * @return the x position of the widget.
     */
    int getX();

    /**
     * Get the the horizontal position of the widget relative to its parent.
     *
     * @param value the x position of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setX(final int value);

    /**
     * Get the vertical position of the widget relative to its parent.
     *
     * @return the y position of the widget.
     */
    int getY();

    /**
     * Set the vertical position of the widget relative to its parent.
     *
     * @param value the y position of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setY(final int value);

    /**
     * Get the position of the widget relative to its parent.
     *
     * @return the position of the widget.
     */
    Vector2 getPos();

    /**
     * Set the position of the widget relative to its parent.
     *
     * @param value the position of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setPos(final Vector2 value);

    /**
     * Get the width of the widget.
     *
     * @return the width of the widget.
     */
    int getWidth();

    /**
     * Set the width of the widget.
     *
     * @param value the width of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setWidth(final int value);

    /**
     * Get the height of the widget.
     *
     * @return the height of the widget.
     */
    int getHeight();

    /**
     * Set the height of the widget.
     *
     * @param value the height of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setHeight(final int value);

    /**
     * Get the size of the widget.
     *
     * @return the size of the widget.
     */
    Vector2 getSize();

    /**
     * Set the size of the widget.
     *
     * @param value the size of the widget.
     * @return the widget itself for builder pattern use.
     */
    T setSize(final Vector2 value);

    /**
     * Convert a coordinate in global coordinate space, i.e. in {@link Canvas}
     * space, to local coordinate space of this widget.
     *
     * @param global the global coordinate.
     * @return the coordinate in local space.
     */
    Vector2 toLocal(final Vector2 global);

    /**
     * Convert a coordinate in local coordinate space of this widget to to
     * global coordinate space, i.e. in {@link Canvas} space.
     *
     * @param local the local coordinate.
     * @return the coordinate in global space.
     */
    Vector2 toGlobal(final Vector2 local);

    /**
     * Render this widget.
     * <p>
     * The GL state will have been set up so that <code>(0, 0, 0)</code>
     * represents the top left corner of the widget, based on its position, and
     * <code>(getWidth(), getHeight(), 0)</code> represents the bottom right
     * corner of the widget, based on its size.
     */
    void render();
}
