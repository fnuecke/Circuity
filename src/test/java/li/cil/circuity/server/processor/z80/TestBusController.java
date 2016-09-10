package li.cil.circuity.server.processor.z80;

import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.common.ecs.component.AbstractComponentBusDevice;
import li.cil.circuity.common.ecs.component.BusControllerBlock;
import li.cil.lib.api.ecs.manager.EntityComponentManager;

import java.util.Collection;

final class TestBusController extends BusControllerBlock {
    public TestBusController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    protected boolean getConnected(final Collection<BusElement> devices) {
        for (final AbstractComponentBusDevice component : TestBusController.this.getComponents(AbstractComponentBusDevice.class)) {
            devices.add(component.getBusDevice());
        }
        return true;
    }
}
