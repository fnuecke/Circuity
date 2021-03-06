package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceRandomAccessMemory;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityRandomAccessMemory extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
    }
}
