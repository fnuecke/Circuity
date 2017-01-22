package li.cil.circuity.common.capabilities.fdd;

import li.cil.circuity.api.item.FloppyDisk;
import li.cil.circuity.common.capabilities.storage.AbstractStorageMedium;

public final class FloppyDiskImpl extends AbstractStorageMedium implements FloppyDisk {
    public static final int SIDES = 1;
    public static final int TRACKS = 77;
    public static final int SECTORS = 26;
    public static final int SECTOR_SIZE = 128;
    public static final int CAPACITY = SIDES * TRACKS * SECTORS * SECTOR_SIZE;

    // --------------------------------------------------------------------- //
    // AbstractStorageMedium

    @Override
    protected int getCapacity() {
        return CAPACITY;
    }

    // --------------------------------------------------------------------- //
    // Disk

    @Override
    public int getSideCount() {
        return SIDES;
    }

    @Override
    public int getTrackCount() {
        return TRACKS;
    }

    @Override
    public int getSectorCount() {
        return SECTORS;
    }

    @Override
    public int getSectorSize() {
        return SECTOR_SIZE;
    }
}
