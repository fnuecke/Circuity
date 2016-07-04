package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;

import javax.annotation.Nullable;

public final class ByteSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Byte.TYPE || clazz == Byte.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagByte((Byte) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagByte) tag).getByte();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return (byte) 0;
    }
}
