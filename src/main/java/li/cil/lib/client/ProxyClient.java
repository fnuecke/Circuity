package li.cil.lib.client;

import li.cil.lib.client.gui.spatial.SpatialUIManagerClient;
import li.cil.lib.common.ProxyCommon;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ProxyClient extends ProxyCommon {
    @Override
    public void preInit(final FMLPreInitializationEvent event) {
        super.preInit(event);

        SpatialUIManagerClient.init();
    }
}
