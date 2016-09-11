package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.BusElement;

/**
 * A subsystem is a distinct module of functionality of a bus controller.
 * <p>
 * Default subsystems include the {@link AddressMapper} and the {@link InterruptMapper}.
 * <p>
 * None of the methods in this interface are to be called by anything but the
 * bus controller the subsystem belongs to. Doing so will lead to undefined
 * behavior.
 */
public interface Subsystem {
    /**
     * Called after a scan to determine if the subsystem is in a valid state.
     * <p>
     * It is guaranteed that this gets called if there were any previous calls
     * to {@link ElementManager#add(BusElement)} or {@link ElementManager#remove(BusElement)},
     * i.e. this method can be used for post-processing.
     *
     * @return <code>true</code> if the subsystem is operational; <code>false</code> otherwise.
     */
    boolean validate();
}
