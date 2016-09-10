package li.cil.circuity.api.bus;

import javax.annotation.Nullable;

/**
 * The minimal interface for an object that can be connected to the bus.
 */
public interface BusElement {
    /**
     * Get the bus controller the device is currently assigned to.
     *
     * @return the controller the the device is connected to.
     */
    @Nullable
    BusController getBusController();

    /**
     * Called when this device is connected to or disconnected from a {@link BusController}.
     * <p>
     * When <code>controller</code> is non-<code>null</code>, this means that
     * the device is now connected to a bus with a single {@link BusController}.
     * This will always be called before further configuration of the device by
     * subsystems of the {@link BusController}.
     * <p>
     * When <code>controller</code> is <code>null</code>, this means the device
     * has been disconnected from the {@link BusController} it was connected to.
     * <p>
     * This should <em>never</em> be called by anything other than the
     * {@link BusController} of the bus this device is/was connected to.
     *
     * @param controller the controller of the bus the device is connected to.
     */
    void setBusController(@Nullable final BusController controller);
}
