package li.cil.circuity.server.processor.z80;

import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.ecs.component.AbstractComponent;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

final class TestRedstone extends AbstractComponent implements Redstone {
    private int input = 0;

    public TestRedstone(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public int getInput(@Nullable final EnumFacing side) {
        return input;
    }

    public void setInput(final int value) {
        input = value;
    }

    @Override
    public int getOutput(@Nullable final EnumFacing side) {
        return 0;
    }

    @Override
    public void setOutput(@Nullable final EnumFacing side, final int value) {
    }
}
