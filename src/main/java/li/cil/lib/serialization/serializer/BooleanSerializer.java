package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;

import javax.annotation.Nullable;

public final class BooleanSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Boolean.TYPE || clazz == Boolean.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagByte((byte) (((Boolean) object) ? 1 : 0));
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagByte) tag).getByte() != 0;
    }

    @Nullable
    @Override
    public Object getDefault() {
        return Boolean.FALSE;
    }
}
