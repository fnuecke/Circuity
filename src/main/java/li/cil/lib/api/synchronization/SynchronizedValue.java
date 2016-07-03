package li.cil.lib.api.synchronization;

import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base contract for synchronized values.
 * <p>
 * Allows more control over how fields of components get synchronized. For most
 * use cases the default implementation should be sufficient, but sometimes you
 * may need just a bit more flexibility.
 * <p>
 * When the object needs synchronization to the client due to an internal state
 * change, it should check if a manager is currently set (via {@link #setManager(SynchronizationManagerServer)}),
 * and if so, notify the manager of the change by calling {@link SynchronizationManagerServer#setDirty(SynchronizedValue, Object)},
 * passing itself as the first value, and any additional context information as
 * the second value. An example for such context information would be the index
 * in an array that changed. The gathered list of such context information is
 * passed into {@link #serialize(PacketBuffer, List)} when the value is next
 * synchronized to the client. When no manager is currently set, no client is
 * tracking the entity the component belongs to, so no synchronization is required.
 *
 * @see li.cil.lib.synchronization.value.SynchronizedByte
 * @see li.cil.lib.synchronization.value.SynchronizedShort
 * @see li.cil.lib.synchronization.value.SynchronizedInt
 * @see li.cil.lib.synchronization.value.SynchronizedLong
 * @see li.cil.lib.synchronization.value.SynchronizedChar
 * @see li.cil.lib.synchronization.value.SynchronizedFloat
 * @see li.cil.lib.synchronization.value.SynchronizedDouble
 * @see li.cil.lib.synchronization.value.SynchronizedString
 * @see li.cil.lib.synchronization.value.SynchronizedObject
 * @see li.cil.lib.synchronization.value.SynchronizedArray
 * @see li.cil.lib.synchronization.value.SynchronizedByteArray
 * @see li.cil.lib.synchronization.value.SynchronizedIntArray
 */
public interface SynchronizedValue {
    /**
     * Called to register or unregister a synchronization manager.
     * <p>
     * This is called when the component declaring the field holding this
     * instance is started to be tracked for changes to send to clients.
     * <p>
     * On the client side this will never be set.
     *
     * @param manager the manager now tracking this value.
     */
    void setManager(@Nullable final SynchronizationManagerServer manager);

    // --------------------------------------------------------------------- //

    /**
     * Called when the value gets synchronized to the client.
     * <p>
     * Writes the data that gets sent to the client to the packet buffer. This
     * allows a more light-weight serialization than using the serialization
     * part of the library.
     * <p>
     * The list of context tokens will empty in two cases:
     * <ul>
     * <li>when no context tokens were passed by this value when it called
     * {@link SynchronizationManagerServer#setDirty(SynchronizedValue, Object)}.</li>
     * <li>when the value is initially sent to the client (full sync).</li>
     * </ul>
     *
     * @param packet the packet to write the data to serialize to.
     * @param tokens a list of context tokens collected since the last synchronization.
     */
    void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens);

    /**
     * Called when the client receives new data for this value.
     * <p>
     * The passed packet buffer will contain the data previously written in
     * {@link #serialize(PacketBuffer, List)}.
     *
     * @param packet the serialized data received from the server.
     */
    void deserialize(final PacketBuffer packet);
}
