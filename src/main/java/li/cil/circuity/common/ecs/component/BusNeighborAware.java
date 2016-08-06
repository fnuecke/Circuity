package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.event.NeighborChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class BusNeighborAware extends AbstractComponentBusDevice implements NeighborChangeListener {
    private final Set<BusDevice> neighbors = new HashSet<>();

    protected BusNeighborAware(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // NeighborChangeListener

    @Override
    public final void handleNeighborChange(@Nullable final BlockPos neighborPos) {
        // Remote check here to avoid the neighbor scan; scheduleScan() also
        // does this check, so it's technically not necessary, but avoids some
        // unnecessary overhead on the client.
        final BusController controller = getController();
        if (controller != null && !getWorld().isRemote) {
            final Collection<BusDevice> devices = getDevicesCollection();
            if (neighbors.retainAll(devices) | neighbors.addAll(devices)) {
                controller.scheduleScan();
            }
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    protected abstract BusController getController();

    protected final Collection<BusDevice> getDevicesCollection() {
        final Optional<Location> location = getComponent(Location.class);
        return location.map(BusNeighborAware::getDevicesAt).orElse(Collections.emptySet());
    }

    // --------------------------------------------------------------------- //

    private static Collection<BusDevice> getDevicesAt(final Location location) {
        final World world = location.getWorld();
        final BlockPos pos = location.getPosition();

        final Set<BusDevice> devices = new HashSet<>();

        for (final EnumFacing side : EnumFacing.VALUES) {
            final BusDevice neighbor = getDeviceOnSide(world, pos, side);
            if (neighbor != null) {
                devices.add(neighbor);
            }
        }

        return devices;
    }

    @Nullable
    private static BusDevice getDeviceOnSide(final World world, final BlockPos pos, final EnumFacing side) {
        final BlockPos neighborPos = pos.offset(side);
        if (world.isBlockLoaded(neighborPos)) {
            final TileEntity tileEntity = world.getTileEntity(neighborPos);
            if (tileEntity != null) {
                if (tileEntity.hasCapability(CapabilityBusDevice.BUS_DEVICE_CAPABILITY, side)) {
                    return tileEntity.getCapability(CapabilityBusDevice.BUS_DEVICE_CAPABILITY, side);
                } else if (tileEntity instanceof BusDevice) {
                    return (BusDevice) tileEntity;
                }
            }
        }
        return null;
    }
}
