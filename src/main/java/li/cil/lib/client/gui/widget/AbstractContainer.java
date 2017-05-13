package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.input.InputEvent;
import li.cil.lib.api.gui.layout.Layout;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.EventHandler;
import li.cil.lib.api.gui.widget.Widget;
import li.cil.lib.api.math.Rect;
import li.cil.lib.client.gui.layout.FixedLayout;
import net.minecraft.client.renderer.GlStateManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractContainer<T extends AbstractContainer> extends AbstractWidget<T> implements Container<T>, EventHandler<T> {
    private static final Layout DEFAULT_LAYOUT = new FixedLayout();

    // --------------------------------------------------------------------- //

    private final List<Widget> children = new ArrayList<>();
    private Layout layout;
    private boolean isLayoutValid;

    // --------------------------------------------------------------------- //
    // Widget

    @Override
    public T setWidth(final int value) {
        if (value == getWidth()) {
            return self();
        }

        super.setWidth(value);

        invalidate();

        return self();
    }

    @Override
    public T setHeight(final int value) {
        if (value == getHeight()) {
            return self();
        }

        super.setHeight(value);

        invalidate();

        return self();
    }

    @Override
    public void render() {
        validateLayout();

        for (final Widget child : children) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(child.getX(), child.getY(), 0);

            child.render();

            GlStateManager.popMatrix();
        }
    }

    // --------------------------------------------------------------------- //
    // Container

    @Override
    public Iterable<Widget> getChildren() {
        return children;
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public Widget getChildAt(final int index) {
        return children.get(index);
    }

    @Override
    public int getChildIndex(final Widget widget) {
        if (widget.getParent() != this) {
            return -1;
        }
        return children.indexOf(widget);
    }

    @Override
    public T add(final Widget widget) {
        if (children.contains(widget)) {
            return self();
        }

        children.add(widget);
        widget.setParent(this);

        invalidate();

        return self();
    }

    @Override
    public void remove(final Widget widget) {
        final int index = children.indexOf(widget);
        if (index < 0) {
            return;
        }

        children.remove(index);
        widget.setParent(null);

        invalidate();
    }

    @Override
    public void removeAt(final int index) {
        if (index < 0 || index >= children.size()) {
            throw new IndexOutOfBoundsException();
        }

        final Widget widget = children.remove(index);
        widget.setParent(null);

        invalidate();
    }

    @Override
    public void clear() {
        if (children.size() < 1) {
            return;
        }

        for (int index = children.size() - 1; index >= 0; index--) {
            final Widget widget = children.remove(index);
            widget.setParent(null);
        }

        invalidate();
    }

    @Override
    public Layout getLayout() {
        return layout == null ? DEFAULT_LAYOUT : layout;
    }

    @Override
    public T setLayout(@Nullable final Layout value) {
        if (Objects.equals(value, layout)) {
            return self();
        }

        layout = value;

        invalidate();

        return self();
    }

    @Override
    public void invalidate() {
        isLayoutValid = false;
    }

    // --------------------------------------------------------------------- //
    // EventHandler

    @Override
    public Rect getGlobalBounds() {
        Rect result = null;
        for (final Widget child : getChildren()) {
            if (child instanceof EventHandler) {
                final Rect bounds = ((EventHandler) child).getGlobalBounds();
                if (result == null) {
                    result = bounds;
                } else {
                    result = result.union(bounds);
                }
            }
        }
        return result == null ? Rect.ZERO : result;
    }

    @Override
    public boolean processInput(final InputEvent event) {
        for (final Widget child : getChildren()) {
            if (child instanceof EventHandler) {
                final EventHandler eventHandler = (EventHandler) child;
                if (event.getType() == InputEvent.Type.MOUSE) {
                    final Rect bounds = eventHandler.getGlobalBounds();
                    if (!bounds.contains(event.getPosition())) {
                        continue;
                    }
                }
                if (eventHandler.processInput(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --------------------------------------------------------------------- //

    private void validateLayout() {
        if (!isLayoutValid) {
            isLayoutValid = true;
            getLayout().apply(this);
            invalidateParent();
        }
    }
}
