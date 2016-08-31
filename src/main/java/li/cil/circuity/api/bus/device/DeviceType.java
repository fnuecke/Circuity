package li.cil.circuity.api.bus.device;

public enum DeviceType {
    UNKNOWN,
    BUS_CONTROLLER,
    READ_WRITE_MEMORY,
    READ_ONLY_MEMORY,
    FLOPPY_DISK_DRIVE,
    HARD_DISK_DRIVE,
    SERIAL_INTERFACE,
    REDSTONE_CONTROLLER,
    SCREEN;

    public final int id;

    DeviceType() {
        this.id = ordinal();
    }
}
