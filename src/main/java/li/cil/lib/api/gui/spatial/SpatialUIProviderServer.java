package li.cil.lib.api.gui.spatial;

/**
 * Implementations are used on the server to create {@link SpatialUIServer}
 * objects for synchronizing data to an active {@link SpatialUIClient} on a client.
 */
public interface SpatialUIProviderServer {
    /**
     * Begin tracking UI data for the block at the specified coordinates.
     * <p>
     * <p>
     * The specified context contains information about the target the UI is
     * created for, as well as a method that will allow sending data to the
     * client representation of the UI.
     *
     * @param context the context for the UI, such as the target object.
     * @return the data tracker for the specified block.
     */
    SpatialUIServer provide(final SpatialUIContext context);
}
