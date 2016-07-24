package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusSegment;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.event.NeighborChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.ecs.component.AbstractComponent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BlockBusSegment extends AbstractComponent implements NeighborChangeListener, ICapabilityProvider {
    protected final BlockBusSegmentImpl segment = new BlockBusSegmentImpl();
    private final Set<BusDevice> neighbors = new HashSet<>();

    // --------------------------------------------------------------------- //

    public BlockBusSegment(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onDestroy() {
        if (segment.getController() != null) {
            segment.getController().scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //
    // NeighborChangeListener

    @Override
    public void onNeighborChange(@Nullable final BlockPos neighborPos) {
        // Remote check here to avoid the neighbor scan; scheduleScan() also
        // does this check, so it's technically not necessary, but avoids some
        // unnecessary overhead on the client.
        if (segment.getController() != null && !getWorld().isRemote) {
            final Collection<BusDevice> devices = getDevicesCollection();
            if (neighbors.retainAll(devices) | neighbors.addAll(devices)) {
                segment.getController().scheduleScan();
            }
        }
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY) {
            return CapabilityBusDevice.BUS_DEVICE_CAPABILITY.cast(segment);
        }
        return null;
    }

    // --------------------------------------------------------------------- //

    protected final class BlockBusSegmentImpl extends AbstractBusDevice implements BusSegment {
        @Override
        public Iterable<BusDevice> getDevices() {
            return BlockBusSegment.this.getDevicesCollection();
        }
    }

    private Collection<BusDevice> getDevicesCollection() {
        final Optional<Location> location = BlockBusSegment.this.getComponent(Location.class);
        return location.map(BlockBusSegment::getDevicesAt).orElse(Collections.emptySet());
    }

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
