package li.cil.circuity.api.bus;

import java.util.Collection;

/**
 * Represents a transitive part of a bus.
 * <p>
 * Typically there are two use cases where this interface gets implemented.
 * The first is simple bus cables. In that case, {@link #getConnected(Collection)} will
 * return the list of neighboring <em>connectors and devices</em>.
 * <p>
 * The other use case are compound devices. In that case, {@link #getConnected(Collection)}
 * will return the list of only <em>internal devices</em>.
 * <p>
 * It is up to the implementer to decide what to do with this, and whether to
 * stick to these recommendations or not.
 * <p>
 * To ensure correct operation of the bus, bus connectors must call {@link BusController#scheduleScan()}
 * when:
 * <ul>
 * <li>their list of neighboring devices changes.</li>
 * <li>they are removed from the world.</li>
 * </ul>
 * There is no need to call this method when being added to the world, as new
 * connectors will be detected via the block change by either adjacent bus
 * connectors or an adjacent bus controller.
 */
public interface BusConnector extends BusElement {
    /**
     * Build the list of adjacent {@link BusDevice}s.
     * <p>
     * There are cases where this may fail, for example due to a neighboring
     * block not being loaded (and we don't want to cause chunk loads by
     * scanning). In these cases, this method may return <code>false</code>,
     * to notify the scanning {@link BusController} to abort the scan and
     * retry again later.
     *
     * @param elements the list into which to put the bus elements this bus connector provides.
     * @return <code>true</code> on success; <code>false</code> on error,
     * causing the the bus controller to rescan again at later point in time.
     */
    boolean getConnected(final Collection<BusElement> elements);
}
