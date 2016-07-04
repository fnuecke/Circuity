package li.cil.lib.util;

import li.cil.oc.common.ModOpenComputers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

public final class Scheduler {
    private static final Scheduler INSTANCE = new Scheduler();
    private final WeakHashMap<World, PriorityQueue<ScheduledCallback>> scheduledCallbacks = new WeakHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            synchronized (scheduledCallbacks) {
                scheduledCallbacks.forEach(Scheduler::runWorldCallbacksServer);
            }
        }
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            synchronized (scheduledCallbacks) {
                scheduledCallbacks.forEach(Scheduler::runWorldCallbacksClient);
            }
        }
    }

    private static void runWorldCallbacksServer(@Nullable final World world, final PriorityQueue<ScheduledCallback> queue) {
        if (world == null) queue.clear();
        else if (!world.isRemote) runWorldCallbacks(world, queue);
    }

    private static void runWorldCallbacksClient(@Nullable final World world, final PriorityQueue<ScheduledCallback> queue) {
        if (world == null) queue.clear();
        else if (world.isRemote) runWorldCallbacks(world, queue);
    }

    private static void runWorldCallbacks(final World world, final PriorityQueue<ScheduledCallback> queue) {
        if (isWorldLoaded(world)) {
            final long currentTick = world.getTotalWorldTime();
            while (!queue.isEmpty() && queue.peek().getTick() <= currentTick) {
                final ScheduledCallback scheduledCallback = queue.poll();
                try {
                    scheduledCallback.getCallback().run();
                } catch (final Throwable t) {
                    ModOpenComputers.getLogger().error("Scheduled callback threw up.", t);
                }
            }
        } else {
            queue.clear();
        }
    }

    private static boolean isWorldLoaded(final World world) {
        if (!world.isRemote) {
            return isWorldLoadedServer(world);
        } else {
            return iwWorldLoadedClient(world);
        }
    }

    private static boolean isWorldLoadedServer(final World world) {
        return DimensionManager.getWorld(world.provider.getDimension()) == world;
    }

    private static boolean iwWorldLoadedClient(final World world) {
        return Minecraft.getMinecraft().theWorld == world;
    }

    public static ScheduledCallback schedule(final World world, final Runnable callback) {
        return scheduleIn(world, 0, callback);
    }

    public static ScheduledCallback scheduleIn(final World world, final int ticks, final Runnable callback) {
        return scheduleAt(world, world.getTotalWorldTime() + ticks, callback);
    }

    public static ScheduledCallback scheduleAt(final World world, final long tick, final Runnable callback) {
        final ScheduledCallback scheduledCallback = new ScheduledCallbackImpl(tick, callback);
        synchronized (INSTANCE.scheduledCallbacks) {
            INSTANCE.scheduledCallbacks.
                    computeIfAbsent(world, (ignored) -> new PriorityQueue<>()).
                    add(scheduledCallback);
        }
        return scheduledCallback;
    }

    public static void cancel(final World world, final ScheduledCallback callback) {
        synchronized (INSTANCE.scheduledCallbacks) {
            final PriorityQueue<ScheduledCallback> queue = INSTANCE.scheduledCallbacks.get(world);
            if (queue != null) {
                queue.remove(callback);
            }
        }
    }

    public interface ScheduledCallback {
        long getTick();

        Runnable getCallback();
    }

    private static final class ScheduledCallbackImpl implements ScheduledCallback, Comparable<ScheduledCallback> {
        private final long tick;
        private final Runnable callback;

        private ScheduledCallbackImpl(final long tick, final Runnable callback) {
            this.tick = tick;
            this.callback = callback;
        }

        public long getTick() {
            return tick;
        }

        @Override
        public Runnable getCallback() {
            return callback;
        }

        @Override
        public int compareTo(final ScheduledCallback that) {
            return (int) (this.getTick() - that.getTick());
        }
    }
}
