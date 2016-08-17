package li.cil.circuity.api.bus;

import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;

import javax.annotation.Nullable;

/**
 * Represents a single device.
 * <p>
 * Typically, cables will look for tile entities providing a capability of this
 * type, or tile entities that directly implement this interface (where the
 * former is the preferred method).
 * <p>
 * Note that on its own, this interface is only of limited usefulness. You'll
 * typically want to implement {@link BusSegment}, {@link Addressable},
 * {@link InterruptSink} and/or {@link InterruptSource} for more functionality.
 */
public interface BusDevice {
    /**
     * Called when this device is connected to or disconnected from a bus controller.
     * <p>
     * When <code>controller</code> is non-<code>null</code>, this merely means
     * that the device is now connected to a bus with a single {@link BusController}.
     * This will always be called before further configuration of the device by
     * the bus controller, e.g. via {@link Addressable#setMemory(AddressBlock)},
     * {@link InterruptSource#setEmittedInterrupts(int[])} or {@link InterruptSink#setAcceptedInterrupts(int[])}.
     * <p>
     * When <code>controller</code> is <code>null</code>, this means the device
     * has been disconnected from the bus controller it was connected to.
     * <p>
     * This should <em>never</em> be called by anything other than the
     * {@link BusController} of the bus this device is/gets connected to.
     *
     * @param controller the controller of the bus the device is connected to.
     */
    void setBusController(@Nullable final BusController controller);
}
