package li.cil.lib;

import li.cil.lib.api.ManagerAPI;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.ecs.manager.event.ComponentChangeListener;
import li.cil.lib.api.ecs.manager.event.EntityChangeListener;
import li.cil.lib.api.event.ForwardedFMLServerStoppedEvent;
import li.cil.lib.api.synchronization.SynchronizationManager;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public enum Manager implements ManagerAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final WeakHashMap<World, EntityComponentManagerImpl> managersServer = new WeakHashMap<>();
    private final WeakHashMap<EntityComponentManager, WeakReference<World>> worldsServer = new WeakHashMap<>();
    private final Object lockServer = new Object();

    private final WeakHashMap<World, EntityComponentManagerImpl> managersClient = new WeakHashMap<>();
    private final WeakHashMap<EntityComponentManager, WeakReference<World>> worldsClient = new WeakHashMap<>();
    private final Object lockClient = new Object();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.manager = INSTANCE;
        SillyBeeAPI.EVENT_BUS.register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void handleServerTick(final TickEvent.ServerTickEvent event) {
        synchronized (lockServer) {
            handleTick(event.phase, managersServer, worldsServer, Manager::getServerWorld);
        }
    }

    @SubscribeEvent
    public void handleClientTick(final TickEvent.ClientTickEvent event) {
        synchronized (lockClient) {
            handleTick(event.phase, managersClient, worldsClient, Manager::getClientWorld);
        }
    }

    @SubscribeEvent
    public void handleWorldUnload(final WorldEvent.Unload event) {
        final World world = event.getWorld();
        if (!world.isRemote) {
            synchronized (lockServer) {
                worldsServer.remove(managersServer.remove(world));
            }
        } else {
            synchronized (lockClient) {
                worldsClient.remove(managersClient.remove(world));
            }
        }
    }

    @SubscribeEvent
    public void handleServerStopped(final ForwardedFMLServerStoppedEvent event) {
        managersServer.clear();
        worldsServer.clear();
        managersClient.clear();
        worldsClient.clear();
    }

    // --------------------------------------------------------------------- //

    @Override
    public EntityComponentManagerImpl getManager(final World world) {
        if (!world.isRemote) {
            synchronized (lockServer) {
                return managersServer.computeIfAbsent(world, this::createManagerForWorld);
            }
        } else {
            synchronized (lockClient) {
                return managersClient.computeIfAbsent(world, this::createManagerForWorld);
            }
        }
    }

    @Nullable
    @Override
    public World getWorld(final EntityComponentManager manager, final boolean forRemote) {
        if (!forRemote) {
            synchronized (lockServer) {
                return tryGetWorld(worldsServer, manager);
            }
        } else {
            synchronized (lockClient) {
                return tryGetWorld(worldsClient, manager);
            }
        }
    }

    // --------------------------------------------------------------------- //

    private EntityComponentManagerImpl createManagerForWorld(final World world) {
        final EntityComponentManagerImpl manager = new EntityComponentManagerImpl();

        final SynchronizationManager synchronizationManager = SillyBeeAPI.synchronization.get(world);
        if (synchronizationManager instanceof EntityChangeListener) {
            manager.addEntityChangeListener((EntityChangeListener) synchronizationManager);
        }
        if (synchronizationManager instanceof ComponentChangeListener) {
            manager.addComponentChangeListener((ComponentChangeListener) synchronizationManager);
        }

        if (world.isRemote) {
            worldsClient.put(manager, new WeakReference<>(world));
        } else {
            worldsServer.put(manager, new WeakReference<>(world));
        }
        return manager;
    }

    private static void handleTick(final TickEvent.Phase phase, final WeakHashMap<World, EntityComponentManagerImpl> managers, final WeakHashMap<EntityComponentManager, WeakReference<World>> worlds, final Function<World, World> actualWorldGetter) {
        if (phase == TickEvent.Phase.START) {
            updateManagers(managers, worlds, actualWorldGetter, EntityComponentManagerImpl::update);
        } else if (phase == TickEvent.Phase.END) {
            updateManagers(managers, worlds, actualWorldGetter, EntityComponentManagerImpl::lateUpdate);
        }
    }

    private static void updateManagers(final WeakHashMap<World, EntityComponentManagerImpl> managers, final WeakHashMap<EntityComponentManager, WeakReference<World>> worlds, final Function<World, World> actualWorldGetter, final Consumer<EntityComponentManagerImpl> updater) {
        final Iterator<Map.Entry<World, EntityComponentManagerImpl>> iterator = managers.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<World, EntityComponentManagerImpl> entry = iterator.next();
            final World world = entry.getKey();
            final EntityComponentManagerImpl manager = entry.getValue();

            if (world == actualWorldGetter.apply(world)) {
                updater.accept(manager);
            } else {
                iterator.remove();
                worlds.remove(entry.getValue());
            }
        }
    }

    @Nullable
    private static World tryGetWorld(final WeakHashMap<EntityComponentManager, WeakReference<World>> worlds, final EntityComponentManager manager) {
        final WeakReference<World> worldRef = worlds.get(manager);
        if (worldRef != null) {
            final World world = worldRef.get();
            if (world != null) {
                return world;
            }
            worlds.remove(manager);
        }
        return null;
    }

    private static World getClientWorld(final World world) {
        return Minecraft.getMinecraft().world;
    }

    private static World getServerWorld(final World world) {
        return DimensionManager.getWorld(world.provider.getDimension());
    }
}
