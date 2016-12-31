package li.cil.lib.client.gui.widget;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import li.cil.lib.api.gui.input.InputEvent;
import li.cil.lib.api.gui.input.InputSystem;
import li.cil.lib.api.gui.widget.Canvas;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.RenderSettings;
import li.cil.lib.api.math.Vector2;
import li.cil.lib.client.renderer.font.FontRenderer;

import javax.annotation.Nullable;

public class Window extends AbstractContainer<Window> implements Canvas<Window>, AbstractEventHandler<Window> {
    private static final int MARGIN_SIZE = 2;

    // --------------------------------------------------------------------- //

    private final RenderSettings renderSettings = new RenderSettingsImpl();
    private final InputSystemImpl inputSystem = new InputSystemImpl();

    // --------------------------------------------------------------------- //
    // Widget

    @Override
    public Container getParent() {
        return null;
    }

    @Override
    public Window setParent(@Nullable final Container value) {
        throw new UnsupportedOperationException("Canvas cannot be child to anything.");
    }

    @Override
    public Canvas getCanvas() {
        return this;
    }

    @Override
    public void render() {
        drawElevatedQuad(0, 0, getWidth(), getHeight(), MARGIN_SIZE);

        super.render();
    }

    // --------------------------------------------------------------------- //
    // Canvas

    @Override
    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    @Override
    public InputSystem getInputSystem() {
        return inputSystem;
    }

    // --------------------------------------------------------------------- //
    // EventHandler

    @Override
    public boolean processInput(final InputEvent event) {
        return inputSystem.processInput(event) && super.processInput(event);
    }

    // --------------------------------------------------------------------- //

    private static final class RenderSettingsImpl implements RenderSettings {
        private FontRenderer fontRenderer = AbstractWidget.DEFAULT_RENDER_SETTINGS.getFontRenderer();
        private int buttonPadding = AbstractWidget.DEFAULT_RENDER_SETTINGS.getButtonPadding();
        private int buttonColor = AbstractWidget.DEFAULT_RENDER_SETTINGS.getBackgroundColor();
        private int buttonColorRimLight = AbstractWidget.DEFAULT_RENDER_SETTINGS.getRimLightColor();
        private int buttonColorRimShadow = AbstractWidget.DEFAULT_RENDER_SETTINGS.getRimShadowColor();

        // --------------------------------------------------------------------- //

        @Override
        public FontRenderer getFontRenderer() {
            return fontRenderer;
        }

        @Override
        public void setFontRenderer(final FontRenderer value) {
            fontRenderer = value;
        }

        @Override
        public int getButtonPadding() {
            return buttonPadding;
        }

        @Override
        public void setButtonPadding(final int value) {
            buttonPadding = value;
        }

        @Override
        public int getBackgroundColor() {
            return buttonColor;
        }

        @Override
        public void setBackgroundColor(final int value) {
            buttonColor = value;
        }

        @Override
        public int getRimLightColor() {
            return buttonColorRimLight;
        }

        @Override
        public void setRimLightColor(final int value) {
            buttonColorRimLight = value;
        }

        @Override
        public int getRimShadowColor() {
            return buttonColorRimShadow;
        }

        @Override
        public void setRimShadowColor(final int value) {
            buttonColorRimShadow = value;
        }
    }

    public static final class InputSystemImpl implements InputSystem {
        private Vector2 mousePosition;
        private final TIntSet mouseDown = new TIntHashSet();
        private final TIntSet mouseUp = new TIntHashSet();
        private final TIntSet keyDown = new TIntHashSet();
        private final TIntSet keyUp = new TIntHashSet();

        @Override
        public Vector2 getMousePosition() {
            return mousePosition;
        }

        @Override
        public boolean getMouseDown(final int index) {
            return mouseDown.contains(index);
        }

        @Override
        public boolean getMouseUp(final int index) {
            return mouseUp.contains(index);
        }

        @Override
        public boolean getKeyDown(final int id) {
            return keyDown.contains(id);
        }

        @Override
        public boolean getKeyUp(final int id) {
            return keyUp.contains(id);
        }

        public void update() {
            mouseUp.clear();
            keyUp.clear();
        }

        public boolean processInput(final InputEvent event) {
            switch (event.getType()) {
                case POINTER:
                    if (mousePosition == event.getPosition()) {
                        return false;
                    }
                    mousePosition = event.getPosition();
                    return true;
                case MOUSE:
                    switch (event.getPhase()) {
                        case BEGIN:
                            return mouseDown.add(event.getButton());
                        case END:
                            mouseDown.remove(event.getButton());
                            return mouseUp.add(event.getButton());
                    }
                    break;
                case KEYBOARD:
                    switch (event.getPhase()) {
                        case BEGIN:
                            return keyDown.add(event.getKey());
                        case END:
                            keyDown.remove(event.getKey());
                            return keyUp.add(event.getKey());
                    }
                    break;
            }
            return true;
        }
    }
}
