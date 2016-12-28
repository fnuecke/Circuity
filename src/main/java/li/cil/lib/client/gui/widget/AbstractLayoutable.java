package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.layout.Layoutable;

public abstract class AbstractLayoutable<T extends AbstractLayoutable> extends AbstractWidget<T> implements Layoutable<T> {
    private float flexibleWidth, flexibleHeight;

    // --------------------------------------------------------------------- //
    // Layoutable

    @Override
    public float getFlexibleWidth() {
        return flexibleWidth;
    }

    @Override
    public T setFlexibleWidth(final float value) {
        if (Float.compare(value, flexibleWidth) == 0) {
            return self();
        }

        flexibleWidth = value;

        invalidateParent();

        return self();
    }

    @Override
    public float getFlexibleHeight() {
        return flexibleHeight;
    }

    @Override
    public T setFlexibleHeight(final float value) {
        if (Float.compare(value, flexibleHeight) == 0) {
            return self();
        }

        flexibleHeight = value;

        invalidateParent();

        return self();
    }
}
