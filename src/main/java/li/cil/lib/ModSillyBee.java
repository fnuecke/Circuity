package li.cil.lib;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.capabilities.ItemHandlerModifiableWrapperProvider;
import li.cil.lib.capabilities.ItemHandlerWrapperProvider;
import li.cil.lib.network.Network;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.logging.log4j.Logger;

@Mod(modid = SillyBeeAPI.MOD_ID, name = ModSillyBee.MOD_NAME, version = ModSillyBee.MOD_VERSION)
public final class ModSillyBee {
    public static final String MOD_NAME = "SillyBee";
    public static final String MOD_VERSION = "@VERSION_LIB@";

    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    @CapabilityInject(IItemHandlerModifiable.class)
    public static Capability<IItemHandlerModifiable> ITEM_HANDLER_MODIFIABLE_CAPABILITY = null;

    private static Logger logger;

    public static Logger getLogger() {
        return logger;
    }

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        logger = event.getModLog();

        Network.init();
        Scheduler.init();

        Capabilities.init();
        Manager.init();
        Scheduler.init();
        Serialization.init();
        Storage.init();
        Synchronization.init();

        SillyBeeAPI.capabilities.register(ITEM_HANDLER_CAPABILITY, ItemHandlerWrapperProvider.INSTANCE);
        SillyBeeAPI.capabilities.register(ITEM_HANDLER_MODIFIABLE_CAPABILITY, ItemHandlerModifiableWrapperProvider.INSTANCE);
    }

    @Mod.EventHandler
    public void serverStopped(final FMLServerStoppedEvent event) {
        Manager.INSTANCE.handleServerStopped(event);
        Scheduler.INSTANCE.handleServerStopped(event);
        Storage.INSTANCE.handleServerStopped(event);
    }
}
