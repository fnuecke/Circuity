package li.cil.circuity.common.capabilities.fdd;

import li.cil.circuity.api.item.FloppyDisk;
import li.cil.circuity.common.capabilities.storage.StorageMediumStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class CapabilityFloppyDisk {
    @CapabilityInject(FloppyDisk.class)
    public static Capability<FloppyDisk> FLOPPY_DISK_CAPABILITY;

    // --------------------------------------------------------------------- //

    public static void register() {
        CapabilityManager.INSTANCE.register(FloppyDisk.class, new StorageMediumStorage<>(), FloppyDiskImpl::new);
    }

    // --------------------------------------------------------------------- //

    private CapabilityFloppyDisk() {
    }
}
