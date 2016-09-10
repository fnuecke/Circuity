package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Provides access to redstone input and output via serial interface.
 * <p>
 * Ports:
 * <table>
 * <tr><td>0</td><td>Current redstone signal received by the device. Read-only.</td></tr>
 * <tr><td>1</td><td>Current redstone signal emitted by the device. Read-write.</td></tr>
 * </table>
 */
@Serializable
public final class BusDeviceRedstoneController extends AbstractComponentBusDevice {
    @Serialize
    private final RedstoneControllerImpl device = new RedstoneControllerImpl();

    // --------------------------------------------------------------------- //

    public BusDeviceRedstoneController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.REDSTONE_CONTROLLER, Constants.DeviceInfo.REDSTONE_CONTROLLER_NAME);

    public final class RedstoneControllerImpl extends AbstractBusDevice implements Addressable, AddressHint {
        // --------------------------------------------------------------------- //
        // BusDevice

        @Nullable
        @Override
        public DeviceInfo getDeviceInfo() {
            return DEVICE_INFO;
        }

        // --------------------------------------------------------------------- //
        // Addressable

        @Override
        public AddressBlock getPreferredAddressBlock(final AddressBlock memory) {
            return memory.take(Constants.REDSTONE_CONTROLLER_ADDRESS, 2);
        }

        @Override
        public int read(final int address) {
            switch (address) {
                case 0: {
                    final Optional<Redstone> redstone = BusDeviceRedstoneController.this.getComponent(Redstone.class);
                    return redstone.map(r -> r.getInput(null)).orElse(0);
                }
                case 1: {
                    final Optional<Redstone> redstone = BusDeviceRedstoneController.this.getComponent(Redstone.class);
                    return redstone.map(r -> r.getOutput(null)).orElse(0);
                }
            }
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
            switch (address) {
                case 1: {
                    final Optional<Redstone> redstone = BusDeviceRedstoneController.this.getComponent(Redstone.class);
                    redstone.ifPresent(r -> r.setOutput(null, value));
                    break;
                }
            }
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.REDSTONE_CONTROLLER_ADDRESS;
        }
    }
}
