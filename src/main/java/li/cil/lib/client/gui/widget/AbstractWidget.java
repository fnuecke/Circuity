package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.input.InputSystem;
import li.cil.lib.api.gui.widget.Canvas;
import li.cil.lib.api.gui.widget.Container;
import li.cil.lib.api.gui.widget.RenderSettings;
import li.cil.lib.api.gui.widget.Widget;
import li.cil.lib.api.math.Vector2;
import li.cil.lib.client.renderer.font.FontRenderer;
import li.cil.lib.client.renderer.font.FontRendererMinecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public abstract class AbstractWidget<T extends AbstractWidget> implements Widget<T> {
    public static final RenderSettings DEFAULT_RENDER_SETTINGS = new DefaultRenderSettings();
    public static final InputSystem DEFAULT_INPUT_SYSTEM = new DefaultInputSystem();

    // --------------------------------------------------------------------- //

    private Container parent;
    private int x, y;
    private int width, height;

    // --------------------------------------------------------------------- //
    // Widget

    @Override
    public Container getParent() {
        return parent;
    }

    @Override
    public T setParent(@Nullable final Container value) {
        if (value == parent) {
            return self();
        }

        final Container oldParent = parent;
        parent = value;

        // After setting, to make sure the early exit kicks in.
        if (oldParent != null) {
            oldParent.remove(this);
        }
        if (parent != null) {
            parent.add(this);
        }

        invalidateParent();

        return self();
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public T setX(final int value) {
        if (value == x) {
            return self();
        }

        x = value;

        invalidateParent();

        return self();
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public T setY(final int value) {
        if (value == y) {
            return self();
        }

        y = value;

        invalidateParent();

        return self();
    }

    @Override
    public Vector2 getPos() {
        return new Vector2(x, y);
    }

    @Override
    public T setPos(final Vector2 value) {
        setX((int) value.x);
        setY((int) value.y);

        return self();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public T setWidth(final int value) {
        if (value == width) {
            return self();
        }

        width = value;

        invalidateParent();

        return self();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public T setHeight(final int value) {
        if (value == height) {
            return self();
        }

        height = value;

        invalidateParent();

        return self();
    }

    @Override
    public Vector2 getSize() {
        return new Vector2(width, height);
    }

    @Override
    public T setSize(final Vector2 value) {
        setWidth((int) value.x);
        setHeight((int) value.y);

        return self();
    }

    @Override
    public Vector2 toLocal(final Vector2 global) {
        final Container parent = getParent();
        if (parent != null) {
            return parent.toLocal(global.sub(getPos()));
        }
        return global;
    }

    @Override
    public Vector2 toGlobal(final Vector2 local) {
        final Container parent = getParent();
        if (parent != null) {
            return parent.toGlobal(local.add(getPos()));
        }
        return local;
    }

    @Override
    public void render() {
        setColorRGB(0xFF00FF);
        drawQuad(0, 0, getWidth(), getHeight());
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public Canvas getCanvas() {
        if (parent == null) {
            return null;
        } else if (parent instanceof AbstractWidget) {
            return ((AbstractWidget) parent).getCanvas();
        } else {
            Container canvasCandidate = parent;
            while (parent.getParent() != null) {
                canvasCandidate = parent.getParent();
            }
            assert canvasCandidate instanceof Canvas : "Object is in a hierarchy that does not have a Canvas as root.";
            return (Canvas) canvasCandidate;
        }
    }

    // --------------------------------------------------------------------- //

    protected static void setColorRGB(final int rgb) {
        final float r, g, b;
        r = ((rgb >> 16) & 0xFF) / 255f;
        g = ((rgb >> 8) & 0xFF) / 255f;
        b = (rgb & 0xFF) / 255f;
        GlStateManager.color(r, g, b);
    }

    protected static void drawQuad(final float x0, final float y0, final float x1, final float y1) {
        final Tessellator t = Tessellator.getInstance();
        final VertexBuffer b = t.getBuffer();

        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        b.pos(x0, y0, 0).endVertex();
        b.pos(x1, y0, 0).endVertex();
        b.pos(x1, y1, 0).endVertex();
        b.pos(x0, y1, 0).endVertex();

        t.draw();
    }

    protected void drawElevatedQuad(final float x0, final float y0, final float x1, final float y1, final float margin) {
        GlStateManager.disableTexture2D();

        setColorRGB(getRenderSettings().getBackgroundColor());
        drawQuad(0, 0, getWidth(), getHeight());
        setColorRGB(getRenderSettings().getRimShadowColor());
        drawQuad(margin, margin, getWidth(), getHeight());
        setColorRGB(getRenderSettings().getRimLightColor());
        drawQuad(0, 0, getWidth() - margin, getHeight() - margin);
        setColorRGB(getRenderSettings().getBackgroundColor());
        drawQuad(margin, margin, getWidth() - margin, getHeight() - margin);

        GlStateManager.enableTexture2D();
    }

    protected void invalidateParent() {
        final Container parent = getParent();
        if (parent != null) {
            parent.invalidate();
        }
    }

    protected RenderSettings getRenderSettings() {
        final Canvas canvas = getCanvas();
        return canvas == null ? DEFAULT_RENDER_SETTINGS : canvas.getRenderSettings();
    }

    protected InputSystem getInputSystem() {
        final Canvas canvas = getCanvas();
        return canvas == null ? DEFAULT_INPUT_SYSTEM : canvas.getInputSystem();
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    // --------------------------------------------------------------------- //

    private static final class DefaultRenderSettings implements RenderSettings {
        @Override
        public FontRenderer getFontRenderer() {
            return FontRendererMinecraft.INSTANCE;
        }

        @Override
        public void setFontRenderer(final FontRenderer value) {
        }

        @Override
        public int getButtonPadding() {
            return 4;
        }

        @Override
        public void setButtonPadding(final int value) {
        }

        @Override
        public int getBackgroundColor() {
            return 0x998877;
        }

        @Override
        public void setBackgroundColor(final int value) {
        }

        @Override
        public int getRimLightColor() {
            return 0xFFEEDD;
        }

        @Override
        public void setRimLightColor(final int value) {
        }

        @Override
        public int getRimShadowColor() {
            return 0x332211;
        }

        @Override
        public void setRimShadowColor(final int value) {
        }
    }

    private static final class DefaultInputSystem implements InputSystem {
        @Override
        public Vector2 getMousePosition() {
            return Vector2.ZERO;
        }

        @Override
        public boolean getMouseDown(final int index) {
            return false;
        }

        @Override
        public boolean getMouseUp(final int index) {
            return false;
        }

        @Override
        public boolean getKeyDown(final int id) {
            return false;
        }

        @Override
        public boolean getKeyUp(final int id) {
            return false;
        }

        @Override
        public void update() {
        }
    }
}
