package li.cil.circuity.api.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.lib.api.ecs.component.Component;

public interface BusDeviceHost extends Component {
    BusDevice getBusDevice();
}
