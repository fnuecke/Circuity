package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.widget.EventHandler;
import li.cil.lib.api.math.Rect;
import li.cil.lib.api.math.Vector2;

public interface AbstractEventHandler<T extends AbstractEventHandler> extends EventHandler<T> {
    @Override
    default Rect getGlobalBounds() {
        return new Rect(toGlobal(Vector2.ZERO), toGlobal(getSize()));
    }
}
