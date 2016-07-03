package li.cil.lib.api.serialization;

import li.cil.lib.serialization.SerializationException;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;

/**
 * Provides serialization facilities for supported types.
 * <p>
 * Supported types include Java primitives, arrays, collections, strings, class
 * types, as well as an assortment of Minecraft specific types.
 * <p>
 * Note that some types, such as {@link net.minecraft.world.World} and
 * {@link net.minecraft.tileentity.TileEntity} are serialized in a shallow
 * manner, i.e. only data required to <em>reference</em> them again during
 * deserialization is serialized.
 * <p>
 * There are two ways of deserialization: creating a new instance, and
 * <em>trying</em> to deserialize into an existing object. The emphasis
 * for the latter lies on trying, as this is not always possible, for
 * example when deserializing an array, but the length differs from the
 * deserialized value. As such, this should be considered an option to
 * reduce memory allocation during deserialization, but the returned value
 * should always be used afterwards, instead of the passed in value.
 * <p>
 * The only exception to this rule are {@link li.cil.lib.api.serialization.Serializable}
 * classes, for which, if an existing instance is given, this instance will
 * always be deserialized into and returned.
 */
@SuppressWarnings("unused")
public interface SerializerCollection {
    /**
     * Whether this serializer operates for the client side.
     * <p>
     * This is used by serializers to determine side specific behavior when
     * deserializing values. This includes how world references are resolved
     * during deserialization, for example.
     *
     * @return <code>true</code> if operating for the client side;
     * <code>false</code> when operating for the server side.
     */
    boolean isClientSide();

    /**
     * Provides a serialization of the specified object.
     *
     * @param object the object to serialize.
     * @return the tag representing the serialized object.
     */
    NBTBase serialize(final Object object);

    @Nullable
    <T> T deserialize(@Nullable final Object object, final Class<T> clazz, final NBTBase tag) throws SerializationException;

    @Nullable
    <T> T deserialize(final T object, final NBTBase tag) throws SerializationException;

    @Nullable
    <T> T deserialize(final Class<T> clazz, final NBTBase tag) throws SerializationException;

    /**
     * Get the default value for the specified type.
     * <p>
     * For example, for numeric primitives this will be <code>0</code>, for
     * object types it will be <code>null</code>.
     *
     * @param clazz the class type to get the default value for.
     * @param <T>   the type of the returned value.
     * @return the default value for the specified type.
     */
    @Nullable
    <T> T getDefault(final Class<T> clazz);
}
