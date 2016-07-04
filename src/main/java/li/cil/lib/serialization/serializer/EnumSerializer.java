package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;

import javax.annotation.Nullable;

public final class EnumSerializer implements Serializer {
    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz.isEnum();
    }

    @Override
    public NBTBase serialize(final Object object) {
        return new NBTTagByte((byte) ((Enum) object).ordinal());
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        return clazz.getEnumConstants()[(int) ((NBTTagByte) tag).getByte()];
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
