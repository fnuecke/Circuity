package li.cil.lib.api;

import li.cil.lib.api.scheduler.ScheduledCallback;
import net.minecraft.world.World;

/**
 * The scheduler API allows scheduling callbacks for execution at future time.
 * <p>
 * Typically this will be used to schedule execution of some lazy update in the
 * next server frame (tick), but the API also supports scheduling execution of
 * callbacks at specific times.
 * <p>
 * Note that all callbacks must be bound to a world, to allow measurement of
 * ticks for that world (to know when to execute the callback).
 */
public interface SchedulerAPI {
    /**
     * Schedule execution of the specified callback in the next tick.
     *
     * @param world    the world to schedule the callback for.
     * @param callback the callback to schedule.
     * @return a handle to the scheduled callback, can be used in {@link #cancel(World, ScheduledCallback)}.
     */
    ScheduledCallback schedule(final World world, final Runnable callback);

    /**
     * Schedule execution of the specified callback after the specified number
     * of ticks have passed.
     *
     * @param world    the world to schedule the callback for.
     * @param ticks    the number of ticks to wait before executing the callback.
     * @param callback the callback to schedule.
     * @return a handle to the scheduled callback, can be used in {@link #cancel(World, ScheduledCallback)}.
     */
    ScheduledCallback scheduleIn(final World world, final int ticks, final Runnable callback);

    /**
     * Schedule execution of the specified callback when the specified tick is
     * reached.
     *
     * @param world    the world to schedule the callback for.
     * @param tick     the tick to wait for before executing the callback.
     * @param callback the callback to schedule.
     * @return a handle to the scheduled callback, can be used in {@link #cancel(World, ScheduledCallback)}.
     */
    ScheduledCallback scheduleAt(final World world, final long tick, final Runnable callback);

    /**
     * Cancel a scheduled callback's execution.
     * <p>
     * This is typically used when an object that had registered an event is
     * being destroyed before the callback was executed.
     *
     * @param world    the world the callback was scheduled for.
     * @param callback the callback to cancel.
     */
    void cancel(final World world, final ScheduledCallback callback);
}
