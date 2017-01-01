package li.cil.lib.common;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.SynchronizationAPI;
import li.cil.lib.api.synchronization.SynchronizationManager;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public enum Synchronization implements SynchronizationAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final SynchronizationManagerClientImpl client = new SynchronizationManagerClientImpl();
    private final SynchronizationManagerServerImpl server = new SynchronizationManagerServerImpl();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.synchronization = INSTANCE;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            server.update();
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    public SynchronizationManagerClientImpl getClient() {
        return client;
    }

    @Override
    public SynchronizationManagerServerImpl getServer() {
        return server;
    }

    @Override
    public SynchronizationManager get(final boolean isRemote) {
        return isRemote ? getClient() : getServer();
    }

    @Override
    public SynchronizationManager get(final World world) {
        return get(world.isRemote);
    }
}
