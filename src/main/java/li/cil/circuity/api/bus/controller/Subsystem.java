package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.BusDevice;

/**
 * A subsystem is a distinct module of functionality of a bus controller.
 * <p>
 * Default subsystems include the {@link AddressMapper} and the {@link InterruptMapper}.
 */
public interface Subsystem {
    /**
     * Called while scanning, when a new devices has been found.
     *
     * @param device the device that has been connected to the bus.
     */
    void add(final BusDevice device);

    /**
     * Called while scanning, when a device has gone missing.
     *
     * @param device the device that has been disconnected from the bus.
     */
    void remove(final BusDevice device);

    /**
     * Called after a scan to determine if the subsystem is in a valid state.
     * <p>
     * It is guaranteed that this gets called if there were any previous calls
     * to {@link #add(BusDevice)} or {@link #remove(BusDevice)}, i.e. this
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
