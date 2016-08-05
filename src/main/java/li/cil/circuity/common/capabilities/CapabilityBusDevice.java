package li.cil.circuity.common.capabilities;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.lib.capabilities.NullFactory;
import li.cil.lib.capabilities.NullStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class CapabilityBusDevice {
    @CapabilityInject(BusDevice.class)
    public static Capability<BusDevice> BUS_DEVICE_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(BusDevice.class, new NullStorage<>(), NullFactory::create);
    }
}
