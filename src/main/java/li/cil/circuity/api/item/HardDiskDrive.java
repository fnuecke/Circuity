package li.cil.circuity.api.item;

public interface HardDiskDrive extends StorageMedium {
    int getTrackCount();

    int getSectorCount();
}
