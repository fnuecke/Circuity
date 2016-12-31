package li.cil.circuity.client.gui.spatial;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.gui.input.InputEvent;
import li.cil.lib.api.gui.spatial.SpatialUIClient;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.widget.Canvas;
import li.cil.lib.api.math.Vector2;
import net.minecraft.client.renderer.GlStateManager;

import javax.annotation.Nullable;

public abstract class AbstractSpatialUIClient implements SpatialUIClient {
    protected final SpatialUIContext context;

    // --------------------------------------------------------------------- //

    protected AbstractSpatialUIClient(final SpatialUIContext context) {
        this.context = context;
    }

    // --------------------------------------------------------------------- //
    // SpatialUIClient

    @Override
    public void handleInput(final InputEvent event) {
        final Canvas window = getWindow();
        if (window != null) {
            final Vector2 localPosition = new Vector2(event.getPosition().x * getSize(), event.getPosition().y * getSize());
            window.processInput(event.withPosition(localPosition));
        }
    }

    @Override
    public void update() {
        if (!isValid()) {
            SillyBeeAPI.spatialUI.close();
            return;
        }

        final Canvas window = getWindow();
        if (window != null) {
            window.getInputSystem().update();
        }
    }

    @Override
    public void render() {
        GlStateManager.pushMatrix();

        final float scale = 1f / getSize();
        GlStateManager.scale(scale, scale, 1);

        final Canvas window = getWindow();
        if (window != null) {
            window.render();
        }

        GlStateManager.popMatrix();
    }

    // --------------------------------------------------------------------- //

    protected abstract int getSize();

    protected abstract boolean isValid();

    @Nullable
    protected abstract Canvas getWindow();
}
