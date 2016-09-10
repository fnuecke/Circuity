package li.cil.circuity.api.bus;

import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.controller.Subsystem;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Implemented by bus controllers responsible for managing a bus.
 * <p>
 * Bus controllers take care of mapping {@link Addressable}s to non-overlapping
 * {@link AddressBlock}s and for assigning interrupts to devices implementing
 * the {@link InterruptSource} and {@link InterruptSink} interfaces.
 * <p>
 * The bus controller determines the bus widths of the bus it is connected to.
 * <p>
 * When scanning for {@link BusDevice}s, the controller follows all {@link BusSegment}s
 * to collect the list of all connected devices. The controller itself also
 * acts as a cable-like bus segment, and is used as the "seed" when scanning.
 */
public interface BusController extends BusSegment {
    /**
     * Get whether the bus controller is currently online.
     * <p>
     * Note that this may return <code>false</code> even if devices implementing
     * {@link BusStateListener} were notified of the bus controller going online if
     * the bus controller is currently in an errored state after scanning for devices.
     * <p>
     * This method is thread safe.
     *
     * @return <code>true</code> if the bus controller is online; <code>false</code> otherwise.
     * @see BusStateListener
     */
    boolean isOnline();

    /**
     * Schedule re-scanning the bus.
     * <p>
     * This does not happen immediately, rather it typically happens in the next
     * frame. In particular, devices cannot immediately remove themselves from
     * the bus. They must continue operating until the bus clears itself from
     * them via {@link BusDevice#setBusController(BusController)}.
     * <p>
     * This method is thread safe.
     */
    void scheduleScan();

    /**
     * Get the subsystem with the specified type.
     * <p>
     * Note that this must be the exact type, e.g. {@link AddressMapper} for
     * the address mapping subsystem. Passing the type of the implementation
     * will return <code>null</code>.
     *
     * @param subsystem the type of the subsystem to retrieve.
     * @param <T>       the generic type of the subsystem to retrieve.
     * @return the subsystem of this bus.
     */
    <T extends Subsystem> T getSubsystem(final Class<T> subsystem);

    // --------------------------------------------------------------------- //

    /**
     * Get the list of all devices currently connected to the controller.
     * <p>
     * This will return an empty list if the controller is currently in an
     * errored state.
     *
     * @return the list of all connected bus devices.
     */
    Iterable<BusDevice> getDevices();

    /**
     * Fast lookup of a device by its persistent globally unique identifier.
     *
     * @param persistentId the ID of the device to get.
     * @return the device with the specified ID if it is connected to the controller; <code>null</code> otherwise.
     */
    @Nullable
    BusDevice getDevice(final UUID persistentId);
}
