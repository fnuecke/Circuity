package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.UUID;

@Serializable
public abstract class AbstractBusDevice implements BusDevice {
    @Serialize
    protected UUID persistentId = UUID.randomUUID();
    protected BusController controller;

    // --------------------------------------------------------------------- //
    // BusElement

    @Nullable
    @Override
    public BusController getBusController() {
        return controller;
    }

    @Override
    public void setBusController(@Nullable final BusController controller) {
        this.controller = controller;
    }

    // --------------------------------------------------------------------- //
    // BusDevice

    @Override
    public UUID getPersistentId() {
        return persistentId;
    }

    @Nullable
    @Override
    public DeviceInfo getDeviceInfo() {
        return null;
    }
}
