package li.cil.lib.api.event;

import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

public class ForwardedFMLServerStoppedEvent extends ForwardedFMLEvent<FMLServerStoppedEvent> {
    public ForwardedFMLServerStoppedEvent(final FMLServerStoppedEvent event) {
        super(event);
    }
}
