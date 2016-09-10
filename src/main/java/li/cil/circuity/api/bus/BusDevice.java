package li.cil.circuity.api.bus;

import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;

import javax.annotation.Nullable;
import java.util.UUID;

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
     * The globally unique, persistent ID of this device.
     * <p>
     * This must always return the same value, even across saving and loading.
     * The recommended approach is to generate a new UUID upon construction,
     * then storing and persisting it as necessary.
     *
     * @return the ID of this device.
     */
    UUID getPersistentId();

    /**
     * Retrieve meta-information about the device.
     * <p>
     * This is used by the {@link BusController} to provide this information
     * via its serial interface.
     * <p>
     * This is optional, but strongly encouraged. Returning <code>null</code>
     * here will lead to default values being returned by the {@link BusController}
     * when queried.
     *
     * @return the device information for this device.
     */
    @Nullable
    DeviceInfo getDeviceInfo();

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
