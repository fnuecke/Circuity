package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.bus.AddressBlock;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.item.EEPROM;
import li.cil.circuity.common.capabilities.CapabilityEEPROM;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.InventoryChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.ecs.component.InventoryMutable;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

@Serializable
public class BusDeviceEEPROMReader extends AbstractComponentBusDevice implements InventoryChangeListener {
    @Serialize
    private final EEPROMImpl device = new EEPROMImpl();

    private ByteBuf data;
    private ScheduledCallback scheduledDataUpdate;

    // --------------------------------------------------------------------- //

    public BusDeviceEEPROMReader(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        scheduleUpdateData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (scheduledDataUpdate != null) {
            SillyBeeAPI.scheduler.cancel(getWorld(), scheduledDataUpdate);
            scheduledDataUpdate = null;
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //
    // InventoryChangeListener

    @Override
    public void handleInventoryChange(final IItemHandler inventory, final int slot) {
        scheduleUpdateData();
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
            final EEPROM eeprom = stack.getCapability(CapabilityEEPROM.EEPROM_CAPABILITY, null);
            data = eeprom.getData();
        }

        final BusController controller = device.getController();
        if (controller != null) {
            controller.scheduleScan();
        }
    }

    private final class EEPROMImpl extends AbstractAddressable implements AddressHint {
        private static final int DEVICE_ADDRESS_OFFSET = 0xC100;

        // --------------------------------------------------------------------- //
        // AbstractAddressable

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            final int size = data != null ? data.capacity() : 0;
            return memory.take(DEVICE_ADDRESS_OFFSET, size * 8);
        }

        // --------------------------------------------------------------------- //
        // Addressable

        @Override
        public int read(final int address) {
            if (data != null) {
                return data.getByte(address);
            } else {
                return 0;
            }
        }

        @Override
        public void write(final int address, final int value) {
            // It's a *reader*... for now, anyway.
//            if (data != null) {
//                data.setByte(address, value);
//            }
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return DEVICE_ADDRESS_OFFSET;
        }
    }
}
