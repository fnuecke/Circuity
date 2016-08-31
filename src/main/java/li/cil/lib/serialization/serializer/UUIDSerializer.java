package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.UUID;

public final class UUIDSerializer implements Serializer {
    public static final String MOST_SIGNIFICANT_BITS_TAG = "mostSigBits";
    public static final String LEAST_SIGNIFICANT_BITS_TAG = "leastSigBits";

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == UUID.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        final UUID uuid = (UUID) object;
        final NBTTagCompound uuidInfo = new NBTTagCompound();
        uuidInfo.setLong(MOST_SIGNIFICANT_BITS_TAG, uuid.getMostSignificantBits());
        uuidInfo.setLong(LEAST_SIGNIFICANT_BITS_TAG, uuid.getLeastSignificantBits());
        return uuidInfo;
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final NBTTagCompound uuidInfo = (NBTTagCompound) tag;
        return new UUID(uuidInfo.getLong(MOST_SIGNIFICANT_BITS_TAG), uuidInfo.getLong(LEAST_SIGNIFICANT_BITS_TAG));
    }

    @Nullable
    @Override
    public Object getDefault() {
        return 0L;
    }
}
