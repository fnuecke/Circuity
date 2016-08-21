package li.cil.circuity.api.bus;

import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;

/**
 * This interface provides additional access to a bus controller.
 * <p>
 * It is intended to only be used when changing the configuration of a bus
 * controller.
 */
public interface ConfigurableBusController extends BusController {
    void setDeviceAddress(final Addressable device, final AddressBlock address);

    void setInterruptMapping(final int sourceId, final int sinkId);
}
