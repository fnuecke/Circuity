package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;

import javax.annotation.Nullable;

public final class ByteArraySerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == byte[].class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagByteArray((byte[]) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final byte[] oldArray = (byte[]) object;
        final byte[] newArray = ((NBTTagByteArray) tag).getByteArray();
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
