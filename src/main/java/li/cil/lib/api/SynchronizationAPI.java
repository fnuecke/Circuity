package li.cil.lib.api;

import li.cil.lib.api.synchronization.SynchronizationManager;
import li.cil.lib.api.synchronization.SynchronizationManagerClient;
import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import net.minecraft.world.World;

/**
 * Access to synchronization managers for the server and client side.
 */
public interface SynchronizationAPI {
    /**
     * Get the synchronization manager for the client side.
     *
     * @return the synchronization manager for the client side.
     */
    SynchronizationManagerClient getClient();

    /**
     * Get the synchronization manager for the server side.
     *
     * @return the synchronization manager for the server side.
     */
    SynchronizationManagerServer getServer();

    /**
     * Utility method for obtaining the synchronization manager based on a world
     * object, which will be used to determine the side.
     *
     * @param world the world based on which to get the synchronization manager.
     * @return the synchronization manager for the side the world lives on.
     */
    SynchronizationManager get(final World world);

    /**
     * Utility method for obtaining the synchronization manager based on a
     * boolean flag indicating sidedness.
     *
     * @param isRemote whether to retrieve the client side synchronization manager.
     * @return the synchronization manager for the client side if <code>isRemote</code>
     * is <code>true</code>, the one for the server side otherwise.
     */
    SynchronizationManager get(final boolean isRemote);
}
