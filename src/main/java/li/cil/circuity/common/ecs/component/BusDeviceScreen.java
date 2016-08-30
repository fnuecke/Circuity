package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.*;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

@Serializable
public final class BusDeviceScreen extends AbstractComponentBusDevice {
    @Serialize
    private final ScreenImpl device = new ScreenImpl();

    // --------------------------------------------------------------------- //

    public BusDeviceScreen(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SCREEN, Constants.DeviceInfo.SCREEN_NAME);

    public final class ScreenImpl extends AbstractAddressable implements AddressHint {
        // --------------------------------------------------------------------- //
        // AbstractAddressable

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(Constants.SCREEN_ADDRESS, 1);
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
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.SCREEN_ADDRESS;
        }
    }
}
