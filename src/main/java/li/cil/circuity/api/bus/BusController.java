package li.cil.circuity.api.bus;

/**
 * Implemented by bus controllers responsible for managing a bus.
 * <p>
 * Bus controllers take care of mapping {@link li.cil.circuity.api.bus.device.Addressable}s
 * to non-overlapping {@link AddressBlock}s and for assigning interrupts to
 * devices implementing the {@link li.cil.circuity.api.bus.device.InterruptSource}
 * and {@link li.cil.circuity.api.bus.device.InterruptSink} interfaces.
 * <p>
 * The bus controller determines the bus widths of the bus it is connected to.
 * <p>
 * When scanning for {@link BusDevice}s, the controller follows all {@link BusSegment}s
 * to collect the list of all connected devices. The controller itself also
 * acts as a cable-like bus segment, and is used as the "seed" when scanning.
 */
public interface BusController extends BusSegment {
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
     * Starts an update cycle.
     * <p>
     * This will trigger a worker thread which will then update all
     * {@link li.cil.circuity.api.bus.device.AsyncTickable} bus devices, if
     * and only if the bus controller is in a legal state (i.e. no two bus
     * controllers connected to the same bus).
     * <p>
     * This method is <em>not</em> thread safe. It is expected to be called
     * from the server thread only.
     */
    void startUpdate();

    /**
     * Finish an update cycle.
     * <p>
     * Waits for the worker thread to complete updating all devices on the bus.
     * This way, even though updates are running in parallel, they are still
     * kept in sync with the server update loop, which is particularly useful
     * to avoid non-deterministic behavior when saving.
     * <p>
     * This method is <em>not</em> thread safe. It is expected to be called
     * from the server thread only.
     */
    void finishUpdate();

    /**
     * Write a value to the specified global address.
     * <p>
     * This will find the device mapped to the specified address, transform the
     * address to an address local to the device, and then write the value at
     * that local address to the device via {@link li.cil.circuity.api.bus.device.Addressable#write(int, int)}.
     * <p>
     * The address must fit into the address width supported by the bus
     * controller, otherwise an {@link IndexOutOfBoundsException} will be
     * thrown.
     * <p>
     * The value must fit into the data width supported by the bus controller,
     * otherwise you have to assume it may get truncated at some point.
     * <p>
     * This method is thread safe.
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
     * that local address from the device via {@link li.cil.circuity.api.bus.device.Addressable#read(int)}.
     * <p>
     * The address must fit into the address width supported by the bus
     * controller, otherwise an {@link IndexOutOfBoundsException} will be
     * thrown.
     * <p>
     * This method is thread safe.
     *
     * @param address the global address to read from.
     * @return the value read.
     * @throws IndexOutOfBoundsException if the address is unsupported.
     */
    int mapAndRead(final int address) throws IndexOutOfBoundsException;
}
