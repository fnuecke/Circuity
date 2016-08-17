package li.cil.lib.api.event;

import net.minecraftforge.fml.common.event.FMLServerStartedEvent;

public class ForwardedFMLServerStartedEvent extends ForwardedFMLEvent<FMLServerStartedEvent> {
    public ForwardedFMLServerStartedEvent(final FMLServerStartedEvent event) {
        super(event);
    }
}
