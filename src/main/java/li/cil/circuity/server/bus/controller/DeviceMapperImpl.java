package li.cil.circuity.server.bus.controller;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.DeviceMapper;
import li.cil.circuity.api.bus.controller.detail.ElementManager;
import li.cil.circuity.api.bus.controller.detail.SerialInterfaceDeviceSelector;
import li.cil.circuity.api.bus.controller.detail.SerialInterfaceProvider;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.util.SerialPortManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Serializable
public class DeviceMapperImpl implements DeviceMapper, ElementManager, SerialInterfaceProvider, SerialInterfaceDeviceSelector {
    /**
     * The controller hosting this system.
     */
    private final AbstractBusController controller;

    /**
     * List of registered listeners for selection changes.
     */
    private final List<Consumer<BusDevice>> selectionChangeListeners = new ArrayList<>();

    /**
     * Direct access to bus devices based on their persistent ID.
     */
    private final Map<UUID, BusDevice> deviceById = new HashMap<>();

    /**
     * List of all currently known bus devices on the bus.
     * <p>
     * Sorted by device UUID, allows binary search and stable access from
     * serial interface by index.
     */
    private final List<BusDevice> devices = new ArrayList<>();

    /**
     * Currently selected device for reading its address via serial interface.
     */
    @Serialize
    private int selected;

    /**
     * Currently index in the name of the selected device for serial interface.
     */
    @Serialize
    private int nameIndex;

    // --------------------------------------------------------------------- //

    public DeviceMapperImpl(final AbstractBusController controller) {
        this.controller = controller;
    }

    // --------------------------------------------------------------------- //
    // DeviceMapper

    @Nullable
    @Override
    public BusDevice getDevice(final UUID persistentId) {
        return deviceById.get(persistentId);
    }

    // --------------------------------------------------------------------- //
    // ElementManager

    @Override
    public void add(final BusElement element) {
        if (element instanceof BusDevice) {
            final BusDevice device = (BusDevice) element;
            final int index = Collections.binarySearch(devices, device);
            assert index < 0 : "Device has been added twice.";
            devices.add(~index, device);
            deviceById.put(device.getPersistentId(), device);
        }
    }

    @Override
    public void remove(final BusElement element) {
        if (element instanceof BusDevice) {
            final BusDevice device = (BusDevice) element;
            final int index = Collections.binarySearch(devices, device);
            assert index >= 0 : "Device does not exist.";
            devices.remove(index);
            deviceById.remove(device.getPersistentId());
        }
    }

    // --------------------------------------------------------------------- //
    // Subsystem

    @Override
    public void reset() {
        selected = 0;
        nameIndex = 0;
    }

    @Override
    public boolean validate() {
        return true;
    }

    // --------------------------------------------------------------------- //
    // SerialInterfaceProvider

    @Override
    public void initializeSerialInterface(final SerialPortManager manager, final SerialInterfaceDeviceSelector selector) {
        manager.addSerialPort(this::readDeviceCount, null, null);
        manager.addSerialPort(this::readSelectedDevice, this::writeSelectedDevice, null);
        manager.addSerialPort(this::readDeviceType, null, null);
        manager.addSerialPort(this::readDeviceName, this::writeResetDeviceNameIndex, null);
    }

    // --------------------------------------------------------------------- //
    // SerialInterfaceDeviceSelector

    @Nullable
    @Override
    public BusDevice getSelectedDevice() {
        if (selected >= 0 && selected < devices.size()) {
            return devices.get(selected);
        } else {
            return null;
        }
    }

    @Override
    public void registerSelectionChangedListener(final Consumer<BusDevice> listener) {
        selectionChangeListeners.add(listener);
    }

    // --------------------------------------------------------------------- //

    private int readDeviceCount(final long address) {
        return devices.size();
    }

    private int readSelectedDevice(final long address) {
        return selected;
    }

    private void writeSelectedDevice(final long address, final int value) {
        selected = value;
        nameIndex = 0;

        for (final Consumer<BusDevice> listener : selectionChangeListeners) {
            listener.accept(getSelectedDevice());
        }

        controller.markChanged();
    }

    private int readDeviceType(final long address) {
        final BusDevice device = getSelectedDevice();
        if (device != null) {
            final DeviceInfo info = device.getDeviceInfo();
            return info != null ? info.type.id : 0xFFFFFFFF;
        }
        return 0xFFFFFFFF;
    }

    private int readDeviceName(final long address) {
        final BusDevice device = getSelectedDevice();
        if (device != null) {
            final DeviceInfo info = device.getDeviceInfo();
            final String name = info != null ? info.name : null;
            return name != null && nameIndex < name.length() ? name.charAt(nameIndex++) : 0;
        }
        return 0xFFFFFFFF;
    }

    private void writeResetDeviceNameIndex(final long address, final int value) {
        nameIndex = 0;

        controller.markChanged();
    }
}
