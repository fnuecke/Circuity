package li.cil.lib.api;

import li.cil.lib.api.gui.spatial.SpatialUIProviderClient;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Access to spatial UI registry.
 */
public interface SpatialUIAPI {
    /**
     * Register a new client and server part of a spatial UI.
     *
     * @param clientProvider the client side provider.
     * @param serverProvider the server side provider.
     */
    void register(final SpatialUIProviderClient clientProvider, final SpatialUIProviderServer serverProvider);

    /**
     * Close the currently open spatial UI.
     * <p>
     * Intended to allow currently open UIs to close themselves in case they
     * become invalid (e.g. due to an item required for it to be shown no
     * longer being held).
     */
    @SideOnly(Side.CLIENT)
    void close();
}
