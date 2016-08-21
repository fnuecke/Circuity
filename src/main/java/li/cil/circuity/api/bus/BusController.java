package li.cil.circuity.api.bus;

import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;

import javax.annotation.Nullable;
import java.util.PrimitiveIterator;

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
     * {@link BusStateAware} were notified of the bus controller going online if
     * the bus controller is currently in an errored state after scanning for devices.
     * <p>
     * This method is thread safe.
     *
     * @return <code>true</code> if the bus controller is online; <code>false</code> otherwise.
     * @see BusStateAware
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

    // --------------------------------------------------------------------- //

    /**
     * Get the address block the specified device is currently assigned to.
     * <p>
     * This allows retrieving the authoritative address from the bus, and avoids
     * having to call {@link Addressable#getMemory(AddressBlock)} in a potentially
     * unknown state.
     *
     * @param device the device to get the address for.
     * @return the address of that device.
     */
    @Nullable
    AddressBlock getAddress(final Addressable device);

    /**
     * Write a value to the specified global address.
     * <p>
     * This will find the device mapped to the specified address, transform the
     * address to an address local to the device, and then write the value at
     * that local address to the device via {@link Addressable#write(int, int)}.
     * <p>
     * The address must fit into the address width supported by the bus
     * controller, otherwise an {@link IndexOutOfBoundsException} will be
     * thrown.
     * <p>
     * The value must fit into the data width supported by the bus controller,
     * otherwise you have to assume it may get truncated at some point.
     * <p>
     * Note that calling this method while {@link #isOnline()} returns <code>false</code>
     * is an illegal operation. Either check before calling this, or, preferably,
     * only call this while processing a call from the bus controller. This
     * includes {@link AsyncTickable#updateAsync()}, {@link Addressable#read(int)},
     * {@link Addressable#write(int, int)} and {@link InterruptSink#interrupt(int, int)}.
     * Doing so will lead to undefined behavior. No exception is thrown
     * <p>
     * This method is <em>not</em> thread safe. It must only be called while in
     * a callback initiated from the bus controller (as listed in the previous
     * paragraph).
     *
     * @param address the global address to write to.
     * @param value   the value to write.
     * @throws IndexOutOfBoundsException if the address is unsupported.
     */
    void mapAndWrite(final int address, final int value) throws IndexOutOfBoundsException;

    /**
     * Read a value from the specified global address.
     * <p>
     * This will find the device mapped to the specified address, transform the
     * address to an address local to the device, and then read a value at
     * that local address from the device via {@link Addressable#read(int)}.
     * <p>
     * The address must fit into the address width supported by the bus
     * controller, otherwise an {@link IndexOutOfBoundsException} will be
     * thrown.
     * <p>
     * Note that calling this method while {@link #isOnline()} returns <code>false</code>
     * is an illegal operation. Either check before calling this, or, preferably,
     * only call this while processing a call from the bus controller. This
     * includes {@link AsyncTickable#updateAsync()}, {@link Addressable#read(int)},
     * {@link Addressable#write(int, int)} and {@link InterruptSink#interrupt(int, int)}.
     * <p>
     * This method is <em>not</em> thread safe. It must only be called while in
     * a callback initiated from the bus controller (as listed in the previous
     * paragraph).
     *
     * @param address the global address to read from.
     * @return the value read.
     * @throws IndexOutOfBoundsException if the address is unsupported.
     */
    int mapAndRead(final int address) throws IndexOutOfBoundsException;

    // --------------------------------------------------------------------- //

    /**
     * Get the list of interrupt IDs the specified source has currently assigned.
     * <p>
     * This allows retrieving the authoritative list from the bus, and avoids
     * having to call {@link InterruptSource#getEmittedInterrupts(InterruptList)}
     * in a potentially unknown state.
     * <p>
     * Note that this method is not very efficient. Do not call this frequently.
     *
     * @param device the interrupt source to get the interrupt IDs for.
     * @return the list of interrupt IDs the source has assigned to it.
     */
    PrimitiveIterator.OfInt getInterruptSourceIds(final InterruptSource device);

    /**
     * Get the list of interrupt IDs the specified sink has currently assigned.
     * <p>
     * This allows retrieving the authoritative list from the bus, and avoids
     * having to call {@link InterruptSink#getAcceptedInterrupts(InterruptList)}
     * in a potentially unknown state.
     * <p>
     * Note that this method is not very efficient. Do not call this frequently.
     *
     * @param device the interrupt sink to get the interrupt IDs for.
     * @return the list of interrupt IDs the sink has assigned to it.
     */
    PrimitiveIterator.OfInt getInterruptSinkIds(final InterruptSink device);

    /**
     * Triggers the interrupt with the specified ID, passing along the specified data.
     * <p>
     * Whether or not the data is actually used depends on the interrupted
     * device.
     * <p>
     * This should only be called by {@link InterruptSource}s, passing one of
     * the interrupt source IDs that has been assigned to them. Passing any
     * other IDs, while possible, is strongly discouraged as it will lead to
     * unexpected behavior.
     * <p>
     * Note that calling this method while {@link #isOnline()} returns <code>false</code>
     * is an illegal operation. Either check before calling this, or, preferably,
     * only call this while processing a call from the bus controller. This
     * includes {@link AsyncTickable#updateAsync()}, {@link Addressable#read(int)},
     * {@link Addressable#write(int, int)} and {@link InterruptSink#interrupt(int, int)}.
     * <p>
     * This method is thread safe.
     *
     * @param interruptId the ID of the interrupt to trigger.
     * @param data        the data to pass along.
     */
    void interrupt(final int interruptId, final int data);
}
