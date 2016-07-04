package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;

import javax.annotation.Nullable;

public final class CharSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Character.TYPE || clazz == Character.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagInt((Character) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return (char) ((NBTTagInt) tag).getInt();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return (char) 0;
    }
}
