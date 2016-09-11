package li.cil.circuity.api.bus.controller.detail;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.controller.Subsystem;
import li.cil.circuity.api.bus.device.util.SerialPortManager;

/**
 * Specialized interface for subsystems contributing to the serial interface.
 * <p>
 * The main reason this is split out of the base interface is to avoid people
 * accidentally calling these methods from anywhere but the {@link BusController}
 * managing the subsystem.
 */
public interface SerialInterfaceProvider extends Subsystem {
    /**
     * Called during construction of the hosting {@link BusController} to build
     * the controller's serial interface.
     * <p>
     * Subsystems may register ports here if they wish to provide dynamic
     * functionality to the bus.
     *
     * @param manager  the manager to register ports with.
     * @param selector the instance tracking the selected device.
     */
    void initializeSerialInterface(final SerialPortManager manager, final SerialInterfaceDeviceSelector selector);
}
