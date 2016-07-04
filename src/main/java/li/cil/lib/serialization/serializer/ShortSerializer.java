package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagShort;

import javax.annotation.Nullable;

public final class ShortSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Short.TYPE || clazz == Short.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagShort((Short) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagShort) tag).getShort();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return (short) 0;
    }
}
