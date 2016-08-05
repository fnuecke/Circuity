package li.cil.circuity.api.item;

import io.netty.buffer.ByteBuf;

/**
 * Capability provided by {@link net.minecraft.item.ItemStack}s.
 * <p>
 * Used to read and/or write data from/to an item that should act as an EEPROM.
 * Such items are usually used to contain code used for initializing the boot
 * sequence of a computer.
 * <p>
 * Specifically, this interface is used by the EEPROM reader to filter which
 * items may be placed into it, and then interact with them, for example.
 */
public interface EEPROM {
    /**
     * A byte buffer representing the data stored on the EEPROM.
     * <p>
     * Note that when interacting with the data, the length/capacity of the
     * buffer <em>should not</em> be manipulated. The EEPROM is expected to
     * enforce its size limits when the buffer is saved/loaded, so data outside
     * the allowed capacity may get truncated.
     * <p>
     * Note that this is only expected to work on the server side. Access on
     * the client side is undefined.
     *
     * @return the data stored on the EEPROM.
     */
    ByteBuf getData();
}
