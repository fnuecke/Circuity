package li.cil.lib.serialization.serializer;

import li.cil.lib.serialization.SerializerCollectionImpl;
import li.cil.lib.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

public final class EntitySerializer implements Serializer {
    private static final String DIMENSION_TAG = "dimension";
    private static final String ENTITY_UUID_MOST_TAG = "mostSignificant";
    private static final String ENTITY_UUID_LEAST_TAG = "leastSignificant";

    // --------------------------------------------------------------------- //

    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public EntitySerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return Entity.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final Entity entity = (Entity) object;
        final World world = entity.getEntityWorld();

        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(DIMENSION_TAG, world.provider.getDimension());
        tag.setLong(ENTITY_UUID_MOST_TAG, entity.getPersistentID().getMostSignificantBits());
        tag.setLong(ENTITY_UUID_LEAST_TAG, entity.getPersistentID().getLeastSignificantBits());

        return tag;
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final NBTTagCompound entityInfo = (NBTTagCompound) tag;
        final int dimension = entityInfo.getInteger(DIMENSION_TAG);
        final World world = WorldUtil.getWorld(dimension, serialization.isClientSide());
        if (world != null) {
            final UUID uuid = new UUID(entityInfo.getLong(ENTITY_UUID_MOST_TAG), entityInfo.getLong(ENTITY_UUID_LEAST_TAG));
            for (final Entity entity : world.getLoadedEntityList()) {
                if (entity.getPersistentID().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Object getDefault() {
        return null;
    }
}
