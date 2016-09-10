package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.component.BusDeviceHost;
import li.cil.lib.api.ecs.component.Component;

/**
 * This is a utility interface that allows devices hosted by a {@link Component}
 * to expose their hosting component. This is used by a number of internal
 * classes to synchronize references to other bus devices to the client via
 * their hosting component, and then accessing the device again on the client
 * via the {@link BusDeviceHost} interface.
 */
public interface ComponentHosted extends BusDevice {
    /**
     * The component hosting this bus device.
     * <p>
     * The hosting component is typically a wrapper component, representing the
     * {@link BusDevice} in its {@link li.cil.lib.api.ecs.manager.EntityComponentManager}.
     *
     * @return the hosting component.
     */
    Component getHostComponent();
}
