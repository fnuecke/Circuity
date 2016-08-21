package li.cil.circuity.common.capabilities.hdd;

import li.cil.circuity.api.item.HardDiskDrive;
import li.cil.circuity.common.capabilities.storage.StorageMediumStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class CapabilityHardDiskDrive {
    @CapabilityInject(HardDiskDrive.class)
    public static Capability<HardDiskDrive> HARD_DISK_DRIVE_CAPABILITY;

    // --------------------------------------------------------------------- //

    public static void register() {
        CapabilityManager.INSTANCE.register(HardDiskDrive.class, new StorageMediumStorage<>(), HardDiskDriveImpl::new);
    }

    // --------------------------------------------------------------------- //

    private CapabilityHardDiskDrive() {
    }
}
