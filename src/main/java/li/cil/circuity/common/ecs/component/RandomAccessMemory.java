package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.ecs.component.AbstractComponent;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

@Serializable
public final class RandomAccessMemory extends AbstractComponent implements ICapabilityProvider {
    private final RandomAccessMemoryImpl device = new RandomAccessMemoryImpl();

    @Serialize
    private byte[] memory = new byte[0];

    // --------------------------------------------------------------------- //

    public RandomAccessMemory(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    public int getSize() {
        return memory.length;
    }

    public void setSize(final int bytes) {
        final byte[] newMemory = new byte[bytes];
        System.arraycopy(memory, 0, newMemory, 0, Math.min(memory.length, newMemory.length));
        memory = newMemory;

        if (device.getController() != null) {
            device.getController().scheduleScan();
        }
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY;
    }

    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY) {
            return CapabilityBusDevice.BUS_DEVICE_CAPABILITY.cast(device);
        }
        return null;
    }

    // --------------------------------------------------------------------- //

    private final class RandomAccessMemoryImpl extends AbstractAddressable implements Addressable {
        @Override
        protected AddressBlock validateAddress(final AddressBlock address) {
            return address.take(RandomAccessMemory.this.memory.length * 8);
        }

        @Override
        public int read(final int address) {
            return RandomAccessMemory.this.memory[address] & 0xFF;
        }

        @Override
        public void write(final int address, final int value) {
            RandomAccessMemory.this.memory[address] = (byte) value;
        }
    }
}
