package li.cil.circuity.common.capabilities.eeprom;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.item.EEPROM;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.storage.ExternalData;

import java.util.UUID;

public final class EEPROMImpl implements EEPROM {
    public static final int CAPACITY = 4 * 1024;

    // --------------------------------------------------------------------- //

    UUID uuid;

    // --------------------------------------------------------------------- //
    // EEPROM

    @Override
    public ByteBuf getData() {
        final ExternalData externalData = SillyBeeAPI.storage.getData(uuid);
        uuid = externalData.getDataId();

        final ByteBuf data = externalData.getData();
        data.capacity(CAPACITY);
        return data;
    }
}
