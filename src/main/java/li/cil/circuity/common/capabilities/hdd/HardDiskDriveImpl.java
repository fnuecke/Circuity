package li.cil.circuity.common.capabilities.hdd;

import li.cil.circuity.api.item.HardDiskDrive;
import li.cil.circuity.common.capabilities.storage.AbstractStorageMedium;

public final class HardDiskDriveImpl extends AbstractStorageMedium implements HardDiskDrive {
    public static final int CAPACITY = 360 * 1024;

    // --------------------------------------------------------------------- //
    // AbstractStorageMedium

    @Override
    protected int getCapacity() {
        return CAPACITY;
    }

    // --------------------------------------------------------------------- //
    // Disk

    @Override
    public int getTrackCount() {
        return 40;
    }

    @Override
    public int getSectorCount() {
        return 1;
    }
}
