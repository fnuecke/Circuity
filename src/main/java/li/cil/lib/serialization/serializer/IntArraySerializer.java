package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagIntArray;

import javax.annotation.Nullable;

public final class IntArraySerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == int[].class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagIntArray((int[]) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final int[] oldArray = (int[]) object;
        final int[] newArray = ((NBTTagIntArray) tag).getIntArray();
        if (oldArray != null && oldArray.length == newArray.length) {
            System.arraycopy(newArray, 0, oldArray, 0, oldArray.length);
            return oldArray;
        } else {
            return newArray;
        }
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
