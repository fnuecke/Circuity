package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.InterruptSink;

import javax.annotation.Nullable;

/**
 * Subsystem responsible for mapping {@link Addressable} devices to {@link AddressBlock}s.
 * <p>
 * This subsystem can support multiple configurations, to allow switching
 * between different mappings, which can be useful during the boot process
 * of a computer (e.g. mapping a ROM to the starting memory, then switch to
 * a layout where the starting memory is RAM).
 * <p>
 * Each configuration must be legal for the computer to start.
 */
public interface AddressMapper extends Subsystem {
    /**
     * The word size of the data bus in bits.
     *
     * @return the word size of the data bus.
     */
    int getWordSize();

    /**
     * Get the word mask of the data bus.
     * <p>
     * This mask can be used to extract just as much data from a value as is
     * supported by the word size.
     *
     * @return the mask based on the word size.
     */
    int getWordMask();

    /**
     * The number of supported configurations.
     * <p>
     * Each configuration represents an independent mapping of devices to
     * address blocks that can be switched between at any time.
     *
     * @return the number of configurations.
     */
    int getConfigurationCount();

    /**
     * Set the currently active configuration.
     * <p>
     * This can be called at any time, to switch between legal configurations.
     *
     * @param index the number of the configuration to switch to.
     * @throws IndexOutOfBoundsException if the index is invalid.
     */
    void setActiveConfiguration(final int index);

    // --------------------------------------------------------------------- //

    void setDeviceAddress(final Addressable device, final AddressBlock address);

    // --------------------------------------------------------------------- //

    /**
     * Get the address block the specified device is currently assigned to.
     *
     * @param device the device to get the address for.
     * @return the address of that device.
     */
    AddressBlock getAddressBlock(final Addressable device);

    /**
     * Get the device mapped to the specified address.
     *
     * @param address the address for which to get the device.
     * @return the device mapped to that address, if any.
     */
    @Nullable
    Addressable getDevice(final long address);

    /**
     * Write a value to the specified global address.
     * <p>
     * This will find the device mapped to the specified address, transform the
     * address to an address local to the device, and then write the value at
     * that local address to the device via {@link Addressable#write(int, int)}.
     * <p>
     * The address must fit into the address width supported by the bus
     * controller, otherwise an {@link IndexOutOfBoundsException} will be
     * thrown.
     * <p>
     * The value must fit into the data width supported by the bus controller,
     * otherwise you have to assume it may get truncated at some point.
     * <p>
     * Note that calling this method while the bus is offline is an illegal
     * operation. Either check before calling this, or, preferably, only call
     * this while processing a call from the bus controller. This includes
     * {@link AsyncTickable#updateAsync()}, {@link Addressable#read(int)},
     * {@link Addressable#write(int, int)} and {@link InterruptSink#interrupt(int, int)}.
     * Doing so will lead to undefined behavior. No exception is thrown
     * <p>
     * This method is <em>not</em> thread safe. It must only be called while in
     * a callback initiated from the bus controller (as listed in the previous
     * paragraph).
     *
     * @param address the global address to write to.
     * @param value   the value to write.
     * @throws IndexOutOfBoundsException if the address is unsupported.
     */
    void mapAndWrite(final long address, final int value) throws IndexOutOfBoundsException;

    /**
     * Read a value from the specified global address.
     * <p>
     * This will find the device mapped to the specified address, transform the
     * address to an address local to the device, and then read a value at
     * that local address from the device via {@link Addressable#read(int)}.
     * <p>
     * The address must fit into the address width supported by the bus
     * controller, otherwise an {@link IndexOutOfBoundsException} will be
     * thrown.
     * <p>
     * Note that calling this method while the bus is offline is an illegal
     * operation. Either check before calling this, or, preferably, only call
     * this while processing a call from the bus controller. This includes
     * {@link AsyncTickable#updateAsync()}, {@link Addressable#read(int)},
     * {@link Addressable#write(int, int)} and {@link InterruptSink#interrupt(int, int)}.
     * <p>
     * This method is <em>not</em> thread safe. It must only be called while in
     * a callback initiated from the bus controller (as listed in the previous
     * paragraph).
     *
     * @param address the global address to read from.
     * @return the value read.
     * @throws IndexOutOfBoundsException if the address is unsupported.
     */
    int mapAndRead(final long address) throws IndexOutOfBoundsException;
}
