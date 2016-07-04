package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;

import javax.annotation.Nullable;

public final class StringSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == String.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagString((String) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagString) tag).getString();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
