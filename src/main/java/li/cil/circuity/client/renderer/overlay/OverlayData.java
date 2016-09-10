package li.cil.circuity.client.renderer.overlay;

/**
 * Additional overlay specific state for a specific overlay.
 */
public class OverlayData {
    /**
     * The actual overlay.
     */
    public final Overlay overlay;

    /**
     * The alpha value to render the overlay at.
     */
    public final float alpha;

    public OverlayData(final Overlay overlay, final float alpha) {
        this.overlay = overlay;
        this.alpha = alpha;
    }
}
