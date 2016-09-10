package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.BusElement;

/**
 * A subsystem is a distinct module of functionality of a bus controller.
 * <p>
 * Default subsystems include the {@link AddressMapper} and the {@link InterruptMapper}.
 */
public interface Subsystem {
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

    /**
     * Called after a scan to determine if the subsystem is in a valid state.
     * <p>
     * It is guaranteed that this gets called if there were any previous calls
     * to {@link #add(BusElement)} or {@link #remove(BusElement)}, i.e. this
     * method can be used for post-processing.
     *
     * @return <code>true</code> if the subsystem is operational; <code>false</code> otherwise.
     */
    boolean validate();

    /**
     * Called when the bus controller is dismantled.
     * <p>
     * This is used to perform any potentially necessary cleanup in subsystems.
     */
    void dispose();
}
