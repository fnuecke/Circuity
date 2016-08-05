package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedByte;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

@Serializable
public final class RedstoneController extends AbstractRedstoneController {
    @Serialize
    private final SynchronizedByte output = new SynchronizedByte();

    // --------------------------------------------------------------------- //

    public RedstoneController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Redstone

    @Override
    public int getOutput(@Nullable final EnumFacing side) {
        return output.get();
    }

    @Override
    public void setOutput(@Nullable final EnumFacing side, final int output) {
        final byte clampedOutput = clampSignal(output);
        if (clampedOutput == getOutput(side)) return;
        this.output.set(clampedOutput);
        scheduleNotifyNeighbors();
        markChanged();
    }
}
