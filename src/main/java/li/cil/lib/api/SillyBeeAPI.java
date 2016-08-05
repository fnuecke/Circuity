package li.cil.lib.api;

/**
 * API entry point for the library.
 */
public final class SillyBeeAPI {
    /**
     * Mod ID of the library mod.
     */
    public static final String MOD_ID = "sillybee";

    /**
     * Version of the library and its API.
     */
    public static final String MOD_VERSION = "@VERSION_LIB@";

    // --------------------------------------------------------------------- //

    /**
     * Access to the capabilities API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static CapabilitiesAPI capabilities;

    /**
     * Access to the serialization API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static SerializationAPI serialization;

    /**
     * Access to the synchronization API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static SynchronizationAPI synchronization;

    /**
     * Access to the manager API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static ManagerAPI manager;

    /**
     * Access to external data storage API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static StorageAPI storage;

    // --------------------------------------------------------------------- //

    private SillyBeeAPI() {
    }
}
