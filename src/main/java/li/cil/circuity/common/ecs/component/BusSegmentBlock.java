package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusSegment;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;

import javax.annotation.Nullable;

public final class BusSegmentBlock extends BusNeighborAware {
    private final BlockBusSegmentImpl segment = new BlockBusSegmentImpl();

    // --------------------------------------------------------------------- //

    public BusSegmentBlock(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (segment.getController() != null) {
            segment.getController().scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return segment;
    }

    // --------------------------------------------------------------------- //
    // BusNeighborAware

    @Nullable
    @Override
    protected BusController getController() {
        return segment.getController();
    }

    // --------------------------------------------------------------------- //

    private final class BlockBusSegmentImpl extends AbstractBusDevice implements BusSegment {
        @Override
        public Iterable<BusDevice> getDevices() {
            return BusSegmentBlock.this.getDevicesCollection();
        }
    }
}
