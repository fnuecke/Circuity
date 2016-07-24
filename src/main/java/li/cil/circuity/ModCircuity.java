package li.cil.circuity;

import li.cil.circuity.api.CircuityAPI;
import li.cil.circuity.common.ProxyCommon;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = CircuityAPI.MOD_ID, name = CircuityAPI.MOD_NAME, version = CircuityAPI.MOD_VERSION)
public final class ModCircuity {
    @Mod.Instance(CircuityAPI.MOD_ID)
    private static ModCircuity instance;

    @SidedProxy(clientSide = "li.cil.circuity.client.ProxyClient", serverSide = "li.cil.circuity.server.ProxyServer")
    private static ProxyCommon proxy;

    private static Logger logger;

    public static ModCircuity getInstance() {
        return instance;
    }

    public static ProxyCommon getProxy() {
        return proxy;
    }

    public static Logger getLogger() {
        return logger;
    }

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(final FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
