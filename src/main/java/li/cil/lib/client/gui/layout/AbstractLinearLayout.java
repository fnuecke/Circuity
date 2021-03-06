package li.cil.lib.client.gui.layout;

import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.gui.layout.Layoutable;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.Widget;
import li.cil.lib.api.math.Vector2;
import sun.plugin.dom.exception.InvalidStateException;

public abstract class AbstractLinearLayout implements Layout {
    private Alignment.Horizontal horizontalAlignment = Alignment.Horizontal.LEFT;
    private Alignment.Vertical verticalAlignment = Alignment.Vertical.TOP;
    private Vector2 horizontalPadding = Vector2.ZERO, verticalPadding = Vector2.ZERO;
    private boolean expandWidth, expandHeight;

    // --------------------------------------------------------------------- //

    public Alignment.Horizontal getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public AbstractLinearLayout setHorizontalAlignment(final Alignment.Horizontal value) {
        horizontalAlignment = value;

        return this;
    }

    public Alignment.Vertical getVerticalAlignment() {
        return verticalAlignment;
    }

    public AbstractLinearLayout setVerticalAlignment(final Alignment.Vertical value) {
        verticalAlignment = value;

        return this;
    }

    public int getPaddingLeft() {
        return (int) horizontalPadding.get(0);
    }

    public AbstractLinearLayout setPaddingLeft(final int value) {
        horizontalPadding = horizontalPadding.set(0, value);

        return this;
    }

    public int getPaddingRight() {
        return (int) horizontalPadding.get(1);
    }

    public AbstractLinearLayout setPaddingRight(final int value) {
        horizontalPadding = horizontalPadding.set(1, value);

        return this;
    }

    public int getPaddingTop() {
        return (int) verticalPadding.get(0);
    }

    public AbstractLinearLayout setPaddingTop(final int value) {
        verticalPadding = verticalPadding.set(0, value);

        return this;
    }

    public int getPaddingBottom() {
        return (int) verticalPadding.get(1);
    }

    public AbstractLinearLayout setPaddingBottom(final int value) {
        verticalPadding = verticalPadding.set(1, value);

        return this;
    }

    public AbstractLinearLayout setPadding(final int left, final int right, final int top, final int bottom) {
        horizontalPadding = new Vector2(left, right);
        verticalPadding = new Vector2(top, bottom);

        return this;
    }

    public boolean shouldExpandWidth() {
        return expandWidth;
    }

    public AbstractLinearLayout setExpandWidth(final boolean value) {
        expandWidth = value;

        return this;
    }

    public boolean shouldExpandHeight() {
        return expandHeight;
    }

    public AbstractLinearLayout setExpandHeight(final boolean value) {
        expandHeight = value;

        return this;
    }

    // --------------------------------------------------------------------- //

    protected abstract int getPrimaryDimension();

    // --------------------------------------------------------------------- //
    // Layout

    @Override
    public void apply(final Container<?> container) {
        final int dim0 = getPrimaryDimension();
        final int dim1 = getSecondaryDimension();
        final int containerSize0 = (int) container.getSize().get(dim0);
        final int containerSize1 = (int) container.getSize().get(dim1);

        if (dim0 != 0 && dim0 != 1) {
            throw new InvalidStateException("getPrimaryDimension returned invalid dimension");
        }

        int totalSize0 = 0;
        float totalWeight0 = 0f;
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            totalSize0 += Math.max(layoutable.getMinSize().get(dim0), layoutable.getPreferredSize().get(dim0));
            totalWeight0 += layoutable.getFlexibleSize().get(dim0);
        }

        final int paddingBefore0 = computePaddingBefore(dim0);
        final int paddingAfter0 = computePaddingAfter(dim0);
        final int availableSize0 = containerSize0 - paddingBefore0 - paddingAfter0;

        final int paddingBefore1 = computePaddingBefore(dim1);
        final int paddingAfter1 = computePaddingAfter(dim1);
        final int availableSize1 = containerSize1 - paddingBefore1 - paddingAfter1;

        final int surplusSize0 = availableSize0 - totalSize0;
        final boolean canExpand = shouldExpand(dim0) && surplusSize0 > 0 && totalWeight0 > 0;

        int offset0 = (canExpand ? 0 : computeOffset(dim0, totalSize0, availableSize0)) + paddingBefore0;
        for (final Widget child : container.getChildren()) {
            if (!(child instanceof Layoutable)) {
                continue;
            }
            final Layoutable layoutable = (Layoutable) child;

            final Vector2 minSize = layoutable.getMinSize();
            final Vector2 preferredSize = layoutable.getPreferredSize();
            final int minSize0 = (int) minSize.get(dim0);
            final int minSize1 = (int) minSize.get(dim1);
            final int preferredSize0 = (int) preferredSize.get(dim0);
            final int preferredSize1 = (int) preferredSize.get(dim1);
            final int weight0 = (int) layoutable.getFlexibleSize().get(dim0);
            int size0 = Math.max(minSize0, preferredSize0);

            Vector2 position = layoutable.getPos().set(dim0, offset0);

            if (canExpand && weight0 > 0) {
                final float relativeWeight = weight0 / totalWeight0;
                size0 += (int) (surplusSize0 * relativeWeight);
            }

            Vector2 size = layoutable.getSize().set(dim0, size0);

            offset0 += size0;

            if (shouldExpand(dim1) && availableSize1 > preferredSize1) {
                final int size1 = Math.max(minSize1, availableSize1);
                size = size.set(dim1, size1);
                position = position.set(dim1, paddingBefore1);
            } else {
                final int size1 = Math.max(minSize1, Math.min(availableSize1, preferredSize1));
                size = size.set(dim1, size1);
                final int offset1 = computeOffset(dim1, (int) size.get(dim1), availableSize1) + paddingBefore1;
                position = position.set(dim1, offset1);
            }

            layoutable.setPos(position);
            layoutable.setSize(size);
        }
    }

    // --------------------------------------------------------------------- //

    private int getSecondaryDimension() {
        return 1 - getPrimaryDimension();
    }

    private int computePaddingBefore(final int dimension) {
        switch (dimension) {
            case 0:
                return (int) horizontalPadding.x;
            case 1:
                return (int) verticalPadding.x;
            default:
                throw new IllegalStateException();
        }
    }

    private int computePaddingAfter(final int dimension) {
        switch (dimension) {
            case 0:
                return (int) horizontalPadding.y;
            case 1:
                return (int) verticalPadding.y;
            default:
                throw new IllegalStateException();
        }
    }

    private int computeOffset(final int dimension, final int inner, final int outer) {
        switch (dimension) {
            case 0:
                return horizontalAlignment.computeOffset(inner, outer);
            case 1:
                return verticalAlignment.computeOffset(inner, outer);
            default:
                throw new IllegalStateException();
        }
    }

    private boolean shouldExpand(final int dimension) {
        switch (dimension) {
            case 0:
                return expandWidth;
            case 1:
                return expandHeight;
            default:
                throw new IllegalStateException();
        }
    }
}
