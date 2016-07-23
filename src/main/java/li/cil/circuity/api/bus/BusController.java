package li.cil.circuity.api.bus;

public interface BusController {
    void scheduleScan();

    void mapAndWrite(final int address, final int value);

    int mapAndRead(final int address);
}
