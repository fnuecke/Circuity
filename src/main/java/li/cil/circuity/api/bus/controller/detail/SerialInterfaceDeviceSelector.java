package li.cil.circuity.api.bus.controller.detail;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Implemented by the {@link li.cil.circuity.api.bus.controller.DeviceMapper},
 * provides access to the currently selected device and a callback when it
 * changes for other {@link SerialInterfaceProvider}s.
 * <p>
 * The main reason this is split out of the base interface is to avoid people
 * accidentally calling these methods from anywhere but the {@link BusController}
 * managing the subsystem.
 */
public interface SerialInterfaceDeviceSelector {
    /**
     * The currently selected bus device, if any.
     *
     * @return the selected bus device, or <code>null</code>.
     */
    @Nullable
    BusDevice getSelectedDevice();

    /**
     * Register a method to be called when the selected device changes.
     *
     * @param listener the listener to register.
     */
    void registerSelectionChangedListener(final Consumer<BusDevice> listener);
}
