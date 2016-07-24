package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusControllerBlock;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityBusController extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BusControllerBlock.class);
    }
}
