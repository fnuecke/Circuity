package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.UUID;

@Serializable
public abstract class AbstractBusDevice extends AbstractBusElement implements BusDevice {
    @Serialize
    protected UUID persistentId = UUID.randomUUID();

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

    // --------------------------------------------------------------------- //
    // Comparable

    @Override
    public int compareTo(final BusDevice that) {
        return this.getPersistentId().compareTo(that.getPersistentId());
    }
}
