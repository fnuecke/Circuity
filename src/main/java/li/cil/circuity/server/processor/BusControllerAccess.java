package li.cil.circuity.server.processor;

import li.cil.circuity.api.bus.BusController;

import java.util.function.Supplier;

public class BusControllerAccess implements BusAccess {
    private final Supplier<BusController> controller;
    private final int offset;

    public BusControllerAccess(final Supplier<BusController> controller, final int offset) {
        this.controller = controller;
        this.offset = offset;
    }

    public BusControllerAccess(final BusController controller, final int offset) {
        this(() -> controller, offset);
    }

    @Override
    public int read(final int address) {
        return controller.get().mapAndRead(address + offset);
    }

    @Override
    public void write(final int address, final int value) {
        controller.get().mapAndWrite(address + offset, value);
    }
}
