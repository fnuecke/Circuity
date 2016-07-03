package li.cil.lib.api;

import li.cil.lib.api.serialization.SerializerCollection;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Access to serializer collections for server and client side.
 */
public interface SerializationAPI {
    /**
     * Get the serializer collection for the specified side.
     *
     * @param side the side to get the serializer collection for.
     * @return the serializer collection for the specified side.
     */
    SerializerCollection get(final Side side);

    /**
     * Utility method for obtaining the serializer collection based on a world
     * object, which will be used to determine the side.
     *
     * @param world the world based on which to get the serializer collection.
     * @return the serializer collection for the side the world lives on.
     */
    SerializerCollection get(final World world);

    /**
     * Utility method for obtaining the serializer collection based on a
     * boolean flag indicating sidedness.
     *
     * @param isRemote whether to retrieve the client side serialization collection.
     * @return the serializer collection for the client side if <code>isRemote</code>
     * is <code>true</code>, the one for the server side otherwise.
     */
    SerializerCollection get(final boolean isRemote);

    // --------------------------------------------------------------------- //

    // TODO More advanced remapping to support structural changes in serialized objects.

    void addClassRemapping(final String className, final Class<?> remappedClass);

    Class<?> getRemappedClass(final String className) throws ClassNotFoundException;
}
