package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

@Serializable
public final class RedstoneController extends AbstractRedstoneController {
    @Serialize
    private byte output;

    // --------------------------------------------------------------------- //

    public RedstoneController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Redstone

    @Override
    public int getOutput(@Nullable final EnumFacing side) {
        return output & 0xFF;
    }

    @Override
    public void setOutput(@Nullable final EnumFacing side, final int value) {
        final byte clampedOutput = clampSignal(value);
        if (clampedOutput == getOutput(side)) {
            return;
        }
        output = clampedOutput;
        scheduleNotifyNeighbors();
        markChanged();
    }
}
