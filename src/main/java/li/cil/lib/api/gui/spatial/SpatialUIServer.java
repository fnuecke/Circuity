package li.cil.lib.api.gui.spatial;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Implementations are used on the server side to provide data to the currently
 * active {@link SpatialUIClient} on a client.
 */
public interface SpatialUIServer {
    /**
     * Called when new data is received from the client.
     * <p>
     * This is the data send via the client {@link SpatialUIContext} from the
     * client's {@link SpatialUIClient}, produced by the {@link SpatialUIProviderClient}
     * that was registered together with the {@link SpatialUIProviderServer}
     * that produced this instance.
     *
     * @param data the received data.
     */
    void handleData(final NBTTagCompound data);

    /**
     * Called each server tick to update the data currently provided to a client.
     */
    void update();
}
