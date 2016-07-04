package li.cil.lib.serialization.serializer;

import li.cil.lib.util.ReflectionUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

public final class NBTSerializableSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return INBTSerializable.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final INBTSerializable serializable = (INBTSerializable) object;
        return serializable.serializeNBT();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final INBTSerializable serializable;
        if (object != null) {
            serializable = (INBTSerializable) object;
        } else {
            serializable = ReflectionUtil.newInstance(clazz);
        }
        serializable.deserializeNBT(tag);
        return serializable;
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
