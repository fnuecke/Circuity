package li.cil.lib.api.gui.widget;

import li.cil.lib.api.gui.layout.Layout;

import javax.annotation.Nullable;

/**
 * A widget the contains a number of child {@link Widget}s.
 * <p>
 * Typically used to group widgets together for a layout to operate on them.
 *
 * @param <T> leaf type for builder pattern use.
 */
public interface Container<T extends Container> extends Widget<T> {
    /**
     * The list of all child widgets currently in this container.
     *
     * @return the children of this container.
     */
    Iterable<Widget> getChildren();

    /**
     * The number of widgets this container currently holds.
     *
     * @return the number of child widgets.
     */
    int getChildCount();

    /**
     * Get the widget at the specified index in this container.
     *
     * @param index the index of the widget to get.
     * @return the widget at that index.
     */
    Widget getChildAt(final int index);

    /**
     * Get the index a widget has in this container.
     * <p>
     * If the child is not contained in this container, this will return <code>-1</code>.
     *
     * @param widget the widget to get the index for.
     * @return the index of the widget in this container, or <code>-1</code> if it is not in this container.
     */
    int getChildIndex(final Widget widget);

    /**
     * Add a widget to this container.
     *
     * @param widget the widget to add to this container.
     * @return the widget itself for builder pattern use.
     */
    T add(final Widget widget);

    /**
     * Remove a widget from this container.
     *
     * @param widget the widget to remove.
     */
    void remove(final Widget widget);

    /**
     * Remove the widget at the specified index from this container.
     *
     * @param index the index of the widget to remove.
     */
    void removeAt(final int index);

    // --------------------------------------------------------------------- //

    /**
     * Get the layout currently assigned to this container.
     *
     * @return the current layout.
     */
    Layout getLayout();

    /**
     * Set the layout to use for this container.
     *
     * @param value the layout to use.
     * @return the widget itself for builder pattern use.
     */
    T setLayout(@Nullable final Layout value);

    /**
     * Layouts are re-applied before the next {@link #render()} call is
     * delegated to the container's children when the container had this method
     * called on it. This happens automatically when a widget is added to or
     * removed from the container, as well as when the container's dimensions
     * change. It needs to be called by child widgets when their dimensions or
     * position changes.
     */
    void invalidate();
}
