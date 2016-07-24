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
     * The number of interrupts provided by this device.
     * <p>
     * These may be regular IRQs (interrupt requests) or NMI (non-maskable
     * interrupts), depending on the implementation of the Device.
     * <p>
     * Interrupts are numbered globally by the {@link BusController}. The
     * passed list of interrupts will contain the list of "holes" in the
     * sequence of already occupied interrupts, as well as the last unused
     * interrupt. For example, if interrupts <code>1</code> and <code>4</code>
     * are already occupied, the list will be <code>{0,2,3,5}</code>. If no
     * interrupts are occupied, the list will be <code>{0}</code>.
     * <p>
     * The device <em>is required to persist the interrupts it is currently
     * occupying</em>.
     *
     * @param interrupts the list of available interrupts.
     * @return the number of interrupts this device provides.
     */
    int[] getAcceptedInterrupts(final int[] interrupts);

    void setAcceptedInterrupts(@Nullable final int[] interrupts);

    /**
     * Activate the specified interrupt provided by this device.
     *
     * @param interrupt the number of the interrupt to activate.
     */
    void interrupt(final int interrupt);
}
