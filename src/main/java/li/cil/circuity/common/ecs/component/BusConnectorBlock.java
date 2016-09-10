package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusConnector;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;

import javax.annotation.Nullable;
import java.util.Collection;

public final class BusConnectorBlock extends BusNeighborAware {
    private final BlockBusConnectorImpl connector = new BlockBusConnectorImpl();

    // --------------------------------------------------------------------- //

    public BusConnectorBlock(final EntityComponentManager manager, final long entity, final long id) {
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

    public final class BlockBusConnectorImpl extends AbstractBusDevice implements BusConnector {
        @Override
        public boolean getConnected(final Collection<BusElement> devices) {
            return BusConnectorBlock.this.getConnected(devices);
        }
    }
}
