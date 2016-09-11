package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.BusDevice;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Keeps track of {@link li.cil.circuity.api.bus.BusDevice}s connected to the
 * bus and allows ID based lookup of devices.
 */
public interface DeviceMapper extends Subsystem {
    /**
     * Fast lookup of a device by its persistent globally unique identifier.
     *
     * @param persistentId the ID of the device to get.
     * @return the device with the specified ID if it is connected to the controller; <code>null</code> otherwise.
     */
    @Nullable
    BusDevice getDevice(final UUID persistentId);
}
