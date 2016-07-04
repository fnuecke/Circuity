package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;

import javax.annotation.Nullable;

public final class IntSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Integer.TYPE || clazz == Integer.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagInt((Integer) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagInt) tag).getInt();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return 0;
    }
}
