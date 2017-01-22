package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.device.InterruptSource;

public interface InterruptSourceProxy extends InterruptSource {
    default void triggerInterrupt(final int index, final int data) {
        final BusController controller = getBusController();
        if (controller == null) {
            return;
        }

        final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
        if (mapper == null) {
            return;
        }

        mapper.interrupt(this, index, data);
    }
}
