package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceSerialConsole;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntitySerialConsole extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(BusDeviceSerialConsole.class);
    }
}
