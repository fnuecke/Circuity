package li.cil.lib.client.gui.layout;

import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.gui.layout.Layoutable;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.Widget;

public class VerticalLayout implements Layout {
    private boolean expandWidth, expandHeight;
    private Alignment.Horizontal horizontalAlignment = Alignment.Horizontal.LEFT;
    private Alignment.Vertical verticalAlignment = Alignment.Vertical.TOP;

    // --------------------------------------------------------------------- //

    public boolean shouldExpandWidth() {
        return expandWidth;
    }

    public VerticalLayout setExpandWidth(final boolean value) {
        if (value == expandWidth) {
            return this;
        }

        expandWidth = value;

        return this;
    }

    public boolean shouldExpandHeight() {
        return expandHeight;
    }

    public VerticalLayout setExpandHeight(final boolean value) {
        if (value == expandHeight) {
            return this;
        }

        expandHeight = value;

        return this;
    }

    public Alignment.Horizontal getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public VerticalLayout setHorizontalAlignment(final Alignment.Horizontal value) {
        if (value == horizontalAlignment) {
            return this;
        }

        horizontalAlignment = value;

        return this;
    }

    public Alignment.Vertical getVerticalAlignment() {
        return verticalAlignment;
    }

    public VerticalLayout setVerticalAlignment(final Alignment.Vertical value) {
        if (value == verticalAlignment) {
            return this;
        }

        verticalAlignment = value;

        return this;
    }

    // --------------------------------------------------------------------- //
    // Layout

    @Override
    public void apply(final Container<?> container) {
        int totalHeight = 0;
        float totalWeight = 0f;
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            totalHeight += Math.max(layoutable.getMinHeight(), layoutable.getPreferredHeight());
            totalWeight += layoutable.getFlexibleHeight();
        }

        final int availableHeight = container.getHeight();
        final int surplusHeight = availableHeight - totalHeight;
        final boolean canExpand = expandHeight && surplusHeight > 0 && totalWeight > 0;

        int y = canExpand ? 0 : verticalAlignment.computeOffset(totalHeight, availableHeight);
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            layoutable.setY(y);
            layoutable.setX(0);

            int height = Math.max(layoutable.getMinHeight(), layoutable.getPreferredHeight());
            if (canExpand && layoutable.getFlexibleHeight() > 0) {
                final float relativeWeight = layoutable.getFlexibleHeight() / totalWeight;
                height += (int) (surplusHeight * relativeWeight);
            }
            layoutable.setHeight(height);
            y += height;

            if (expandWidth && container.getWidth() > layoutable.getPreferredWidth()) {
                final int width = Math.max(layoutable.getMinWidth(), container.getWidth());
                layoutable.setWidth(width);
            } else {
                final int width = Math.max(layoutable.getMinWidth(), Math.min(container.getWidth(), layoutable.getPreferredWidth()));
                layoutable.setWidth(width);
                layoutable.setX(horizontalAlignment.computeOffset(layoutable.getWidth(), container.getWidth()));
            }
        }
    }
}
