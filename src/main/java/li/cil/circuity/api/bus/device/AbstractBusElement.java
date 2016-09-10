package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;

import javax.annotation.Nullable;

public abstract class AbstractBusElement implements BusElement {
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
}
