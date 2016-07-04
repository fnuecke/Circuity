package li.cil.lib.serialization.serializer;

import li.cil.lib.api.serialization.SerializerCollection;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArraySerializer implements Serializer {
    private final SerializerCollection serialization;

    // --------------------------------------------------------------------- //

    public ArraySerializer(final SerializerCollection serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz.isArray();
    }

    @Override
    public NBTBase serialize(final Object object) {
        return serialization.serialize(Arrays.asList((Object[]) object));
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final Object[] array = (Object[]) object;
        final List list = serialization.deserialize(ArrayList.class, tag);
        if (list == null) {
            return null;
        }
        if (array != null && array.length == list.size()) {
            return list.toArray(array);
        } else {
            return list.toArray((Object[]) Array.newInstance(clazz.getComponentType(), list.size()));
        }
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
