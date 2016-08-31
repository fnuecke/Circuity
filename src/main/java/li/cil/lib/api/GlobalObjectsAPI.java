package li.cil.lib.api;

import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * This API provides a simple per-world global lookup table in which values may
 * be placed using a globally unique identifier.
 * <p>
 * It is primarily intended to enable referencing values on the client side,
 * where there may be no supporting structures enabling lookup (e.g. a node
 * network of adjacent tile entities). This way, the server can synchronize a
 * unique ID for an object to the client, and the object can register itself
 * in this table to allow itself being registered by whatever other object
 * needs to reference it.
 * <p>
 * A specific example are screens in Circuity referencing bus devices providing
 * rendering callbacks.
 */
public interface GlobalObjectsAPI {
    /**
     * Puts an object into the globals table of the specified world.
     *
     * @param world the world to store the value for.
     * @param key   the key to store the value with.
     * @param value the value to store.
     */
    void put(final World world, final UUID key, final Object value);

    /**
     * Removes an entry from the globals table of the specified world.
     *
     * @param world the world to remove the value from.
     * @param key   the key of the value to remove.
     */
    void remove(final World world, final UUID key);

    /**
     * Retrieve an object with the specified key from the globals table of the
     * specified world.
     *
     * @param world the world to retrieve the value from.
     * @param key   the key of the value to retrieve.
     * @return the value; <code>null</code> if no such value is stored.
     */
    @Nullable
    Object get(final World world, final UUID key);
}
