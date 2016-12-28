package li.cil.lib.api.gui.layout;

import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.Widget;

/**
 * A layout implementation is responsible for positioning and sizing child
 * {@link Widget}s in a {@link Container}.
 */
public interface Layout {
    /**
     * Applies this layout to the children of the specified container widget.
     *
     * @param container the container to apply the layout to.
     */
    void apply(final Container<?> container);
}
