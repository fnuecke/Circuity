package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;

/**
 * When implemented on a {@link li.cil.circuity.api.bus.BusDevice} it will be
 * notified by the {@link li.cil.circuity.api.bus.BusController} whenever its
 * power state changes.
 * <p>
 * After a bus goes online it will start updating {@link AsyncTickable}s.
 * Devices will usually want to use this to initialize some base state that
 * depends on other devices. When the bus goes offline, devices should cease
 * all activity and reset their volatile state.
 */
public interface BusStateAware extends BusDevice {
    /**
     * Called when the bus goes online.
     * <p>
     * Note that this is only called if the device is connected to a controller
     * at the time it goes online. If the device is connected to a bus with an
     * already online controller, this will not be called.
     * <p>
     * For the default bus controller this is when its ingoing redstone signal
     * changes to a high signal (i.e. non-zero).
     */
    void handleBusOnline();

    /**
     * Called when the bus goes offline.
     * <p>
     * Note that this is only called if the device is connected to a controller
     * at the time it goes offline. If the device is disconnected from a bus
     * with an online controller, this will not be called.
     * <p>
     * For the default bus controller this is when its ingoing redstone signal
     * changes to a low signal (i.e. zero).
     */
    void handleBusOffline();
}
