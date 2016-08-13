package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;

import javax.annotation.Nullable;

/**
 * Implemented on devices that can be mapped to an address block on the bus.
 * <p>
 * This allows other devices to read from and write to the device. A device
 * should usually allow binding to any address (typically configured by the
 * user though a {@link BusController}), but may force binding to a specific
 * address by returning a fixed offset from {@link #getMemory(AddressBlock)}.
 * The actual address the device is getting bound to is passed to the
 * {@link #setMemory(AddressBlock)} method, called by the {@link BusController}
 * when its configuration is valid.
 * <p>
 * <em>Important</em>: a device is responsible for persisting the address it is
 * bound to after {@link #setMemory(AddressBlock)} has been called with a
 * non-<code>null</code> value, until it is called again with a <code>null</code>
 * value. This is necessary for devices to be still bound to the same location
 * after a save and load, because the {@link BusController} does not store the
 * addresses of the connected devices (because it has no persistable way of
 * referencing devices).
 */
public interface Addressable extends BusDevice {
    /**
     * Retrieve meta-information about the device.
     * <p>
     * This is used by the {@link BusController} to provide this information
     * via its serial interface.
     * <p>
     * This is optional, but strongly encouraged. Returning <code>null</code>
     * here will lead to default values being returned by the {@link BusController}
     * when queried.
     *
     * @return the device information for this device.
     */
    @Nullable
    DeviceInfo getDeviceInfo();

    // --------------------------------------------------------------------- //

    /**
     * Get the address block of this device.
     * <p>
     * This method has two behaviors, depending on whether it is currently
     * bound to a specific address on a bus or not. When not currently bound
     * to an address (the initial state), this is used to validate potential
     * addresses by the {@link BusController}. When bound to an address, i.e.
     * after {@link #setMemory(AddressBlock)} has been called with a non-
     * <code>null</code> value, but before it has been called again with a
     * <code>null</code> falue, this must return the address the device is
     * bound to (i.e. the address that was passed to {@link #setMemory(AddressBlock)}).
     * <p>
     * Furthermore, the device <em>is required to persist the address it is
     * currently bound to</em>.
     * <p>
     * A {@link BusController} can only operate on devices with word size it
     * is currently configured for itself. Devices may support multiple word
     * sizes by adjusting the word size of the returned address block to the
     * word size of the specified address block.
     * <p>
     * This passed address block represents the address <em>range</em> in which
     * the mapper tries to allocate an address for this device. Unless this
     * device must be at a specific address, this should return an address
     * block at the start of the specified block.
     * <p>
     * Thus, the returned value can be used to control what address range this
     * device must be assigned to.
     * <p>
     * Note that the specified address block may also be too small to fit this
     * device. This can typically ignored (only offset validation is required).
     * If the resulting block overlaps with another, it is the users
     * responsibility to resolve any conflicts via the address mapper.
     *
     * @param memory the address range to preferably select a location.
     * @return the address block at which this device would like to be mapped.
     */
    AddressBlock getMemory(final AddressBlock memory);

    /**
     * Set the address for this device.
     * <p>
     * This is called with a non-<code>null</code> value when the {@link BusController}
     * successfully mapped the device in a non-overlapping way. The passed
     * address block has been validated via {@link #getMemory(AddressBlock)}.
     * <p>
     * Alternatively this is called with a <code>null</code> value, to notify
     * the device that it is no longer bound to an address.
     * <p>
     * After being bound to an address, other devices on the same bus may read
     * from and write to the device via the bus controller's {@link BusController#mapAndRead(int)}
     * and {@link BusController#mapAndWrite(int, int)} methods, which will
     * convert the passed address to a local address and call this device's
     * {@link #read(int)} and {@link #write(int, int)} methods.
     * <p>
     * To read from and write to other devices, use the {@link BusController}
     * passed to {@link #setBusController(BusController)}. It is guaranteed
     * that {@link #setBusController(BusController)} will always be called
     * before this method is called.
     * <p>
     * <em>Important</em>: the device must store the passed address and return
     * it from {@link #getMemory(AddressBlock)} until this is called again
     * with a <code>null</code> value.
     * <p>
     * This should <em>never</em> be called by anything other than the
     * {@link BusController} of the bus this device is connected to.
     *
     * @param memory the address the device was assigned to.
     */
    void setMemory(@Nullable final AddressBlock memory);

    // --------------------------------------------------------------------- //

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
