package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;

/**
 * When implemented on a {@link li.cil.circuity.api.bus.BusDevice} it will be
 * notified by the {@link li.cil.circuity.api.bus.BusController} whenever a
 * scan completes.
 * <p>
 * This can be useful when devices wish to dynamically establish a connection
 * to
 */
public interface BusChangeListener extends BusDevice {
    /**
     * Called when the bus finished a scan.
     */
    void handleBusChanged();
}
