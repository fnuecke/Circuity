package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceMips3Processor;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityProcessorMips3 extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BusDeviceMips3Processor.class);
    }
}
