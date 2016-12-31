package li.cil.lib;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.event.ForwardedFMLServerStartedEvent;
import li.cil.lib.api.event.ForwardedFMLServerStartingEvent;
import li.cil.lib.api.event.ForwardedFMLServerStoppedEvent;
import li.cil.lib.api.event.ForwardedFMLServerStoppingEvent;
import li.cil.lib.common.ProxyCommon;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = SillyBeeAPI.MOD_ID, name = ModSillyBee.MOD_NAME, version = SillyBeeAPI.MOD_VERSION)
public final class ModSillyBee {
    public static final String MOD_NAME = "SillyBee";

    @SidedProxy(clientSide = "li.cil.lib.client.ProxyClient", serverSide = "li.cil.lib.common.ProxyCommon")
    private static ProxyCommon proxy;

    private static Logger logger;

    public static Logger getLogger() {
        return logger;
    }

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(final FMLServerStartingEvent event) {
        SillyBeeAPI.EVENT_BUS.post(new ForwardedFMLServerStartingEvent(event));
    }

    @Mod.EventHandler
    public void serverStarted(final FMLServerStartedEvent event) {
        SillyBeeAPI.EVENT_BUS.post(new ForwardedFMLServerStartedEvent(event));
    }

    @Mod.EventHandler
    public void serverStopping(final FMLServerStoppingEvent event) {
        SillyBeeAPI.EVENT_BUS.post(new ForwardedFMLServerStoppingEvent(event));
    }

    @Mod.EventHandler
    public void serverStopped(final FMLServerStoppedEvent event) {
        SillyBeeAPI.EVENT_BUS.post(new ForwardedFMLServerStoppedEvent(event));
    }
}
