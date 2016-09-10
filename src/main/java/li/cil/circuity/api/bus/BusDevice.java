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
 * typically want to implement {@link BusConnector}, {@link Addressable},
 * {@link InterruptSink} and/or {@link InterruptSource} for more functionality.
 */
public interface BusDevice extends BusElement {
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
}
