package li.cil.lib;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.SpatialUIAPI;
import li.cil.lib.api.gui.spatial.SpatialUIProviderClient;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import li.cil.lib.client.gui.spatial.SpatialUIManagerClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum SpatialUI implements SpatialUIAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final Map<SpatialUIProviderClient, Class<? extends SpatialUIProviderServer>> clientToServer = new HashMap<>();
    private final Map<Class<? extends SpatialUIProviderServer>, SpatialUIProviderServer> serverProviders = new HashMap<>();
    private final List<SpatialUIProviderClient> clientProviders = new ArrayList<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.spatialUI = INSTANCE;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void register(final SpatialUIProviderClient clientProvider, final SpatialUIProviderServer serverProvider) {
        if (clientProviders.contains(clientProvider)) {
            ModSillyBee.getLogger().warn("Each SpatialUIClientProvider may only be registered once.");
            return;
        }
        clientToServer.put(clientProvider, serverProvider.getClass());
        clientProviders.add(clientProvider);
        serverProviders.put(serverProvider.getClass(), serverProvider);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void close() {
        SpatialUIManagerClient.INSTANCE.close();
    }

    // --------------------------------------------------------------------- //

    public Iterable<SpatialUIProviderClient> getClientProviders() {
        return clientProviders;
    }

    public Class<? extends SpatialUIProviderServer> getServerProviderClass(final SpatialUIProviderClient clientProvider) {
        return clientToServer.get(clientProvider);
    }

    public SpatialUIProviderServer getServerProvider(final Class<? extends SpatialUIProviderServer> providerClass) {
        return serverProviders.get(providerClass);
    }
}
