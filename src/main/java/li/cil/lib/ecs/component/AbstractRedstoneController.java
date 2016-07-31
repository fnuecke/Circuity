package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.component.event.NeighborChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.synchronization.value.SynchronizedByteArray;
import li.cil.lib.util.Scheduler;
import li.cil.lib.util.SpatialUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class AbstractRedstoneController extends AbstractComponent implements Redstone, NeighborChangeListener {
    private final SynchronizedByteArray input = new SynchronizedByteArray(EnumFacing.VALUES.length);

    // --------------------------------------------------------------------- //

    protected AbstractRedstoneController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onCreate() {
        final Optional<Location> location = getComponent(Location.class);
        location.ifPresent(this::initializeInput);
    }

    // --------------------------------------------------------------------- //
    // Redstone

    @Override
    public int getInput(@Nullable final EnumFacing side) {
        return (side != null ? input.get(side.getIndex()) : getMaxInput()) & 0xFF;
    }

    // --------------------------------------------------------------------- //
    // NeighborChangeListener

    @Override
    public void handleNeighborChange(@Nullable final BlockPos neighborPos) {
        final Optional<Location> location = getComponent(Location.class);
        if (neighborPos != null) {
            location.ifPresent(l -> updateInput(l, neighborPos));
        } else {
            location.ifPresent(l -> recomputeInput(l.getWorld(), l.getPosition()));
        }
    }

    // --------------------------------------------------------------------- //

    protected static void notifyNeighbors(final Location location) {
        final World world = location.getWorld();
        final BlockPos pos = location.getPosition();
        final IBlockState state = world.getBlockState(pos);
        world.notifyNeighborsOfStateChange(pos, state.getBlock());
    }

    protected static byte clampSignal(final int value) {
        if (value < 0) return (byte) 0;
        if (value > 0xFF) return (byte) 0xFF;
        return (byte) value;
    }

    private int getMaxInput() {
        int max = 0;
        for (final EnumFacing value : EnumFacing.VALUES) {
            max = Math.max(max, input.get(value.getIndex()));
        }
        return max;
    }

    private void initializeInput(final Location location) {
        final World world = location.getWorld();
        final BlockPos pos = location.getPosition();
        Scheduler.schedule(world, () -> recomputeInput(world, pos));
    }

    private void updateInput(final Location location, final BlockPos neighborPos) {
        final World world = location.getWorld();
        final BlockPos pos = location.getPosition();
        final EnumFacing side = SpatialUtil.getNeighborFacing(pos, neighborPos);

        final byte input = clampSignal(world.getRedstonePower(pos.offset(side), side));
        if (input == getInput(side)) {
            return;
        }

        if (input > getInput(side)) {
            this.input.set(side.getIndex(), input);
        } else {
            recomputeInput(world, pos);
        }
    }

    private void recomputeInput(final World world, final BlockPos pos) {
        for (final EnumFacing side : EnumFacing.VALUES) {
            final byte input = clampSignal(world.getRedstonePower(pos.offset(side), side));
            this.input.set(side.getIndex(), input);
        }
    }
}
