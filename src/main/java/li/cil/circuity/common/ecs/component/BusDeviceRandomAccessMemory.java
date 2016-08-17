package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.Arrays;

@Serializable
public final class BusDeviceRandomAccessMemory extends AbstractComponentBusDevice {
    private static final byte[] EMPTY = new byte[0];

    @Serialize
    private final RandomAccessMemoryImpl device = new RandomAccessMemoryImpl();

    @Serialize
    private byte[] memory = EMPTY;

    // --------------------------------------------------------------------- //

    public BusDeviceRandomAccessMemory(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    public int getSize() {
        return memory.length;
    }

    public void setSize(final int bytes) {
        final byte[] newMemory = bytes > 0 ? new byte[bytes] : EMPTY;
        System.arraycopy(memory, 0, newMemory, 0, Math.min(memory.length, newMemory.length));
        memory = newMemory;

        if (device.getController() != null) {
            device.getController().scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    private static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.READ_WRITE_MEMORY);

    private final class RandomAccessMemoryImpl extends AbstractAddressable implements AddressHint, BusStateAware {
        // --------------------------------------------------------------------- //
        // AbstractAddressable

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(Constants.MEMORY_ADDRESS, BusDeviceRandomAccessMemory.this.memory.length * 8);
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
            return BusDeviceRandomAccessMemory.this.memory[address] & 0xFF;
        }

        @Override
        public void write(final int address, final int value) {
            BusDeviceRandomAccessMemory.this.memory[address] = (byte) value;
            BusDeviceRandomAccessMemory.this.markChanged();
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.MEMORY_ADDRESS;
        }

        // --------------------------------------------------------------------- //
        // BusStateAware

        @Override
        public void handleBusOnline() {
        }

        @Override
        public void handleBusOffline() {
            Arrays.fill(BusDeviceRandomAccessMemory.this.memory, (byte) 0);
        }
    }
}
