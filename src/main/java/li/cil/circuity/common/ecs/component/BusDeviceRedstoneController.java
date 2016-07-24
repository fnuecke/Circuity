package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import java.util.Optional;

/**
 * Provides access to redstone input and output via serial interface.
 * <p>
 * Mapped memory:
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
    protected BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    private final class RedstoneControllerImpl extends AbstractAddressable implements Addressable {
        @Override
        protected AddressBlock validateAddress(final AddressBlock address) {
            return address.take(2 * 8);
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
    }
}
