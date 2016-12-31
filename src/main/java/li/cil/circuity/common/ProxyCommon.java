package li.cil.circuity.common;

import li.cil.circuity.client.gui.spatial.SpatialUIProviderClientAddressMapping;
import li.cil.circuity.client.gui.spatial.SpatialUIProviderClientAddressable;
import li.cil.circuity.client.gui.spatial.SpatialUIProviderClientInterruptable;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.circuity.common.capabilities.eeprom.CapabilityEEPROM;
import li.cil.circuity.common.capabilities.hdd.CapabilityHardDiskDrive;
import li.cil.circuity.common.init.Blocks;
import li.cil.circuity.common.init.Items;
import li.cil.circuity.server.gui.spatial.SpatialUIProviderServerAddressMapping;
import li.cil.circuity.server.gui.spatial.SpatialUIProviderServerAddressable;
import li.cil.circuity.server.gui.spatial.SpatialUIProviderServerInterruptable;
import li.cil.lib.api.SillyBeeAPI;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public abstract class ProxyCommon {
    public void preInit(final FMLPreInitializationEvent event) {
        Settings.init(event.getSuggestedConfigurationFile());

        Blocks.init();
        Items.init();

        CapabilityBusElement.register();
        CapabilityHardDiskDrive.register();
        CapabilityEEPROM.register();
    }

    public void init(final FMLInitializationEvent event) {
        SillyBeeAPI.spatialUI.register(SpatialUIProviderClientAddressable.INSTANCE, SpatialUIProviderServerAddressable.INSTANCE);
        SillyBeeAPI.spatialUI.register(SpatialUIProviderClientAddressMapping.INSTANCE, SpatialUIProviderServerAddressMapping.INSTANCE);
        SillyBeeAPI.spatialUI.register(SpatialUIProviderClientInterruptable.INSTANCE, SpatialUIProviderServerInterruptable.INSTANCE);
    }

    public void postInit(final FMLPostInitializationEvent event) {
    }

    public void handleRegisterItem(final Item item) {
    }
}
