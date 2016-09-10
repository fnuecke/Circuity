package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.controller.AddressMapper;

/**
 * May be implemented on {@link Addressable}s to give the {@link AddressMapper}
 * some hints when distributing addresses to connected devices. This should be
 * implemented by devices that prefer or require to be mapped to a specific
 * address range.
 * <p>
 * When implemented this allows the controller to assign such devices first,
 * making sure no devices that actually don't care get in their way, as well as
 * mapping devices implementing this interface in an order where they will not
 * conflict with each other (unless the address space they require being mapped
 * to has been used up).
 */
public interface AddressHint extends BusDevice {
    /**
     * The sorting order of the device.
     * <p>
     * Devices are mapped in order of increasing value. For example, a device
     * with sort hint <code>5</code> will be mapped before a device with a value
     * of <code>10</code>.
     * <p>
     * Devices <em>not</em> implementing this interface will be mapped after all
     * devices implementing the interface have been mapped.
     * <p>
     * A basic convention is to provide the memory address at or after which
     * this device prefers to be mounted.
     *
     * @return the sorting order of this device.
     */
    int getSortHint();
}
