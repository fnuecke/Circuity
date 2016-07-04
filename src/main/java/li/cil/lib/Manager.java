package li.cil.lib;

import li.cil.lib.api.ManagerAPI;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.ecs.manager.event.ComponentChangeListener;
import li.cil.lib.api.ecs.manager.event.EntityChangeListener;
import li.cil.lib.api.synchronization.SynchronizationManager;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public enum Manager implements ManagerAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final WeakHashMap<World, EntityComponentManager> managers = new WeakHashMap<>();
    private final WeakHashMap<EntityComponentManager, WeakReference<World>> worlds = new WeakHashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.manager = INSTANCE;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (final Map.Entry<World, EntityComponentManager> entry : managers.entrySet()) {
                update(entry.getKey(), false);
            }
        }
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (final Map.Entry<World, EntityComponentManager> entry : managers.entrySet()) {
                update(entry.getKey(), true);
            }
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    public EntityComponentManager getManager(final World world) {
        return managers.computeIfAbsent(world, this::createManagerForWorld);
    }

    @Nullable
    @Override
    public World getWorld(final EntityComponentManager manager) {
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

    // --------------------------------------------------------------------- //

    private EntityComponentManager createManagerForWorld(final World w) {
        final EntityComponentManager manager = new EntityComponentManagerImpl();

        final SynchronizationManager synchronizationManager = SillyBeeAPI.synchronization.get(w);
        if (synchronizationManager instanceof EntityChangeListener) {
            manager.addEntityChangeListener((EntityChangeListener) synchronizationManager);
        }
        if (synchronizationManager instanceof ComponentChangeListener) {
            manager.addComponentChangeListener((ComponentChangeListener) synchronizationManager);
        }

        worlds.put(manager, new WeakReference<>(w));
        return manager;
    }

    private static void update(final World world, final boolean forRemote) {
        if (world.isRemote != forRemote) {
            return;
        }

        // We need/want to cast here, because we don't want this method in the public API.
        final EntityComponentManagerImpl manager = (EntityComponentManagerImpl) SillyBeeAPI.manager.getManager(world);
        manager.update();
    }
}
