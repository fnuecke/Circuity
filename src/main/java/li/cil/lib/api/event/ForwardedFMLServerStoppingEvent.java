package li.cil.lib.api.event;

import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

public class ForwardedFMLServerStoppingEvent extends ForwardedFMLEvent<FMLServerStoppingEvent> {
    public ForwardedFMLServerStoppingEvent(final FMLServerStoppingEvent event) {
        super(event);
    }
}
