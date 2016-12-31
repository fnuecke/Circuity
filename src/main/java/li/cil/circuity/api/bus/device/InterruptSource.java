package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * Implemented by devices that can <em>emit</em> interrupts.
 * <p>
 * Typically this will be implemented by devices that need to inform other
 * devices of unpredictable events, i.e. for which polling them is suboptimal,
 * such as input devices (e.g. keyboards) or devices performing operations that
 * that some interval of time (e.g. floppy drives seeking to a sector/reading
 * a block). Interrupting devices are collected by the {@link InterruptMapper}
 * to build the global list of assignments of interrupts.
 */
public interface InterruptSource extends BusDevice {
    /**
     * Get number of interrupts emitted by this device.
     *
     * @return the list of IDs equal in size as the number of emitted interrupts.
     */
    int getEmittedInterrupts();

    /**
     * Get a descriptive name for the specified output.
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
    ITextComponent getEmittedInterruptName(final int interrupt);
}
