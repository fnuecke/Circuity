package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.bus.AbstractBusController;
import li.cil.lib.api.ecs.component.LateTickable;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedBoolean;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

@Serializable
public final class BusControllerBlock extends BusNeighborAware implements ITickable, LateTickable {
    @Serialize
    private final BlockBusControllerImpl controller = new BlockBusControllerImpl();

    private final SynchronizedBoolean hasErrors = new SynchronizedBoolean();
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
        final World world = getWorld();
        if (!world.isRemote) {
            final boolean online = redstone.getInput(null) > 0;
            controller.setOnline(online);
            if (online) {
                controller.startUpdate();
            }

            isOnline.set(online);
            hasErrors.set(controller.hasErrors());
        } else {
            if (hasErrors.get()) {
                spawnParticle(EnumParticleTypes.FLAME);
            } else if (isOnline.get()) {
                spawnParticle(EnumParticleTypes.REDSTONE);
            }
        }
    }

    // --------------------------------------------------------------------- //
    // LateTickable

    @Override
    public void lateUpdate() {
        controller.finishUpdate();
    }

    // --------------------------------------------------------------------- //

    private void spawnParticle(final EnumParticleTypes particleType) {
        final Optional<Location> location = getComponent(Location.class);
        location.ifPresent(l -> {
            final Vec3d pos = l.getPositionVector();
            l.getWorld().spawnParticle(particleType, pos.xCoord, pos.yCoord + 0.5f, pos.zCoord, 0, 0.01f, 0);
        });
    }

    // --------------------------------------------------------------------- //

    private final class BlockBusControllerImpl extends AbstractBusController {
        @Override
        protected World getBusWorld() {
            return BusControllerBlock.this.getWorld();
        }

        @Override
        public boolean getDevices(final Collection<BusDevice> devices) {
            return BusControllerBlock.this.getDevices(devices);
        }
    }
}
