package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;

import javax.annotation.Nullable;

public abstract class AbstractBusDevice implements BusDevice {
    protected BusController controller;

    @Override
    public void setBusController(@Nullable final BusController controller) {
        this.controller = controller;
    }
}
