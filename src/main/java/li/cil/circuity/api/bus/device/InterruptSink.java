package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;

import javax.annotation.Nullable;

/**
 * Implemented by devices that can <em>accept</em> interrupts.
 * <p>
 * Typically this will be implemented by processors and programmable interrupt
 * controllers. Interruptable devices are collected by the {@link BusController}
 * to build the global list of legal interrupts, as provided to the full list
 * of {@link BusDevice}s to assign them to the available interrupts.
 * <p>
 * <em>Important</em>: a device is responsible for persisting the interrupt
 * numbers assigned to it via {@link #setAcceptedInterrupts(int[])} until
 * <code>null</code> is passed to {@link #setAcceptedInterrupts(int[])}.
 * This is necessary for devices to have the same interrupt numbers assigned
 * to them after a save and load, because the {@link BusController} does not
 * store the interrupt numbers of the connected devices.
 */
public interface InterruptSink extends BusDevice {
    /**
     * Get the list of interrupt IDs accepted by this device.
     * <p>
     * These may be regular IRQs (interrupt requests) or NMI (non-maskable
     * interrupts), depending on the implementation of the Device. There must
     * be one interrupt ID for each interrupt the device accepts. For example,
     * if the device accepts one IRQ and one NMI, it must return two IDs.
     * <p>
     * Interrupts are numbered globally by the {@link BusController}. The
     * passed list provides an abstracted way of selecting the desired number
     * of interrupt IDs (for as many interrupts as the device uses).
     * <p>
     * Returning an interrupt ID that is already in use is an error; a warning
     * will be logged, and the device will be assigned no interrupt IDs.
     * <p>
     * The device <em>is required to persist the interrupts it is currently
     * occupying</em>. In that case, the given list is to be ignored, and the
     * persisted list of interrupt IDs is to be returned.
     *
     * @param interrupts the list of available interrupts.
     * @return the list of IDs equal in size as the number of accepted interrupts.
     */
    int[] getAcceptedInterrupts(final InterruptList interrupts);

    /**
     * Sets the list of actual interrupt sink IDs bound to this device.
     * <p>
     * Passing <code>null</code> indicates unbinding the device from all
     * interrupts.
     * <p>
     * The device <em>is required to persist the interrupts it is currently
     * occupying</em>.
     *
     * @param interrupts the list of interrupt IDs the device is now bound to.
     */
    void setAcceptedInterrupts(@Nullable final int[] interrupts);

    /**
     * Activate the specified interrupt provided by this device.
     *
     * @param interruptId the ID of the interrupt to activate.
     * @param data        additional data passed along with the interrupt.
     */
    void interrupt(final int interruptId, final int data);
}
