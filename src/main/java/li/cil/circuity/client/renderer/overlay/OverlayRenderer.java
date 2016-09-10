package li.cil.circuity.client.renderer.overlay;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public final class OverlayRenderer {
    private static final float TWO_PI = 2 * (float) Math.PI;

    /**
     * Get the current, time dependent, alpha for all pulsing overlays
     * using the default seed.
     *
     * @return the current overlay alpha.
     */
    public static float getPulseAlpha() {
        return getPulseAlpha(0);
    }

    /**
     * Get the current, time dependent, alpha for pulsing overlays using
     * the specified seed.
     *
     * @return the current overlay alpha.
     */
    public static float getPulseAlpha(final long seed) {
        // Sine wave, frequency of 2/3hz.
        final float phase = ((System.currentTimeMillis() + seed) % 3000) / 3000f;
        final float normalized = (MathHelper.sin(phase * TWO_PI) + 1) * 0.5f;
        return 0.75f + 0.25f * normalized;
    }

    /**
     * Render a block overlay.
     * <p>
     * This will render the top, bottom and side overlays for a block. It uses
     * the currently bound texture. It will adjust the color value to enforce
     * the current pulse alpha.
     *
     * @param atlas   the texture atlas to look up the overlay sprites in.
     * @param overlay the overlay info.
     */
    public static void renderOverlay(final TextureMap atlas, final OverlayData overlay) {
        GlStateManager.depthMask(false);
        GlStateManager.color(overlay.alpha, overlay.alpha, overlay.alpha);

        final Tessellator tessellator = Tessellator.getInstance();
        final VertexBuffer buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        renderOverlayStatic(atlas, overlay.overlay, buffer);

        tessellator.draw();

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.depthMask(true);
    }

    public static void renderOverlayStatic(final TextureMap atlas, final Overlay overlay, final VertexBuffer buffer) {
        final float l = -0.002f, h = 1.002f;
        if (overlay.top != null) {
            final TextureAtlasSprite sprite = atlas.getAtlasSprite(overlay.top.toString());
            buffer.pos(l, h, l).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            buffer.pos(l, h, h).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
            buffer.pos(h, h, h).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(h, h, l).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        }

        if (overlay.side != null) {
            final TextureAtlasSprite sprite = atlas.getAtlasSprite(overlay.side.toString());
            buffer.pos(l, l, l).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(l, h, l).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(h, h, l).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            buffer.pos(h, l, l).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();

            buffer.pos(h, l, l).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(h, h, l).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(h, h, h).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            buffer.pos(h, l, h).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();

            buffer.pos(h, l, h).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(h, h, h).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(l, h, h).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            buffer.pos(l, l, h).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();

            buffer.pos(l, l, h).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(l, h, h).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(l, h, l).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            buffer.pos(l, l, l).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        }

        if (overlay.bottom != null) {
            final TextureAtlasSprite sprite = atlas.getAtlasSprite(overlay.bottom.toString());
            buffer.pos(l, l, l).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
            buffer.pos(h, l, l).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(h, l, h).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(l, l, h).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        }
    }

    // --------------------------------------------------------------------- //

    private OverlayRenderer() {
    }
}
