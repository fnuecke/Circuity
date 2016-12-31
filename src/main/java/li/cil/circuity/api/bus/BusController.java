package li.cil.circuity.api.bus;

import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.controller.Subsystem;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;

import javax.annotation.Nullable;

/**
 * Implemented by bus controllers responsible for managing a bus.
 * <p>
 * Bus controllers take care of mapping {@link Addressable}s to non-overlapping
 * {@link AddressBlock}s and for assigning interrupts to devices implementing
 * the {@link InterruptSource} and {@link InterruptSink} interfaces.
 * <p>
 * The bus controller determines the bus widths of the bus it is connected to.
 * <p>
 * When scanning for {@link BusDevice}s, the controller follows all {@link BusConnector}s
 * to collect the list of all connected devices. The controller itself also
 * acts as a cable-like bus connector, and is used as the "seed" when scanning.
 */
public interface BusController extends BusConnector {
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
     * Called by sub-systems when their state changes.
     * <p>
     * Such changes typically indicate that the controller requires saving in
     * the next world save, and this method should be implemented such that the
     * container of the controller (e.g. a tile entity) is marked for saving
     * accordingly.
     */
    void markChanged();

    /**
     * Get the subsystem with the specified type.
     * <p>
     * Note that this must be the exact type, e.g. {@link AddressMapper} for
     * the address mapping subsystem. Passing the type of the implementation
     * will return <code>null</code>.
     *
     * @param subsystem the type of the subsystem to retrieve.
     * @param <T>       the generic type of the subsystem to retrieve.
     * @return the subsystem of this bus, or <code>null</code> if no such system exists.
     */
    @Nullable
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
    Iterable<BusElement> getElements();
}
