package li.cil.circuity.common.item;

import li.cil.circuity.common.capabilities.eeprom.EEPROMProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class ItemEEPROM extends Item {
    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, final NBTTagCompound nbt) {
        return new EEPROMProvider();
    }
}
