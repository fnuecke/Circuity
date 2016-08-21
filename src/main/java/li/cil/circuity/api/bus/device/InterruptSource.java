package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import net.minecraft.util.text.ITextComponent;

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
    /**
     * Get the list of interrupts IDs emitted by this device.
     * <p>
     * There must be one interrupt ID for each interrupt the device emits. For
     * example, if the device emits two different interrupts, it must return
     * two IDs. How these interrupts are used depends on how they are mapped
     * to {@link InterruptSink}s in the {@link BusController}.
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
     * @return the list of IDs equal in size as the number of emitted interrupts.
     */
    int[] getEmittedInterrupts(final InterruptList interrupts);

    /**
     * Sets the list of actual interrupt source IDs bound to this device.
     * <p>
     * Passing <code>null</code> indicates unbinding the device from all
     * interrupts.
     * <p>
     * The device <em>is required to persist the interrupts it is currently
     * occupying</em>.
     *
     * @param interrupts the list of interrupt IDs the device is now bound to.
     */
    void setEmittedInterrupts(@Nullable final int[] interrupts);

    /**
     * Get a descriptive name for the specified output ID.
     * <p>
     * This is used for communicating what a single interrupt of this device
     * does to the user (i.e. it will be shown to the user in status messages
     * and interfaces).
     * <p>
     * While this may return <em>null</em>, it is strongly recommended to
     * return a meaningful, human-readable value here.
     *
     * @param interruptId the ID to get the name for.
     * @return the name for that ID.
     */
    @Nullable
    ITextComponent getInterruptName(final int interruptId);
}
