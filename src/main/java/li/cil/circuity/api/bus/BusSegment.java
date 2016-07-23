package li.cil.circuity.api.bus;

/**
 * Represents a transitive part of a bus.
 * <p>
 * Typically there are two use cases where this interface gets implemented.
 * The first is simple bus cables. In that case, {@link #getDevices()} will
 * return the list of neighboring <em>segments and devices</em>.
 * <p>
 * The other use case are compound devices. In that case, {@link #getDevices()}
 * will return the list of only <em>internal devices</em>.
 * <p>
 * It is up to the implementer to decide what to do with this, and whether to
 * stick to these recommendations or not.
 * <p>
 * To ensure correct operation of the bus, bus segments must call {@link BusController#scheduleScan()}
 * when:
 * <ul>
 * <li>their list of neighboring devices changes.</li>
 * <li>they are removed from the world.</li>
 * </ul>
 * There is no need to call this method when being added to the world, as new
 * segments will be detected via the block change by either adjacent bus
 * segments or an adjacent bus controller.
 */
public interface BusSegment extends BusDevice {
    /**
     * The list of adjacent {@link BusDevice}s.
     *
     * @return the bus devices adjacent to this bus segment.
     */
    Iterable<BusDevice> getDevices();
}
