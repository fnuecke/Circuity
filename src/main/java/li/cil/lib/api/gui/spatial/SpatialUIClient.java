package li.cil.lib.api.gui.spatial;

import li.cil.lib.api.gui.input.InputEvent;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Implemented by actual instances of spatial UIs, which are kept while
 * displaying the same UI over multiple frames.
 * <p>
 * These are instantiated by the library on demand using a {@link SpatialUIProviderClient}.
 */
public interface SpatialUIClient {
    /**
     * Called when new data is received from the server.
     * <p>
     * This is the data send via the server {@link SpatialUIContext} from the
     * server's {@link SpatialUIServer}, produced by the {@link SpatialUIProviderServer}
     * that was registered together with the {@link SpatialUIProviderClient}
     * that produced this instance.
     *
     * @param data the received data.
     */
    void handleData(final NBTTagCompound data);

    /**
     * Called whenever there is some kind of input that can be processed by
     * the current UI.
     *
     * @param event the event to process.
     */
    void handleInput(final InputEvent event);

    /**
     * Called each client game tick while the UI is shown.
     */
    void update();

    /**
     * Called each frame while the UI is shown to render the UI in the world.
     * <p>
     * The GL state will have been adjusted such that the UI may render its
     * content in a rectangular space from (0, 0, 0) to (1, 1, 0).
     */
    void render();
}
