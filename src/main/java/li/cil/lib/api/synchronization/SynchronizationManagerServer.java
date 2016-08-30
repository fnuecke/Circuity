package li.cil.lib.api.synchronization;

import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Server specific synchronization logic.
 */
public interface SynchronizationManagerServer extends SynchronizationManager {
    /**
     * Mark a serialized value as dirty, so that it gets synchronized to any
     * subscribed clients.
     * <p>
     * Any tokens passed to this method will be stored in a list that is passed
     * to {@link SynchronizedValue#serialize(PacketBuffer, List)} when it is
     * time to send the the changed data to the client. These may be used by
     * more complex synchronized values to specify which part of the value has
     * changed, so only that part need to be serialized and sent to the client.
     * A simple example are indices for list like synchronized values.
     * <p>
     * If a value is marked dirty multiple times before the next actual
     * synchronization occurs, every non-null token will be added to the list.
     * It is the responsibility of the serialized value to discard redundant
     * information and only serialize what is truly necessary. This also means
     * that for null tokens, this is a non-issue, as no list is built, i.e.
     * even for multiple calls to this, the value will only be asked once to
     * serialize itself.
     * <p>
     * Note that the serialize method of the synchronized value may not be
     * called at all due to this, if there are no clients currently subscribed
     * to value changes from the specified synchronized value.
     *
     * @param value the value to mark dirty.
     * @param token an optional token identify what part of the value changed.
     */
    void setDirty(final SynchronizedValue value, @Nullable final Object token);

    /**
     * Mark a serialized value as dirty, so that it gets synchronized to any
     * subscribed clients.
     * <p>
     * This method allows more in-depth control over the token list that will
     * be passed to {@link SynchronizedValue#serialize(PacketBuffer, List)}
     * when it is time to send the changed data to the client.
     * <p>
     * Note that the serialize method of the synchronized value may not be
     * called at all due to this, if there are no clients currently subscribed
     * to value changes from the specified synchronized value.
     *
     * @param value        the value to mark dirty.
     * @param tokenUpdater a callback used to update the value's token list.
     */
    // IMPORTANT: Must have a different name, as Object vs. Consumer causes
    //            can cause unexpected behavior when passing null.
    void setDirtyAdvanced(final SynchronizedValue value, final Consumer<List<Object>> tokenUpdater);

    // --------------------------------------------------------------------- //

    /**
     * Get the type ID for the specified type.
     * <p>
     * Type IDs are a more bandwidth friendly way of synchronizing types to the
     * client. The synchronization library keeps a list of type to ID mappings
     * which is automatically kept up-to-date on all connected clients. This
     * way it is not necessary to serialize a full class name, for example.
     * Only the type ID needs to be serialized to the synchronization packet.
     * The client can then resolve the type ID via {@link SynchronizationManagerClient#getTypeByTypeId(int)}.
     * <p>
     * Note that these IDs are <em>not stable across multiple runs</em>. That
     * is when the server gets restarted, the type ID mappings may change, as
     * the IDs are based on the registration order. As such, they should be
     * used exclusively for synchronization, never for persistent serialization.
     * <p>
     * Registration of new types happens automatically when this method is
     * called with a not yet mapped type.
     *
     * @param type the type to get the type ID for.
     * @return the type ID of the type.
     */
    int getTypeIdByType(@Nullable final Class type);

    /**
     * Get the type ID for the specified object.
     * <p>
     * This is a convenience method that delegates to {@link #getTypeIdByType(Class)},
     * taking care of <code>null</code> inputs automatically.
     *
     * @param object the object to get the type ID for.
     * @return the type ID of the type of the object.
     */
    int getTypeIdByValue(@Nullable final Object object);
}
