package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.capabilities.fdd.CapabilityFloppyDisk;
import li.cil.circuity.common.ecs.component.BusDeviceFloppyDiskDrive;
import li.cil.lib.ecs.component.InventoryMutable;
import li.cil.lib.tileentity.TileEntityEntityContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class TileEntityFloppyDiskDrive extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(InventoryMutable.class).setSize(1).setStackLimit(1).setFilter(TileEntityFloppyDiskDrive::canInsertItem);
        addComponent(BusDeviceFloppyDiskDrive.class);
    }

    private static boolean canInsertItem(final IItemHandler inventory, final int slot, final ItemStack stack) {
        return stack.hasCapability(CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY, null);
    }
}
