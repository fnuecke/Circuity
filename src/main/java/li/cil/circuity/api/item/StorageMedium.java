package li.cil.circuity.api.item;

import io.netty.buffer.ByteBuf;

/**
 * Capability provided by {@link net.minecraft.item.ItemStack}s.
 * <p>
 * Used to read and/or write data from/to an item that should act as storage
 * medium. This is the base interface for items that act as storage medium.
 * Typically they will use a specialization of this interface with medium-
 * specific information (e.g. number of tracks / sectors).
 */
public interface StorageMedium {
    /**
     * A byte buffer representing the data stored on the storage medium.
     * <p>
     * Note that when interacting with the data, the length/capacity of the
     * buffer <em>should not</em> be manipulated. The provider is expected to
     * enforce its size limits when the buffer is saved/loaded, so data outside
     * the allowed capacity may get truncated.
     * <p>
     * Note that this is only expected to work on the server side. Access on
     * the client side is undefined.
     *
     * @return the data stored on the medium.
     */
    ByteBuf getData();
}
