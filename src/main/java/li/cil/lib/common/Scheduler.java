package li.cil.lib.common;

import li.cil.lib.ModSillyBee;
import li.cil.lib.api.SchedulerAPI;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.event.ForwardedFMLServerStoppedEvent;
import li.cil.lib.api.scheduler.ScheduledCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

public enum Scheduler implements SchedulerAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final WeakHashMap<World, List<ScheduledCallback>> addedCallbacksClient = new WeakHashMap<>();
    private final WeakHashMap<World, PriorityQueue<ScheduledCallback>> scheduledCallbacksClient = new WeakHashMap<>();
    private final WeakHashMap<World, List<ScheduledCallback>> addedCallbacksServer = new WeakHashMap<>();
    private final WeakHashMap<World, PriorityQueue<ScheduledCallback>> scheduledCallbacksServer = new WeakHashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.scheduler = INSTANCE;
        SillyBeeAPI.EVENT_BUS.register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    // --------------------------------------------------------------------- //

    @SubscribeEvent
    public void handleServerTick(final TickEvent.ServerTickEvent event) {
        handleTick(event.phase, addedCallbacksServer, scheduledCallbacksServer);
    }

    @SubscribeEvent
    public void handleClientTick(final TickEvent.ClientTickEvent event) {
        handleTick(event.phase, addedCallbacksClient, scheduledCallbacksClient);
    }

    @SubscribeEvent
    public void handleServerStopped(final ForwardedFMLServerStoppedEvent event) {
        addedCallbacksServer.clear();
        scheduledCallbacksServer.clear();
    }

    @SubscribeEvent
    public void handleClientDisconnection(final FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        addedCallbacksClient.clear();
        scheduledCallbacksClient.clear();
    }

    // --------------------------------------------------------------------- //
    // SchedulerAPI

    @Override
    public ScheduledCallback schedule(final World world, final Runnable callback) {
        return scheduleIn(world, 0, callback);
    }

    @Override
    public ScheduledCallback scheduleIn(final World world, final int ticks, final Runnable callback) {
        return scheduleAt(world, world.getTotalWorldTime() + ticks, callback);
    }

    @Override
    public ScheduledCallback scheduleAt(final World world, final long tick, final Runnable callback) {
        final ScheduledCallback scheduledCallback = new ScheduledCallbackImpl(tick, callback);
        if (world.isRemote) {
            scheduleAt(addedCallbacksClient, world, scheduledCallback);
        } else {
            scheduleAt(addedCallbacksServer, world, scheduledCallback);
        }
        return scheduledCallback;
    }

    @Override
    public void cancel(final World world, final ScheduledCallback callback) {
        if (world.isRemote) {
            cancel(world, callback, addedCallbacksClient, scheduledCallbacksClient);
        } else {
            cancel(world, callback, addedCallbacksServer, scheduledCallbacksServer);
        }
    }

    // --------------------------------------------------------------------- //

    private static void handleTick(final TickEvent.Phase phase, final WeakHashMap<World, List<ScheduledCallback>> added, final WeakHashMap<World, PriorityQueue<ScheduledCallback>> scheduled) {
        if (phase != TickEvent.Phase.END) {
            return;
        }

        synchronized (scheduled) {
            synchronized (added) {
                for (final Map.Entry<World, List<ScheduledCallback>> entry : added.entrySet()) {
                    scheduled.
                            computeIfAbsent(entry.getKey(), w -> new PriorityQueue<>()).
                            addAll(entry.getValue());
                }
                added.clear();
            }
            scheduled.forEach(Scheduler::runWorldCallbacks);
        }
    }

    private static void scheduleAt(final WeakHashMap<World, List<ScheduledCallback>> added, final World world, final ScheduledCallback callback) {
        synchronized (added) {
            added.
                    computeIfAbsent(world, (ignored) -> new ArrayList<>()).
                    add(callback);
        }
    }

    private static void cancel(final World world, final ScheduledCallback callback, final WeakHashMap<World, List<ScheduledCallback>> added, final WeakHashMap<World, PriorityQueue<ScheduledCallback>> scheduled) {
        synchronized (added) {
            final List<ScheduledCallback> list = added.get(world);
            if (list != null) {
                list.remove(callback);
            }
        }
        synchronized (scheduled) {
            final PriorityQueue<ScheduledCallback> queue = scheduled.get(world);
            if (queue != null) {
                queue.remove(callback);
            }
        }
    }

    private static void runWorldCallbacks(final World world, final PriorityQueue<ScheduledCallback> queue) {
        if (isWorldLoaded(world)) {
            final long currentTick = world.getTotalWorldTime();
            while (!queue.isEmpty() && queue.peek().getTick() <= currentTick) {
                final ScheduledCallback scheduledCallback = queue.poll();
                try {
                    scheduledCallback.getCallback().run();
                } catch (final Throwable t) {
                    ModSillyBee.getLogger().error("Scheduled callback threw up.", t);
                }
            }
        } else {
            queue.clear();
        }
    }

    private static boolean isWorldLoaded(@Nullable final World world) {
        if (world == null) {
            return false;
        }
        if (!world.isRemote) {
            return isWorldLoadedServer(world);
        } else {
            return isWorldLoadedClient(world);
        }
    }

    private static boolean isWorldLoadedServer(final World world) {
        return DimensionManager.getWorld(world.provider.getDimension()) == world;
    }

    private static boolean isWorldLoadedClient(final World world) {
        return Minecraft.getMinecraft().world == world;
    }

    // --------------------------------------------------------------------- //

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
