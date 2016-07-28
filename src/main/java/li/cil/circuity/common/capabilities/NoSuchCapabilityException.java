package li.cil.circuity.common.capabilities;

public final class NoSuchCapabilityException extends IllegalArgumentException {
    public NoSuchCapabilityException() {
        super("No such capability. Use hasCapability to check before calling hasCapability.");
    }
}
