package li.cil.lib.api.scheduler;

import net.minecraft.world.World;

/**
 * Reference to a scheduled callback.
 * <p>
 * Primary use of this is to allow keeping a reference to a scheduled callback
 * so that it can be canceled again via {@link li.cil.lib.api.SchedulerAPI#cancel(World, ScheduledCallback)},
 * which is usually necessary to cancel pending callbacks registered by an
 * object that is getting destroyed.
 */
public interface ScheduledCallback {
    /**
     * The tick in which the callback is scheduled to run.
     *
     * @return the tick the callback will be executed in.
     */
    long getTick();

    /**
     * The actual callback method that is scheduled.
     * <p>
     * Mainly for internal use, should not be called unless you really know
     * what you're doing.
     *
     * @return the scheduled callback.
     */
    Runnable getCallback();
}
