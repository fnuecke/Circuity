package li.cil.lib.api.event;

import net.minecraftforge.fml.common.event.FMLEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class ForwardedFMLEvent<T extends FMLEvent> extends Event {
    public final T event;

    public ForwardedFMLEvent(final T event) {
        this.event = event;
    }
}
