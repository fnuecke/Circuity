package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

public class BusDeviceScreen extends AbstractComponentBusDevice {
    @Serialize
    private final ScreenImpl device = new ScreenImpl();

    public BusDeviceScreen(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public BusDevice getDevice() {
        return device;
    }

    @Serializable
    public final class ScreenImpl extends AbstractBusDevice {
    }
}
