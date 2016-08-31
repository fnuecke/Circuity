package li.cil.lib.api;

import net.minecraftforge.fml.common.eventhandler.EventBus;

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
     * Event bus the library will post events to.
     * <p>
     * Most notably, it will forward some FML events to this bus, so that they
     * can also be used in other classes than just directly in the mod class.
     * See the <code>event</code> package for the list of forwarded events.
     */
    public static final EventBus EVENT_BUS = new EventBus();

    // --------------------------------------------------------------------- //

    /**
     * Access to the capabilities API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static CapabilitiesAPI capabilities;

    /**
     * Access to the global object API.
     * <p>
     * This is initialized by the mod in the pre-init phase.
     */
    public static GlobalObjectsAPI globalObjects;

    /**
     * Access to the manager API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static ManagerAPI manager;

    /**
     * Access to the scheduler API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static SchedulerAPI scheduler;

    /**
     * Access to the serialization API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static SerializationAPI serialization;

    /**
     * Access to external data storage API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static StorageAPI storage;

    /**
     * Access to the synchronization API.
     * <p>
     * This is initialized by the library mod in the pre-init phase.
     */
    public static SynchronizationAPI synchronization;

    // --------------------------------------------------------------------- //

    private SillyBeeAPI() {
    }
}
