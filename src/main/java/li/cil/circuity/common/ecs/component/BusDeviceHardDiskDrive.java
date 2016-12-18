package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
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

public final class BusDeviceHardDiskDrive extends AbstractComponentBusDevice implements InventoryChangeListener {
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
    public BusElement getBusElement() {
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
        if (!stack.isEmpty()) {
            final HardDiskDrive medium = stack.getCapability(CapabilityHardDiskDrive.HARD_DISK_DRIVE_CAPABILITY, null);
            if (medium != null) {
                data = medium.getData();
            }
        }

        final BusController controller = device.getBusController();
        if (controller != null) {
            controller.scheduleScan();
        }
    }

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.HARD_DISK_DRIVE, Constants.DeviceInfo.HARD_DISK_DRIVE_NAME);

    public final class HardDiskDriveImpl extends AbstractBusDevice implements Addressable, AddressHint {
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
            final int size = data != null ? data.capacity() : 0;
            return memory.take(Constants.DISK_DRIVE_ADDRESS, size);
        }

        @Override
        public int read(final long address) {
            if (data != null) {
                switch ((int) address) {
                    case 0xA0: { //
                        break;
                    }
                }
                return data.getByte((int) address);
            } else {
                return 0xFFFFFFFF;
            }
        }

        @Override
        public void write(final long address, final int value) {
            if (data != null) {
                data.setByte((int) address, value);
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
