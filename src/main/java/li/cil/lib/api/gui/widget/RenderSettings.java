package li.cil.lib.api.gui.widget;

import li.cil.lib.client.renderer.font.FontRenderer;

public interface RenderSettings {
    FontRenderer getFontRenderer();

    void setFontRenderer(final FontRenderer value);

    int getButtonPadding();

    void setButtonPadding(final int value);

    int getBackgroundColor();

    void setBackgroundColor(final int value);

    int getRimLightColor();

    void setRimLightColor(final int value);

    int getRimShadowColor();

    void setRimShadowColor(final int value);
}
