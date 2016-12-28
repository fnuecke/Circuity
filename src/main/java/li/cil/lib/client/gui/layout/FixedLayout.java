package li.cil.lib.client.gui.layout;

import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.gui.layout.Layoutable;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.Widget;

public class FixedLayout implements Layout {
    @Override
    public void apply(final Container<?> container) {
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            final int availableWidth = container.getWidth() - layoutable.getX();
            final int availableHeight = container.getHeight() - layoutable.getY();
            final int preferredWidth = Math.min(availableWidth, layoutable.getPreferredWidth());
            final int preferredHeight = Math.min(availableHeight, layoutable.getPreferredHeight());
            final int width = Math.max(layoutable.getMinWidth(), preferredWidth);
            final int height = Math.max(layoutable.getMinHeight(), preferredHeight);
            layoutable.setWidth(width);
            layoutable.setHeight(height);
        }
    }
}
