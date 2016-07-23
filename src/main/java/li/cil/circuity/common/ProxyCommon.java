package li.cil.circuity.common;

import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.circuity.common.init.Blocks;
import li.cil.circuity.common.init.Items;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public abstract class ProxyCommon {
    public void preInit(final FMLPreInitializationEvent event) {
        Settings.init(event.getSuggestedConfigurationFile());

        Blocks.init();
        Items.init();

        CapabilityBusDevice.register();
    }

    public void init(final FMLInitializationEvent event) {

    }

    public void postInit(final FMLPostInitializationEvent event) {
    }
}
