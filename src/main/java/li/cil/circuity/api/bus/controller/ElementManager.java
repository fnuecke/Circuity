package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;

/**
 * Specialized interface for element managing subsystems.
 * <p>
 * The main reason this is split out of the base interface is to avoid people
 * accidentally calling these methods from anywhere but the {@link BusController}
 * managing the subsystem.
 */
public interface ElementManager extends Subsystem {
    /**
     * Called while scanning, when a new devices has been found.
     *
     * @param element the device that has been connected to the bus.
     */
    void add(final BusElement element);

    /**
     * Called while scanning, when a device has gone missing.
     *
     * @param element the device that has been disconnected from the bus.
     */
    void remove(final BusElement element);
}
