package li.cil.circuity.common.capabilities.eeprom;

import li.cil.circuity.api.item.EEPROM;
import li.cil.circuity.common.capabilities.storage.AbstractStorageMedium;

public final class EEPROMImpl extends AbstractStorageMedium implements EEPROM {
    public static final int CAPACITY = 4 * 1024;

    // --------------------------------------------------------------------- //
    // AbstractStorageMedium

    @Override
    protected int getCapacity() {
        return CAPACITY;
    }
}
