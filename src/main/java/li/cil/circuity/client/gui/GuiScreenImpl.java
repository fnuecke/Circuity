package li.cil.circuity.client.gui;

import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.common.ecs.component.BusDeviceScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public final class GuiScreenImpl extends GuiScreen {
    // --------------------------------------------------------------------- //
    // Settings

    static final int WINDOW_GAP_SIZE = 5;
    static final int SCREEN_BORDER_SIZE = 10;
    static final int INNER_BORDER_SIZE = 5;

    // --------------------------------------------------------------------- //

    private final BusDeviceScreen screen;

    // --------------------------------------------------------------------- //

    public GuiScreenImpl(final BusDeviceScreen screen) {
        this.screen = screen;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void drawScreen(final int mx, final int my, final float unk1) {
        super.drawScreen(mx, my, unk1);

        // Get GUI dimensions and center
        final int sw = this.width;
        final int sh = this.height;
        final int scx = sw / 2;
        final int scy = sh / 2;

        // Get inner size
        int innerW = sw - WINDOW_GAP_SIZE - SCREEN_BORDER_SIZE - INNER_BORDER_SIZE;
        int innerH = sh - WINDOW_GAP_SIZE - SCREEN_BORDER_SIZE - INNER_BORDER_SIZE;

        // Shrink to fit aspect ratio
        if (innerW * BusDeviceScreen.SCREEN_RES_PIXELS_H > innerH * BusDeviceScreen.SCREEN_RES_PIXELS_W) {
            innerW = (innerH * BusDeviceScreen.SCREEN_RES_PIXELS_W) / BusDeviceScreen.SCREEN_RES_PIXELS_H;
        } else {
            innerH = (innerW * BusDeviceScreen.SCREEN_RES_PIXELS_H) / BusDeviceScreen.SCREEN_RES_PIXELS_W;
        }

        // Get screen rect size
        final int innerBorderW = innerW + INNER_BORDER_SIZE;
        final int innerBorderH = innerH + INNER_BORDER_SIZE;

        // Get outer size
        final int outerW = innerBorderW + SCREEN_BORDER_SIZE;
        final int outerH = innerBorderH + SCREEN_BORDER_SIZE;

        // Set up GL state
        GlStateManager.pushAttrib();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);

        // Start drawing frame
        GlStateManager.glBegin(GL11.GL_QUADS);

        // Outer rectangle
        GlStateManager.color(0.8f, 0.7f, 0.6f);
        GlStateManager.glVertex3f(scx - outerW / 2f, scy - outerH / 2f, 0f);
        GlStateManager.glVertex3f(scx - outerW / 2f, scy + outerH / 2f, 0f);
        GlStateManager.glVertex3f(scx + outerW / 2f, scy + outerH / 2f, 0f);
        GlStateManager.glVertex3f(scx + outerW / 2f, scy - outerH / 2f, 0f);

        // Inner rectangle (actual screen)
        GlStateManager.color(0.0f, 0.0f, 0.0f);
        GlStateManager.glVertex3f(scx - innerBorderW / 2f, scy - innerBorderH / 2f, 0f);
        GlStateManager.glVertex3f(scx - innerBorderW / 2f, scy + innerBorderH / 2f, 0f);
        GlStateManager.glVertex3f(scx + innerBorderW / 2f, scy + innerBorderH / 2f, 0f);
        GlStateManager.glVertex3f(scx + innerBorderW / 2f, scy - innerBorderH / 2f, 0f);

        // Finish drawing frame
        GlStateManager.glEnd();

        // Clean up GL state
        GlStateManager.enableTexture2D();
        GlStateManager.popAttrib();

        // Draw screen
        final ScreenRenderer renderer = screen.getScreenRenderer();
        if (renderer != null) {
            GlStateManager.translate((scx - innerW / 2f), (scy - innerH / 2f), 0f);
            renderer.render(innerW, innerH);
            GlStateManager.translate(-(scx - innerW / 2f), -(scy - innerH / 2f), 0f);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
