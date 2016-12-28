package li.cil.lib.api.gui.widget;

import li.cil.lib.client.renderer.font.FontRenderer;

public interface RenderSettings {
    FontRenderer getFontRenderer();

    void setFontRenderer(final FontRenderer value);

    int getButtonPadding();

    void setButtonPadding(final int value);

    int getButtonColor();

    void setButtonColor(final int value);

    int getButtonColorRimLight();

    void setButtonColorRimLight(final int value);

    int getButtonColorRimShadow();

    void setButtonColorRimShadow(final int value);
}
