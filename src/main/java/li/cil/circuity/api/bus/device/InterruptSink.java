package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * Implemented by devices that can <em>accept</em> interrupts.
 * <p>
 * Typically this will be implemented by processors and programmable interrupt
 * controllers. Interruptable devices are collected by the {@link InterruptMapper}
 * to build the global list of legal interrupts, as provided to the full list
 * of {@link BusDevice}s to assign them to the available interrupts.
 */
public interface InterruptSink extends BusDevice {
    /**
     * Get the number of interrupts accepted by this device.
     *
     * @return the number of accepted interrupts.
     */
    int getAcceptedInterrupts();

    /**
     * Get a descriptive name for the specified input.
     * <p>
     * This is used for communicating what a single interrupt of this device
     * does to the user (i.e. it will be shown to the user in status messages
     * and interfaces).
     * <p>
     * While this may return <em>null</em>, it is strongly recommended to
     * return a meaningful, human-readable value here.
     *
     * @param interrupt the index of the interrupt to get the name for.
     * @return the name for that interrupt.
     */
    @Nullable
    ITextComponent getAcceptedInterruptName(final int interrupt);

    /**
     * Activate the specified interrupt provided by this device.
     *
     * @param interrupt the index of the interrupt to activate.
     * @param data      additional data passed along with the interrupt.
     */
    void interrupt(final int interrupt, final int data);
}
