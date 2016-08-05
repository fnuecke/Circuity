package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

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

    private final class RandomAccessMemoryImpl extends AbstractAddressable {
        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(BusDeviceRandomAccessMemory.this.memory.length * 8);
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
    }
}
