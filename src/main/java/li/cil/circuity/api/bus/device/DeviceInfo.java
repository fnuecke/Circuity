package li.cil.circuity.api.bus.device;

public final class DeviceInfo {
    public final DeviceType type;
    public final String name;

    public DeviceInfo(final DeviceType type, final String name) {
        this.type = type;
        this.name = name;
    }
}
