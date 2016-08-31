package li.cil.circuity.client.gui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.common.ecs.component.BusDeviceScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public final class GuiScreenImpl extends GuiScreen {
    // --------------------------------------------------------------------- //
    // Settings

    static final int SCREEN_RES_PIXELS_W = 320;
    static final int SCREEN_RES_PIXELS_H = 200;
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
    // GuiScreen

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
        if (innerW * SCREEN_RES_PIXELS_H > innerH * SCREEN_RES_PIXELS_W) {
            innerW = (innerH * SCREEN_RES_PIXELS_W) / SCREEN_RES_PIXELS_H;
        } else {
            innerH = (innerW * SCREEN_RES_PIXELS_H) / SCREEN_RES_PIXELS_W;
        }

        // Get screen rect size
        final int innerBorderW = innerW + INNER_BORDER_SIZE;
        final int innerBorderH = innerH + INNER_BORDER_SIZE;

        // Get outer size
        final int outerW = innerBorderW + SCREEN_BORDER_SIZE;
        final int outerH = innerBorderH + SCREEN_BORDER_SIZE;

        // Set up GL state
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);

        // Start drawing frame
        final Tessellator tessellator = Tessellator.getInstance();
        final VertexBuffer buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // Outer rectangle
        buffer.pos(scx - outerW / 2f, scy - outerH / 2f, 0f).color(0.8f, 0.7f, 0.6f, 1f).endVertex();
        buffer.pos(scx - outerW / 2f, scy + outerH / 2f, 0f).color(0.8f, 0.7f, 0.6f, 1f).endVertex();
        buffer.pos(scx + outerW / 2f, scy + outerH / 2f, 0f).color(0.8f, 0.7f, 0.6f, 1f).endVertex();
        buffer.pos(scx + outerW / 2f, scy - outerH / 2f, 0f).color(0.8f, 0.7f, 0.6f, 1f).endVertex();

        // Inner rectangle (actual screen)
        buffer.pos(scx - innerBorderW / 2f, scy - innerBorderH / 2f, 0f).color(0f, 0f, 0f, 1f).endVertex();
        buffer.pos(scx - innerBorderW / 2f, scy + innerBorderH / 2f, 0f).color(0f, 0f, 0f, 1f).endVertex();
        buffer.pos(scx + innerBorderW / 2f, scy + innerBorderH / 2f, 0f).color(0f, 0f, 0f, 1f).endVertex();
        buffer.pos(scx + innerBorderW / 2f, scy - innerBorderH / 2f, 0f).color(0f, 0f, 0f, 1f).endVertex();

        // Finish drawing frame
        tessellator.draw();

        // Clean up GL state
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Draw screen
        final ScreenRenderer renderer = screen.getScreenRenderer();
        if (renderer != null) {
            GlStateManager.color(1f, 1f, 1f);
            GlStateManager.translate((scx - innerW / 2f), (scy - innerH / 2f), 0f);
            renderer.render(innerW, innerH);
            GlStateManager.translate(-(scx - innerW / 2f), -(scy - innerH / 2f), 0f);
        }
    }

    @Override
    protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        final ByteBuf data = Unpooled.buffer(1);
        data.writeByte((byte) typedChar);
        screen.sendData(data);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
