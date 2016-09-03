package li.cil.lib.serialization;

import li.cil.lib.api.serialization.SerializerCollection;
import li.cil.lib.serialization.serializer.ArraySerializer;
import li.cil.lib.serialization.serializer.BlockPosSerializer;
import li.cil.lib.serialization.serializer.BooleanSerializer;
import li.cil.lib.serialization.serializer.ByteArraySerializer;
import li.cil.lib.serialization.serializer.ByteSerializer;
import li.cil.lib.serialization.serializer.CharSerializer;
import li.cil.lib.serialization.serializer.ClassSerializer;
import li.cil.lib.serialization.serializer.CollectionSerializer;
import li.cil.lib.serialization.serializer.DoubleSerializer;
import li.cil.lib.serialization.serializer.EntitySerializer;
import li.cil.lib.serialization.serializer.EnumSerializer;
import li.cil.lib.serialization.serializer.FloatSerializer;
import li.cil.lib.serialization.serializer.FutureSerializer;
import li.cil.lib.serialization.serializer.IntArraySerializer;
import li.cil.lib.serialization.serializer.IntSerializer;
import li.cil.lib.serialization.serializer.LongSerializer;
import li.cil.lib.serialization.serializer.MapSerializer;
import li.cil.lib.serialization.serializer.NBTSerializableSerializer;
import li.cil.lib.serialization.serializer.SerializableSerializer;
import li.cil.lib.serialization.serializer.Serializer;
import li.cil.lib.serialization.serializer.ShortSerializer;
import li.cil.lib.serialization.serializer.StringSerializer;
import li.cil.lib.serialization.serializer.TileEntitySerializer;
import li.cil.lib.serialization.serializer.UUIDSerializer;
import li.cil.lib.serialization.serializer.WorldSerializer;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default internal implementation with all known serializers.
 */
public final class SerializerCollectionImpl implements SerializerCollection {
    private final boolean isClientSide;
    private final List<Serializer> serializerList = new ArrayList<>();
    private final Map<Class<?>, Serializer> serializerLookup = new HashMap<>();

    // --------------------------------------------------------------------- //

    public SerializerCollectionImpl(final boolean isClientSide) {
        this.isClientSide = isClientSide;

        // Generic types declaring fields annotated with Serialize. Try this
        // first, to allow overriding default serialization behavior.
        serializerList.add(new SerializableSerializer(this));

        // Primitive types.
        serializerList.add(new ByteSerializer());
        serializerList.add(new ShortSerializer());
        serializerList.add(new IntSerializer());
        serializerList.add(new LongSerializer());
        serializerList.add(new FloatSerializer());
        serializerList.add(new DoubleSerializer());
        serializerList.add(new BooleanSerializer());
        serializerList.add(new CharSerializer());

        // Java type serializers.
        serializerList.add(new ByteArraySerializer());
        serializerList.add(new IntArraySerializer());
        serializerList.add(new StringSerializer());
        serializerList.add(new EnumSerializer());
        serializerList.add(new ClassSerializer());
        serializerList.add(new FutureSerializer(this));
        serializerList.add(new UUIDSerializer());

        // Array and collection serializers.
        serializerList.add(new ArraySerializer(this));
        serializerList.add(new CollectionSerializer(this));
        serializerList.add(new MapSerializer(this));

        // Minecraft type serializers.
        serializerList.add(new BlockPosSerializer());
        serializerList.add(new EntitySerializer(this));
        serializerList.add(new TileEntitySerializer(this));
        serializerList.add(new WorldSerializer(this));
        serializerList.add(new NBTSerializableSerializer());
    }

    // --------------------------------------------------------------------- //
    // SerializerCollection

    @Override
    public boolean isClientSide() {
        return isClientSide;
    }

    @Override
    public NBTBase serialize(final Object object) {
        synchronized (serializerList) {
            return getSerializer(object.getClass()).serialize(object);
        }
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(@Nullable final Object object, final Class<T> clazz, final NBTBase tag) {
        synchronized (serializerList) {
            return (T) getSerializer(clazz).deserialize(object, clazz, tag);
        }
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(final T object, final NBTBase tag) throws SerializationException {
        return deserialize(object, (Class<T>) object.getClass(), tag);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(final Class<T> clazz, final NBTBase tag) {
        return (T) getSerializer(clazz).deserialize(null, clazz, tag);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getDefault(final Class<T> clazz) {
        return (T) getSerializer(clazz).getDefault();
    }

    // --------------------------------------------------------------------- //

    private Serializer getSerializer(final Class<?> clazz) {
        return serializerLookup.computeIfAbsent(clazz, this::findSerializer);
    }

    private Serializer findSerializer(final Class<?> clazz) {
        for (final Serializer serializer : serializerList) {
            if (serializer.canSerialize(clazz)) {
                return serializer;
            }
        }
        throw new SerializationException("Unsupported type: " + clazz);
    }
}
