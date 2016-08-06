package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.bus.AbstractBusController;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.world.World;

import javax.annotation.Nullable;

@Serializable
public final class BusControllerBlock extends BusNeighborAware {
    @Serialize
    private final BlockBusControllerImpl controller = new BlockBusControllerImpl();

    // --------------------------------------------------------------------- //

    public BusControllerBlock(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onCreate() {
        super.onCreate();

        controller.scheduleScan();
    }

    // No need to call super.onDestroy() here, because our controller is by
    // necessity our own, so we really don't need to schedule a scan anymore.
    @Override
    public void onDestroy() {
        super.onDestroy();

        controller.clear();
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return controller;
    }

    // --------------------------------------------------------------------- //
    // BusNeighborAware

    @Nullable
    @Override
    protected BusController getController() {
        return controller;
    }

    // --------------------------------------------------------------------- //

    private final class BlockBusControllerImpl extends AbstractBusController {
        @Override
        protected World getBusWorld() {
            return BusControllerBlock.this.getWorld();
        }

        @Override
        public Iterable<BusDevice> getDevices() {
            return BusControllerBlock.this.getDevicesCollection();
        }
    }
}
