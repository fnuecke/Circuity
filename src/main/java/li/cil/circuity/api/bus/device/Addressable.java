package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.controller.AddressMapper;

/**
 * Implemented on devices that can be mapped to an address block on the bus.
 * <p>
 * This allows other devices to read from and write to the device.
 */
public interface Addressable extends BusDevice {
    /**
     * Get the preferred address block of this device.
     * <p>
     * This passed address block represents the address <em>range</em> in which
     * the {@link AddressMapper} tries to allocate an address for this device.
     * Unless this device must be at a specific address, this should return an
     * address block at the start of the specified block. Note that the user
     * may always choose to remap the device to whichever address they please.
     * <p>
     * Note that the specified address block may also be too small to fit this
     * device. This can typically ignored (only offset validation is required).
     * If the resulting block overlaps with another, it is the users
     * responsibility to resolve any conflicts via the address mapper.
     *
     * @param memory the address range to preferably select a location.
     * @return the address block at which this device would like to be mapped.
     */
    AddressBlock getPreferredAddressBlock(final AddressBlock memory);

    /**
     * Reads a value from this device from the specified local address.
     *
     * @param address the local address to read from.
     * @return the value read.
     */
    int read(final int address);

    /**
     * Write a value to this device to the specified local address.
     *
     * @param address the local address to write to.
     * @param value   the value to write to the device.
     */
    void write(final int address, final int value);
}
