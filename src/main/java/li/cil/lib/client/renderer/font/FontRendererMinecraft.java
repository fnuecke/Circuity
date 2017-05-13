package li.cil.lib.client.renderer.font;

import com.google.common.base.Charsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class FontRendererMinecraft implements FontRenderer {
    public static final FontRenderer INSTANCE = new FontRendererMinecraft();

    private static final ResourceLocation FONT_LOCATION = new ResourceLocation("textures/font/ascii.png");
    private static final String FONT_CHARS = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

    // --------------------------------------------------------------------- //
    // FontRenderer

    @Override
    public void drawString(final String string) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(FONT_LOCATION);

        GlStateManager.pushMatrix();

        for (int i = 0; i < string.length(); i++) {
            final int w = renderChar(string.charAt(i));
            GlStateManager.translate(w, 0, 0);
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void drawString(final String string, final int maxWidth) {
        drawString(getFontRenderer().trimStringToWidth(string, maxWidth));
    }

    @Override
    public void drawString(final byte[] chars, final int offset, final int length) {
        drawString(new String(chars, offset, length, Charsets.US_ASCII));
    }

    @Override
    public int getCharWidth(final char character) {
        return getFontRenderer().getCharWidth(character);
    }

    @Override
    public int getCharHeight() {
        return getFontRenderer().FONT_HEIGHT;
    }

    @Override
    public int getStringWidth(final String string) {
        return getFontRenderer().getStringWidth(string);
    }

    // --------------------------------------------------------------------- //

    private static net.minecraft.client.gui.FontRenderer getFontRenderer() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    // This is basically FontRenderer#renderDefaultChar.
    private static int renderChar(final char ch) {
        final int charIndex = FONT_CHARS.indexOf(ch);
        if (charIndex < 0) {
            return 0;
        }
        final int width = getFontRenderer().getCharWidth(ch);
        if (width < 1) {
            return 0;
        }

        final int uIndex = charIndex % 16 * 8;
        final int vIndex = charIndex / 16 * 8;
        final float u = uIndex / 128f;
        final float v = vIndex / 128f;
        final float sx = width - 1.01f;
        final float sy = getFontRenderer().FONT_HEIGHT - 1.01f;
        final float su = sx / 128f;
        final float sv = sy / 128f;

        final Tessellator tessellator = Tessellator.getInstance();
        final VertexBuffer buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(0, 0, 0).tex(u, v).endVertex();
        buffer.pos(0, sy, 0).tex(u, v + sv).endVertex();
        buffer.pos(sx, sy, 0).tex(u + su, v + sv).endVertex();
        buffer.pos(sx, 0, 0).tex(u + su, v).endVertex();

        tessellator.draw();

        return width;
    }

    // --------------------------------------------------------------------- //

    private FontRendererMinecraft() {
    }
}
