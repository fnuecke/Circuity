package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusConnector;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.synchronization.value.SynchronizedLong;

import javax.annotation.Nullable;
import java.util.Collection;

public final class BusCable extends BusNeighborAware {
    private final BusCableImpl connector = new BusCableImpl();

    // Component ID of controller for client.
    public final SynchronizedLong controllerId = new SynchronizedLong();

    // --------------------------------------------------------------------- //

    public BusCable(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (connector.getBusController() != null) {
            connector.getBusController().scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //
    // BusDeviceHost

    @Override
    public BusDevice getBusDevice() {
        return connector;
    }

    // --------------------------------------------------------------------- //
    // BusNeighborAware

    @Nullable
    @Override
    protected BusController getController() {
        return connector.getBusController();
    }

    // --------------------------------------------------------------------- //

    public final class BusCableImpl extends AbstractBusDevice implements BusConnector {
        @Override
        public void setBusController(@Nullable final BusController controller) {
            super.setBusController(controller);
            BusCable.this.controllerId.set(getBusControllerId(controller));
        }

        @Override
        public boolean getConnected(final Collection<BusElement> devices) {
            return BusCable.this.getConnected(devices);
        }
    }
}
