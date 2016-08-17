package li.cil.lib.api.event;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class ForwardedFMLServerStartingEvent extends ForwardedFMLEvent<FMLServerStartingEvent> {
    public ForwardedFMLServerStartingEvent(final FMLServerStartingEvent event) {
        super(event);
    }
}
