package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagLong;

import javax.annotation.Nullable;

public final class LongSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Long.TYPE || clazz == Long.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagLong((Long) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagLong) tag).getLong();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return 0L;
    }
}
