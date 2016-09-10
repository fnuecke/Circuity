package li.cil.circuity.client.renderer.overlay;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Utility structure for the list of overlay textures for a single block.
 */
public final class Overlay {
    public final ResourceLocation top, side, bottom;

    public Overlay(@Nullable final ResourceLocation top, @Nullable final ResourceLocation side, @Nullable final ResourceLocation bottom) {
        this.top = top;
        this.side = side;
        this.bottom = bottom;
    }

    public OverlayData get() {
        return new OverlayData(this, OverlayRenderer.getPulseAlpha());
    }

    public OverlayData get(final float alpha) {
        return new OverlayData(this, alpha);
    }
}
