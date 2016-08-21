package li.cil.circuity.common.capabilities.eeprom;

import li.cil.circuity.api.item.EEPROM;
import li.cil.circuity.common.capabilities.storage.StorageMediumStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class CapabilityEEPROM {
    @CapabilityInject(EEPROM.class)
    public static Capability<EEPROM> EEPROM_CAPABILITY;

    // --------------------------------------------------------------------- //

    public static void register() {
        CapabilityManager.INSTANCE.register(EEPROM.class, new StorageMediumStorage<>(), EEPROMImpl::new);
    }

    // --------------------------------------------------------------------- //

    private CapabilityEEPROM() {
    }
}
