package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceZ80Processor;
import li.cil.lib.ecs.component.RedstoneController;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityProcessorZ80 extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(RedstoneController.class);
        addComponent(BusDeviceZ80Processor.class);
    }
}
