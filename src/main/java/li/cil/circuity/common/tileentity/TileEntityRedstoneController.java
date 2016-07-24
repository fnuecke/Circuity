package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.ecs.component.BusDeviceRedstoneController;
import li.cil.lib.ecs.component.RedstoneController;
import li.cil.lib.tileentity.TileEntityEntityContainer;

public class TileEntityRedstoneController extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(RedstoneController.class);
        addComponent(BusDeviceRedstoneController.class);
    }
}
