package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceI8080Processor;
import li.cil.lib.ecs.component.RedstoneController;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityProcessorI8080 extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(RedstoneController.class);
        addComponent(BusDeviceI8080Processor.class);
    }
}
