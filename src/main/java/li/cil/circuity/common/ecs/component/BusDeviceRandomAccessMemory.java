package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedLong;

import javax.annotation.Nullable;
import java.util.Arrays;

@Serializable
public final class BusDeviceRandomAccessMemory extends AbstractComponentBusDevice {
    private static final byte[] EMPTY = new byte[0];

    @Serialize
    private final RandomAccessMemoryImpl device = new RandomAccessMemoryImpl();

    @Serialize
    private byte[] memory = EMPTY;

    // Component ID of controller for client.
    public final SynchronizedLong controllerId = new SynchronizedLong();

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

        if (device.getBusController() != null) {
            device.getBusController().scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusElement getBusElement() {
        return device;
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.READ_WRITE_MEMORY, Constants.DeviceInfo.RANDOM_ACCESS_MEMORY_NAME);

    public final class RandomAccessMemoryImpl extends AbstractBusDevice implements Addressable, AddressHint, BusStateListener {
        // --------------------------------------------------------------------- //
        // BusElement

        @Override
        public void setBusController(@Nullable final BusController controller) {
            super.setBusController(controller);
            BusDeviceRandomAccessMemory.this.controllerId.set(getBusControllerId(controller));
        }

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
            return memory.take(Constants.MEMORY_ADDRESS, BusDeviceRandomAccessMemory.this.memory.length);
        }

        @Override
        public int read(final long address) {
            return BusDeviceRandomAccessMemory.this.memory[(int) address] & 0xFF;
        }

        @Override
        public void write(final long address, final int value) {
            BusDeviceRandomAccessMemory.this.memory[(int) address] = (byte) value;
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
