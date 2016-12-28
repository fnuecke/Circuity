package li.cil.lib.api.gui.layout;

import li.cil.lib.api.gui.widget.Widget;
import li.cil.lib.client.gui.layout.HorizontalLayout;
import li.cil.lib.client.gui.layout.VerticalLayout;

/**
 * Implemented by {@link Widget}s that can be managed by a {@link Layout}.
 * <p>
 * Provides information required to properly position and size the elements.
 *
 * @param <T> leaf type for builder pattern use.
 */
public interface Layoutable<T extends Layoutable> extends Widget<T> {
    /**
     * The minimal width of the widget.
     * <p>
     * A layout will never set the width of the widget to a value smaller than
     * this, even if this results in it overflowing it container.
     *
     * @return the minimum width of the widget;
     */
    default int getMinWidth() {
        return 0;
    }

    /**
     * The minimal height of the widget.
     * <p>
     * A layout will never set the height of the widget to a value smaller than
     * this, even if this results in it overflowing it container.
     *
     * @return the minimum height of the widget;
     */
    default int getMinHeight() {
        return 0;
    }

    /**
     * The preferred width of the widget.
     * <p>
     * A layout will typically try to set the widget's width to this value, if
     * this does not result in it overflowing its container.
     * <p>
     * If {@link #getFlexibleWidth()} returns a non-zero value, this may be
     * ignored by the layout, depending on its configuration.
     *
     * @return the preferred width of the widget.
     */
    default int getPreferredWidth() {
        return getWidth();
    }

    /**
     * The preferred height of the widget.
     * <p>
     * A layout will typically try to set the widget's height to this value, if
     * this does not result in it overflowing its container.
     * <p>
     * If {@link #getFlexibleHeight()} returns a non-zero value, this may be
     * ignored by the layout, depending on its configuration.
     *
     * @return the preferred height of the widget.
     */
    default int getPreferredHeight() {
        return getHeight();
    }

    /**
     * The flexible width of the widget.
     * <p>
     * This can be considered the <em>weight</em> of the widget in layouts that
     * support flexible widths. Typically the widget will then be assigned a
     * width that is proportional to this value in relation to other widgets'
     * flexible width. A typical use is for making an element stretch across
     * the full width of its parent in a {@link VerticalLayout}.
     *
     * @return the flexible width of this widget.
     */
    float getFlexibleWidth();

    /**
     * Sets the flexible width of the widget.
     *
     * @param value the flexible width.
     * @return the widget itself, for builder pattern use.
     * @see #getFlexibleWidth()
     */
    T setFlexibleWidth(final float value);

    /**
     * The flexible height of the widget.
     * <p>
     * This can be considered the <em>weight</em> of the widget in layouts that
     * support flexible heights. Typically the widget will then be assigned a
     * height that is proportional to this value in relation to other widgets'
     * flexible height. A typical use is for making an element stretch across
     * the full height of its parent in a {@link HorizontalLayout}.
     *
     * @return the flexible height of this widget.
     */
    float getFlexibleHeight();

    /**
     * Sets the flexible height of the widget.
     *
     * @param value the flexible height.
     * @return the widget itself, for builder pattern use.
     * @see #getFlexibleHeight()
     */
    T setFlexibleHeight(final float value);
}
