package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;

import javax.annotation.Nullable;

/**
 * Implemented by devices that can <em>emit</em> interrupts.
 * <p>
 * Typically this will be implemented by devices that need to inform other
 * devices of unpredictable events, i.e. for which polling them is suboptimal,
 * such as input devices (e.g. keyboards) or devices performing operations that
 * that some interval of time (e.g. floppy drives seeking to a sector/reading
 * a block). Interrupting devices are collected by the {@link BusController} to
 * build the global list of assignments of interrupts.
 */
public interface InterruptSource extends BusDevice {
    int[] getEmittedInterrupts(final int[] interrupts);

    void setEmittedInterrupts(@Nullable final int[] interrupts);
}
