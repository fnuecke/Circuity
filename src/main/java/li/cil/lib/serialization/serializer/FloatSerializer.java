package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagFloat;

import javax.annotation.Nullable;

public final class FloatSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Float.TYPE || clazz == Float.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagFloat((Float) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagFloat) tag).getFloat();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return 0f;
    }
}
