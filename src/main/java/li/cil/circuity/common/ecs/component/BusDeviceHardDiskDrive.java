package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.item.HardDiskDrive;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.capabilities.hdd.CapabilityHardDiskDrive;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.InventoryChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.ecs.component.InventoryMutable;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class BusDeviceHardDiskDrive extends AbstractComponentBusDevice implements InventoryChangeListener {
    @Serialize
    private final HardDiskDriveImpl device = new HardDiskDriveImpl();

    private ByteBuf data;
    private ScheduledCallback scheduledDataUpdate;

    // --------------------------------------------------------------------- //

    public BusDeviceHardDiskDrive(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return null;
    }

    // --------------------------------------------------------------------- //
    //InventoryChangeListener

    @Override
    public void handleInventoryChange(final IItemHandler inventory, final int slot) {

    }

    // --------------------------------------------------------------------- //

    private void scheduleUpdateData() {
        if (scheduledDataUpdate != null) return;

        final World world = getWorld();
        if (world.isRemote) return;

        scheduledDataUpdate = SillyBeeAPI.scheduler.schedule(world, this::updateData);
    }

    private void updateData() {
        scheduledDataUpdate = null;
        data = null;

        final IItemHandler inventory = getComponent(InventoryMutable.class).orElseThrow(IllegalStateException::new);
        final ItemStack stack = inventory.getStackInSlot(0);
        if (stack != null) {
            final HardDiskDrive medium = stack.getCapability(CapabilityHardDiskDrive.HARD_DISK_DRIVE_CAPABILITY, null);
            data = medium.getData();
        }

        final BusController controller = device.getController();
        if (controller != null) {
            controller.scheduleScan();
        }
    }
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.HARD_DISK_DRIVE);

    private final class HardDiskDriveImpl extends AbstractAddressable implements AddressHint {
        // --------------------------------------------------------------------- //
        // AbstractAddressable

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            final int size = data != null ? data.capacity() : 0;
            return memory.take(Constants.DISK_DRIVE_ADDRESS, size * 8);
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
            if (data != null) {
                switch (address) {
                    case 0xA0: { //
                        break;
                    }
                }
                return data.getByte(address);
            } else {
                return 0;
            }
        }

        @Override
        public void write(final int address, final int value) {
            if (data != null) {
                data.setByte(address, value);
            }
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.DISK_DRIVE_ADDRESS;
        }
    }
}
