package li.cil.lib.common.gui.spatial;

import li.cil.lib.ModSillyBee;
import li.cil.lib.SpatialUI;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import li.cil.lib.api.gui.spatial.SpatialUIServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

public final class SpatialUIManagerServer {
    public static final SpatialUIManagerServer INSTANCE = new SpatialUIManagerServer();

    // --------------------------------------------------------------------- //

    private final Map<NetHandlerPlayServer, SpatialUIServer> activeProviders = new WeakHashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public void handleData(final NetHandlerPlayServer client, final NBTTagCompound data) {
        synchronized (activeProviders) {
            final SpatialUIServer serverUi = activeProviders.get(client);
            if (serverUi != null) {
                try {
                    serverUi.handleData(data);
                } catch (final Throwable t) {
                    ModSillyBee.getLogger().error(t);
                }
            }
        }
    }

    public void subscribe(final NetHandlerPlayServer client, final Class<? extends SpatialUIProviderServer> dataProvider, final ICapabilityProvider target, @Nullable final EnumFacing side) {
        synchronized (activeProviders) {
            activeProviders.put(client, SpatialUI.INSTANCE.getServerProvider(dataProvider).provide(new SpatialUIContextServer(target, side, client)));
        }
    }

    public void unsubscribe(final NetHandlerPlayServer client) {
        synchronized (activeProviders) {
            activeProviders.remove(client);
        }
    }

    public boolean isSubscribed(final NetHandlerPlayServer client) {
        synchronized (activeProviders) {
            return activeProviders.containsKey(client);
        }
    }

    // --------------------------------------------------------------------- //

    @SubscribeEvent
    public void handleServerTick(final TickEvent.ServerTickEvent event) {
        synchronized (activeProviders) {
            activeProviders.values().forEach(SpatialUIServer::update);
        }
    }

    // --------------------------------------------------------------------- //

    private SpatialUIManagerServer() {
    }
}
