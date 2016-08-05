package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedByteArray;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

@Serializable
public final class SidedRedstoneController extends AbstractRedstoneController {
    @Serialize
    private final SynchronizedByteArray output = new SynchronizedByteArray(EnumFacing.VALUES.length + 1);

    // --------------------------------------------------------------------- //

    public SidedRedstoneController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Redstone

    @Override
    public int getOutput(@Nullable final EnumFacing side) {
        return output.get(side != null ? side.getIndex() : EnumFacing.VALUES.length) & 0xFF;
    }

    @Override
    public void setOutput(@Nullable final EnumFacing side, final int output) {
        final byte clampedOutput = clampSignal(output);
        if (clampedOutput == getOutput(side)) return;
        this.output.set(side != null ? side.getIndex() : EnumFacing.VALUES.length, clampedOutput);
        scheduleNotifyNeighbors();
        markChanged();
    }
}
