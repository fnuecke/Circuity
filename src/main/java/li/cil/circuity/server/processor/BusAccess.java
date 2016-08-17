package li.cil.circuity.server.processor;

/**
 * Generic bus access abstraction layer for devices.
 * <p>
 * Used to separate specific {@link li.cil.circuity.api.bus.BusDevice} logic
 * from actual device implementations.
 */
public interface BusAccess {
    int read(final int address);

    void write(final int address, final int value);
}
