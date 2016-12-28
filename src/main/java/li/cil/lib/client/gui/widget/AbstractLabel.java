package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.layout.Alignment;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Objects;

public abstract class AbstractLabel<T extends AbstractLabel> extends AbstractLayoutable<T> {
    private String text = "";
    private int textColor = 0xFFFFFF;
    private Alignment.Horizontal horizontalAlignment = Alignment.Horizontal.LEFT;
    private Alignment.Vertical verticalAlignment = Alignment.Vertical.MIDDLE;

    // --------------------------------------------------------------------- //

    public String getText() {
        return text;
    }

    public T setText(final String value) {
        if (Objects.equals(value, text)) {
            return self();
        }

        text = value;

        invalidateParent();

        return self();
    }

    public int getTextColor() {
        return textColor;
    }

    public T setTextColor(final int color) {
        this.textColor = color;

        return self();
    }

    public Alignment.Horizontal getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public T setHorizontalAlignment(final Alignment.Horizontal value) {
        horizontalAlignment = value;

        return self();
    }

    public Alignment.Vertical getVerticalAlignment() {
        return verticalAlignment;
    }

    public T setVerticalAlignment(final Alignment.Vertical value) {
        verticalAlignment = value;

        return self();
    }

    // --------------------------------------------------------------------- //
    // Layoutable

    @Override
    public int getMinHeight() {
        return getTextHeight();
    }

    @Override
    public int getPreferredWidth() {
        return getTextWidth();
    }

    @Override
    public int getPreferredHeight() {
        return getTextHeight();
    }

    // --------------------------------------------------------------------- //
    // Widget

    @Override
    public void render() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(computeLeft(), computeTop(), 0);

        setColorRGB(getTextColor());
        // TODO How to handle overflow consistently?
        getRenderSettings().getFontRenderer().drawString(getText());

        GlStateManager.popMatrix();
    }

    // --------------------------------------------------------------------- //

    private int getTextWidth() {
        return getRenderSettings().getFontRenderer().getStringWidth(getText());
    }

    private int getTextHeight() {
        return getRenderSettings().getFontRenderer().getCharHeight();
    }

    private int computeLeft() {
        return getHorizontalAlignment().computeOffset(getTextWidth(), getWidth());
    }

    private int computeTop() {
        return getVerticalAlignment().computeOffset(getTextHeight(), getHeight());
    }
}
