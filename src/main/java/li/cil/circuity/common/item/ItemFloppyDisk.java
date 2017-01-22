package li.cil.circuity.common.item;

import li.cil.circuity.common.capabilities.fdd.FloppyDiskProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public class ItemFloppyDisk extends Item {
    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final NBTTagCompound nbt) {
        return new FloppyDiskProvider();
    }
}
