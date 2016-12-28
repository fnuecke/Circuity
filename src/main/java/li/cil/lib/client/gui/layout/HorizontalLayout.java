package li.cil.lib.client.gui.layout;

import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.gui.layout.Layoutable;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.Widget;

public class HorizontalLayout implements Layout {
    private boolean expandWidth, expandHeight;
    private Alignment.Horizontal horizontalAlignment = Alignment.Horizontal.LEFT;
    private Alignment.Vertical verticalAlignment = Alignment.Vertical.TOP;

    // --------------------------------------------------------------------- //

    public boolean shouldExpandWidth() {
        return expandWidth;
    }

    public HorizontalLayout setExpandWidth(final boolean value) {
        if (value == expandWidth) {
            return this;
        }

        expandWidth = value;

        return this;
    }

    public boolean shouldExpandHeight() {
        return expandHeight;
    }

    public HorizontalLayout setExpandHeight(final boolean value) {
        if (value == expandHeight) {
            return this;
        }

        expandHeight = value;

        return this;
    }

    public Alignment.Horizontal getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public HorizontalLayout setHorizontalAlignment(final Alignment.Horizontal value) {
        if (value == horizontalAlignment) {
            return this;
        }

        horizontalAlignment = value;

        return this;
    }

    public Alignment.Vertical getVerticalAlignment() {
        return verticalAlignment;
    }

    public HorizontalLayout setVerticalAlignment(final Alignment.Vertical value) {
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
        int totalWidth = 0;
        float totalWeight = 0f;
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            totalWidth += Math.max(layoutable.getMinWidth(), layoutable.getPreferredWidth());
            totalWeight += layoutable.getFlexibleWidth();
        }

        final int availableWidth = container.getWidth();
        final int surplusWidth = availableWidth - totalWidth;
        final boolean canExpand = expandWidth && surplusWidth > 0 && totalWeight > 0;

        int x = canExpand ? 0 : horizontalAlignment.computeOffset(totalWidth, availableWidth);
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            layoutable.setX(x);
            layoutable.setY(0);

            int width = Math.max(layoutable.getMinWidth(), layoutable.getPreferredWidth());
            if (canExpand && layoutable.getFlexibleWidth() > 0) {
                final float relativeWeight = layoutable.getFlexibleWidth() / totalWeight;
                width += (int) (surplusWidth * relativeWeight);
            }
            layoutable.setWidth(width);
            x += width;

            if (expandHeight && container.getHeight() > layoutable.getPreferredHeight()) {
                final int height = Math.max(layoutable.getMinHeight(), container.getHeight());
                layoutable.setHeight(height);
            } else {
                final int height = Math.max(layoutable.getMinHeight(), Math.min(container.getHeight(), layoutable.getPreferredHeight()));
                layoutable.setHeight(height);
                layoutable.setY(verticalAlignment.computeOffset(layoutable.getHeight(), container.getHeight()));
            }
        }
    }
}
