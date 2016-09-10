package li.cil.circuity.api.bus.controller;

import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;

import java.util.PrimitiveIterator;

public interface InterruptMapper extends Subsystem {
    // TODO Might want to make this (InterruptSource, int, InterruptSink, int)? Could be annoying for sync to client for GUI/config, though...
    void setInterruptMapping(final int sourceId, final int sinkId);

    // --------------------------------------------------------------------- //

    /**
     * Get the list of interrupt IDs the specified source has currently assigned.
     * <p>
     * The returned indices will be unique for this bus, and are assigned
     * automatically to {@link InterruptSource}s for each of their provided
     * interrupts.
     *
     * @param device the interrupt source to get the interrupt IDs for.
     * @return the list of interrupt IDs the source has assigned to it.
     */
    PrimitiveIterator.OfInt getInterruptSourceIds(final InterruptSource device);

    /**
     * Get the list of interrupt IDs the specified sink has currently assigned.
     * <p>
     * The returned indices will be unique for this bus, and are assigned
     * automatically to {@link InterruptSink}s for each of their provided
     * interrupts.
     *
     * @param device the interrupt sink to get the interrupt IDs for.
     * @return the list of interrupt IDs the sink has assigned to it.
     */
    PrimitiveIterator.OfInt getInterruptSinkIds(final InterruptSink device);

    // --------------------------------------------------------------------- //

    /**
     * Triggers the specified interrupt, passing along the specified data.
     * <p>
     * Whether or not the data is actually used depends on the interrupted
     * device.
     * <p>
     * This should only be called by the {@link InterruptSource} passing itself
     * as the first parameter. Triggering interrupts for other devices, while
     * possible, is strongly discouraged as it will lead to unexpected behavior.
     * <p>
     * Note that calling this method while the bus is offline is an illegal
     * operation. Either check before calling this, or, preferably, only call
     * this while processing a call from the bus controller. This includes
     * {@link AsyncTickable#updateAsync()}, {@link Addressable#read(int)},
     * {@link Addressable#write(int, int)} and {@link InterruptSink#interrupt(int, int)}.
     * <p>
     * This method is thread safe.
     *
     * @param source    the device emitting the interrupt.
     * @param interrupt the index of the <em>source</em> interrupt.
     * @param data      the data to pass along.
     */
    void interrupt(final InterruptSource source, final int interrupt, final int data);
}
