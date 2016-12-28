package li.cil.lib.serialization.serializer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public final class ItemStackSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == ItemStack.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        final ItemStack stack = (ItemStack) object;
        return stack.serializeNBT();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final NBTTagCompound compound = (NBTTagCompound) tag;
        if (object != null) {
            final ItemStack stack = (ItemStack) object;
            stack.deserializeNBT(compound);
            return stack;
        } else {
            return new ItemStack(compound);
        }
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
