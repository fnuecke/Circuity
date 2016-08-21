package li.cil.lib.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ItemUtil {
    public static NBTTagCompound getOrAddTagCompound(final ItemStack stack) {
        final NBTTagCompound tag;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(tag = new NBTTagCompound());
        } else {
            tag = stack.getTagCompound();
            assert tag != null : "ItemStack#hasTackCompound lied to me!";
        }
        return tag;
    }

    private ItemUtil() {
    }
}
