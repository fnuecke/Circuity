package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceScreen;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityScreen extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BusDeviceScreen.class);
    }
}
