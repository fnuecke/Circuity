package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.layout.Layoutable;
import li.cil.lib.api.gui.widget.Widget;

public class Panel extends AbstractContainer<Panel> implements Layoutable<Panel> {
    private float flexibleWidth, flexibleHeight;

    // --------------------------------------------------------------------- //
    // Layoutable

    @Override
    public int getPreferredWidth() {
        int xMax = 0;
        for (final Widget child : getChildren()) {
            xMax = Math.max(xMax, child.getX() + child.getWidth());
        }
        return xMax;
    }

    @Override
    public int getPreferredHeight() {
        int yMax = 0;
        for (final Widget child : getChildren()) {
            yMax = Math.max(yMax, child.getY() + child.getHeight());
        }
        return yMax;
    }

    @Override
    public float getFlexibleWidth() {
        return flexibleWidth;
    }

    @Override
    public Panel setFlexibleWidth(final float value) {
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
    public Panel setFlexibleHeight(final float value) {
        if (Float.compare(value, flexibleHeight) == 0) {
            return self();
        }

        flexibleHeight = value;

        invalidateParent();

        return self();
    }
}
