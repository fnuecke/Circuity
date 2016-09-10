package li.cil.circuity.common.capabilities;

import li.cil.circuity.api.bus.BusElement;
import li.cil.lib.capabilities.NullFactory;
import li.cil.lib.capabilities.NullStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class CapabilityBusElement {
    @CapabilityInject(BusElement.class)
    public static Capability<BusElement> BUS_ELEMENT_CAPABILITY;

    // --------------------------------------------------------------------- //

    public static void register() {
        CapabilityManager.INSTANCE.register(BusElement.class, new NullStorage<>(), NullFactory::create);
    }

    // --------------------------------------------------------------------- //

    private CapabilityBusElement() {
    }
}
