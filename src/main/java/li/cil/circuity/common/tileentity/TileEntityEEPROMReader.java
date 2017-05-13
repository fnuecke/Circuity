package li.cil.circuity.common.tileentity;

import li.cil.circuity.common.capabilities.eeprom.CapabilityEEPROM;
import li.cil.circuity.common.ecs.component.BusDeviceEEPROMReader;
import li.cil.lib.ecs.component.InventoryMutable;
import li.cil.lib.ecs.component.SimpleInventoryInteraction;
import li.cil.lib.tileentity.TileEntityEntityContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class TileEntityEEPROMReader extends TileEntityEntityContainer {
    @Override
    protected void addComponents() {
        super.addComponents();

        addComponent(InventoryMutable.class).setSize(1).setStackLimit(1).setFilter(TileEntityEEPROMReader::canInsertItem);
        addComponent(SimpleInventoryInteraction.class);
        addComponent(BusDeviceEEPROMReader.class);
    }

    private static boolean canInsertItem(final IItemHandler inventory, final int slot, final ItemStack stack) {
        return stack.hasCapability(CapabilityEEPROM.EEPROM_CAPABILITY, null);
    }
}
