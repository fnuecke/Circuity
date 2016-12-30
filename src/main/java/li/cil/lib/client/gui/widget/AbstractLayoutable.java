package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.layout.Layoutable;
import li.cil.lib.api.math.Vector2;

public interface AbstractLayoutable<T extends AbstractLayoutable> extends Layoutable<T> {
    // --------------------------------------------------------------------- //
    // Layoutable

    @Override
    default int getMinWidth() {
        return 0;
    }

    @Override
    default int getMinHeight() {
        return 0;
    }

    @Override
    default Vector2 getMinSize() {
        return new Vector2(getMinWidth(), getMinHeight());
    }

    @Override
    default int getPreferredWidth() {
        return getMinWidth();
    }

    @Override
    default int getPreferredHeight() {
        return getMinHeight();
    }

    @Override
    default Vector2 getPreferredSize() {
        return new Vector2(getPreferredWidth(), getPreferredHeight());
    }

    @Override
    default Vector2 getFlexibleSize() {
        return new Vector2(getPreferredWidth(), getPreferredHeight());
    }
}
