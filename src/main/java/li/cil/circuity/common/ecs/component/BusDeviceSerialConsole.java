package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

public class BusDeviceSerialConsole extends AbstractComponentBusDevice {
    @Serialize
    private final SerialConsoleImpl device = new SerialConsoleImpl();

    public BusDeviceSerialConsole(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    private static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SERIAL_INTERFACE);

    private static final class SerialConsoleImpl extends AbstractAddressable implements AddressHint {
        // --------------------------------------------------------------------- //
        // AbstractAddressable

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(Constants.SERIAL_CONSOLE_ADDRESS, 8);
        }

        // --------------------------------------------------------------------- //
        // Addressable

        @Nullable
        @Override
        public DeviceInfo getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Override
        public int read(final int address) {
            switch (address) {
                case 0: {
                    return 0;
                }
            }
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
            switch (address) {
                case 0: {
                    final char ch = (char) value;
                    System.out.print(ch);
                    break;
                }
            }
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.SERIAL_CONSOLE_ADDRESS;
        }
    }
}
