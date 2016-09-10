package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;

/**
 * When implemented on a {@link BusDevice}, it may be used by a screen to
 * render some content via this device.
 */
public interface ScreenRenderer extends BusDevice {
    /**
     * Called when the device should render to a screen.
     * <p>
     * The GL state will have been set up such that the device should render
     * its content on the XZ-plane, in a rectangle of the specified size.
     *
     * @param width  the available width for rendering.
     * @param height the available height for rendering.
     */
    void render(final int width, final int height);
}
