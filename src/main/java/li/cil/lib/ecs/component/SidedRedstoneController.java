package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

@Serializable
public final class SidedRedstoneController extends AbstractRedstoneController {
    @Serialize
    private final byte[] output = new byte[EnumFacing.VALUES.length + 1];

    // --------------------------------------------------------------------- //

    public SidedRedstoneController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Redstone

    @Override
    public int getOutput(@Nullable final EnumFacing side) {
        return output[side != null ? side.getIndex() : EnumFacing.VALUES.length] & 0xFF;
    }

    @Override
    public void setOutput(@Nullable final EnumFacing side, final int value) {
        final byte clampedOutput = clampSignal(value);
        if (clampedOutput == getOutput(side)) {
            return;
        }
        output[side != null ? side.getIndex() : EnumFacing.VALUES.length] = clampedOutput;
        scheduleNotifyNeighbors();
        markChanged();
    }
}
