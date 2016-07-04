package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;

public interface Serializer {
    boolean canSerialize(Class<?> clazz);

    NBTBase serialize(Object object);

    @Nullable
    Object deserialize(@Nullable Object object, Class<?> clazz, NBTBase tag);

    @Nullable
    Object getDefault();
}
