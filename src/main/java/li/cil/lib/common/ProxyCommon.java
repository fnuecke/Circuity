package li.cil.lib.common;

import li.cil.lib.Capabilities;
import li.cil.lib.GlobalObjects;
import li.cil.lib.Manager;
import li.cil.lib.Scheduler;
import li.cil.lib.Serialization;
import li.cil.lib.SpatialUI;
import li.cil.lib.Storage;
import li.cil.lib.Synchronization;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.capabilities.CapabilityEntityContainer;
import li.cil.lib.capabilities.ItemHandlerModifiableWrapperProvider;
import li.cil.lib.capabilities.ItemHandlerWrapperProvider;
import li.cil.lib.common.gui.spatial.SpatialUIManagerServer;
import li.cil.lib.network.Network;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ProxyCommon {
    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    @CapabilityInject(IItemHandlerModifiable.class)
    public static Capability<IItemHandlerModifiable> ITEM_HANDLER_MODIFIABLE_CAPABILITY = null;

    // --------------------------------------------------------------------- //

    public void preInit(final FMLPreInitializationEvent event) {
        CapabilityEntityContainer.register();

        Network.init();
        Scheduler.init();

        Capabilities.init();
        SpatialUI.init();
        GlobalObjects.init();
        Manager.init();
        Scheduler.init();
        Serialization.init();
        Storage.init();
        Synchronization.init();

        SpatialUIManagerServer.init();

        SillyBeeAPI.capabilities.register(ITEM_HANDLER_CAPABILITY, ItemHandlerWrapperProvider.INSTANCE);
        SillyBeeAPI.capabilities.register(ITEM_HANDLER_MODIFIABLE_CAPABILITY, ItemHandlerModifiableWrapperProvider.INSTANCE);
    }
}
