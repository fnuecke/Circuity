package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.event.NeighborChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.util.CapabilityUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
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
            final HashSet<BusDevice> devices = new HashSet<>();
            getDevices(devices);
            if (neighbors.retainAll(devices) | neighbors.addAll(devices)) {
                controller.scheduleScan();
            }
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    protected abstract BusController getController();

    protected final boolean getDevices(final Collection<BusDevice> devices) {
        final Optional<Location> location = getComponent(Location.class);
        return !location.isPresent() || getDevicesAt(location.get(), devices);
    }

    private static boolean getDevicesAt(final Location location, final Collection<BusDevice> devices) {
        final World world = location.getWorld();
        final BlockPos pos = location.getPosition();

        for (final EnumFacing side : EnumFacing.VALUES) {
            final BlockPos neighborPos = pos.offset(side);
            if (!world.isBlockLoaded(neighborPos)) {
                return false;
            }

            final BusDevice neighbor = CapabilityUtil.getCapability(world, neighborPos, side, CapabilityBusDevice.BUS_DEVICE_CAPABILITY, BusDevice.class);
            if (neighbor != null) {
                devices.add(neighbor);
            }
        }

        return true;
    }
}
