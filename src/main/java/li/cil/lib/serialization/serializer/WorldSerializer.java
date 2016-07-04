package li.cil.lib.serialization.serializer;

import li.cil.lib.serialization.SerializerCollectionImpl;
import li.cil.lib.util.WorldUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class WorldSerializer implements Serializer {
    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public WorldSerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return World.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final World world = (World) object;
        final int dimension = world.provider.getDimension();
        return new NBTTagInt(dimension);
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final int dimension = ((NBTTagInt) tag).getInt();
        return WorldUtil.getWorld(dimension, serialization.isClientSide());
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }
}
