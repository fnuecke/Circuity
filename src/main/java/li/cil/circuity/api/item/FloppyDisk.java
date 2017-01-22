package li.cil.circuity.api.item;

public interface FloppyDisk extends StorageMedium {
    int getSideCount();

    int getTrackCount();

    int getSectorCount();

    int getSectorSize();
}
