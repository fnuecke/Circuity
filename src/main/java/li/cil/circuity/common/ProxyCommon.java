package li.cil.circuity.common;

import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.circuity.common.capabilities.eeprom.CapabilityEEPROM;
import li.cil.circuity.common.capabilities.hdd.CapabilityHardDiskDrive;
import li.cil.circuity.common.init.Blocks;
import li.cil.circuity.common.init.Items;
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
    }

    public void postInit(final FMLPostInitializationEvent event) {
    }

    public void handleRegisterItem(final Item item) {
    }
}
