package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceProcessorZ80;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityProcessorZ80 extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BusDeviceProcessorZ80.class);
    }
}
