package li.cil.circuity.api.item;

/**
 * Used to read and/or write data from/to an item that should act as an EEPROM.
 * Such items are usually used to contain code used for initializing the boot
 * sequence of a computer.
 * <p>
 * Specifically, this interface is used by the EEPROM reader to filter which
 * items may be placed into it, and then interact with them, for example.
 */
public interface EEPROM extends StorageMedium {
}
