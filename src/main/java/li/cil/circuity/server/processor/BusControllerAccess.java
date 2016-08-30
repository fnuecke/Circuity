package li.cil.circuity.server.processor;

import li.cil.circuity.api.bus.BusController;

import java.util.function.Supplier;

public class BusControllerAccess implements BusAccess {
    private final Supplier<BusController> controller;
    private final int offset;
    private final int addressMask;

    public BusControllerAccess(final Supplier<BusController> controller, final int offset, final int addressMask) {
        this.controller = controller;
        this.offset = offset;
        this.addressMask = addressMask;
    }

    public BusControllerAccess(final Supplier<BusController> controller, final int offset) {
        this(controller, offset, 0xFFFFFFFF);
    }

    @Override
    public int read(final int address) {
        return controller.get().mapAndRead((address & addressMask) + offset);
    }

    @Override
    public void write(final int address, final int value) {
        controller.get().mapAndWrite((address & addressMask) + offset, value);
    }
}
