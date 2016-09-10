package li.cil.circuity.server.processor.z80;

import com.google.common.base.Throwables;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.common.ecs.component.AbstractComponentBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class TestHooks extends AbstractComponentBusDevice {
    public final TestHooksImpl device = new TestHooksImpl();

    public TestHooks(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public BusDevice getDevice() {
        return device;
    }

    public static final class TestHooksImpl extends AbstractBusDevice implements Addressable, BusStateListener {
        private final StringBuilder serialConsole = new StringBuilder();
        private Z80 cpu;

        public void setCpu(final Z80 cpu) {
            this.cpu = cpu;
        }

        public String getOutput() {
            return serialConsole.toString();
        }

        @Override
        public AddressBlock getPreferredAddressBlock(final AddressBlock memory) {
            return memory.take(0x10030, 1);
        }

        @Override
        public int read(final int address) {
            if (cpu.C() == 2) {
                serialConsole.append((char) cpu.E());
                System.out.print((char) cpu.E());
            } else if (cpu.C() == 9) {
                final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
                for (int i = cpu.DE() & 0xFFFF; mapper.mapAndRead(i) != '$'; ++i) {
                    serialConsole.append((char) mapper.mapAndRead(i));
                    System.out.print((char) mapper.mapAndRead(i));
                }
            }
            System.out.flush();
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
        }

        @Override
        public void handleBusOnline() {
            final int diagnosticsOffset = 0x100;
            final byte[] diagnosticsRom;
            try {
                diagnosticsRom = Files.readAllBytes(Paths.get("src/test/resources/zexdoc.com"));

                final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
                for (int address = diagnosticsOffset, end = Math.min(0xFFFF, diagnosticsOffset + diagnosticsRom.length); address < end; ++address) {
                    mapper.mapAndWrite(address, diagnosticsRom[address - diagnosticsOffset] & 0xFF);
                }

                final int biosOffset = 0x00;
                final byte[] biosRom = {
                        (byte) 0x76, /* HLT */
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0xDB, /* IN A, N */
                        (byte) 0x30,
                        (byte) 0xC9  /* RET */
                };
                for (int address = biosOffset, end = Math.min(0xFFFF, biosOffset + biosRom.length); address < end; ++address) {
                    mapper.mapAndWrite(address, biosRom[address - biosOffset] & 0xFF);
                }
            } catch (final IOException t) {
                Throwables.propagate(t);
            }
        }

        @Override
        public void handleBusOffline() {
        }
    }
}
