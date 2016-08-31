package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;

import java.util.UUID;

/**
 * Implementing this allows {@link BusDevice}s to be referenced in a robust
 * and persistable manner, usually by other bus devices.
 * <p>
 * The contract is that the device must <em>always</em> return the same ID
 * from {@link #getPersistentId()}. The {@link li.cil.circuity.api.bus.BusController}
 * allows fast and direct lookup of connected bus devices with a persistable
 * ID via {@link li.cil.circuity.api.bus.BusController#getDevice(UUID)}.
 */
public interface PersistentIdentifiable extends BusDevice {
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
}
