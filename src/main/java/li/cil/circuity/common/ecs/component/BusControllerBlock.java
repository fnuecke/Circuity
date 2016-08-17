package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.bus.AbstractBusController;
import li.cil.lib.api.ecs.component.LateTickable;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedBoolean;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

import javax.annotation.Nullable;

@Serializable
public final class BusControllerBlock extends BusNeighborAware implements ITickable, LateTickable {
    @Serialize
    private final BlockBusControllerImpl controller = new BlockBusControllerImpl();

    private final SynchronizedBoolean isOnline = new SynchronizedBoolean();

    private Redstone redstone;

    // --------------------------------------------------------------------- //

    public BusControllerBlock(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onCreate() {
        super.onCreate();

        redstone = getComponent(Redstone.class).orElseThrow(IllegalStateException::new);
        controller.scheduleScan();
    }

    // No need to call super.onDestroy() here, because our controller is by
    // necessity our own, so we really don't need to schedule a scan anymore.
    @Override
    public void onDestroy() {
        super.onDestroy();

        redstone = null;
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
    // ITickable

    @Override
    public void update() {
        final boolean online = redstone.getInput(null) > 0;
        isOnline.set(online);
        controller.setOnline(online);
        if (online) {
            controller.startUpdate();
        }
    }

    // --------------------------------------------------------------------- //
    // LateTickable

    @Override
    public void lateUpdate() {
        controller.finishUpdate();
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
