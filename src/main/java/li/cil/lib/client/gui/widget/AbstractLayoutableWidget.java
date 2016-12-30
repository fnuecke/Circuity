package li.cil.lib.client.gui.widget;

public abstract class AbstractLayoutableWidget<T extends AbstractLayoutableWidget> extends AbstractWidget<T> implements AbstractLayoutable<T> {
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
