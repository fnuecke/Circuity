package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagDouble;

import javax.annotation.Nullable;

public final class DoubleSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == Double.TYPE || clazz == Double.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagDouble((Double) object);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return ((NBTTagDouble) tag).getDouble();
    }

    @Nullable
    @Override
    public Object getDefault() {
        return 0.0;
    }
}
