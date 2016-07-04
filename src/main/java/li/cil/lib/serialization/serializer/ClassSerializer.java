package li.cil.lib.serialization.serializer;

import li.cil.lib.api.SillyBeeAPI;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;

import javax.annotation.Nullable;

public final class ClassSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return Class.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagString(((Class) object).getName());
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        try {
            return SillyBeeAPI.serialization.getRemappedClass(((NBTTagString) tag).getString());
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
