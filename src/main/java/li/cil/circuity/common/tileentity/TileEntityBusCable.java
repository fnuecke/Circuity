package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BlockBusSegment;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityBusCable extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BlockBusSegment.class);
    }
}
