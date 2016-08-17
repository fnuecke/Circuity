package li.cil.circuity.server.processor.z80;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.server.processor.BusControllerAccess;
import org.junit.Test;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class Z80Test {
    @Test
    public void runDiag() throws Exception {
        final BusControllerWithMemory controller = new BusControllerWithMemory();

        final Z80 cpu = new Z80(new BusControllerAccess(controller, 0), new BusControllerAccess(controller, 0x10000));
        controller.setCPU(cpu);

        final int diagnosticsOffset = 0x100;
        final byte[] diagnosticsRom = Files.readAllBytes(Paths.get("src/test/resources/zexdoc.com"));

        for (int address = diagnosticsOffset, end = Math.min(0xFFFF, diagnosticsOffset + diagnosticsRom.length); address < end; ++address) {
            controller.mapAndWrite(address, diagnosticsRom[address - diagnosticsOffset] & 0xFF);
        }

        final int biosOffset = 0x00;
        final byte[] biosRom = {
         (byte) 0x76,       /* HLT */
                0x00,
                0x00,
                0x00,
                0x00,
         (byte) 0xDB,       /* IN A, N */
         (byte) 0x00,
         (byte) 0xC9        /* RET */
        };
        for (int address = biosOffset, end = Math.min(0xFFFF, biosOffset + biosRom.length); address < end; ++address) {
            controller.mapAndWrite(address, biosRom[address - biosOffset] & 0xFF);
        }

        final int mhz = 2_000_000;
        final int tps = 20;
        final int cps = mhz / tps;

        cpu.reset(diagnosticsOffset);
        while (cpu.run(cps)) {
        }

        assertTrue(!controller.serialConsole.toString().trim().contains("ERROR"));
    }

    private class BusControllerWithMemory implements BusController {
        final byte[] memory = new byte[0x10000];
        final StringBuilder serialConsole = new StringBuilder();
        private Z80 cpu;

        @Override
        public void mapAndWrite(final int address, final int value) throws IndexOutOfBoundsException {
            memory[address] = (byte) value;
        }

        @Override
        public int mapAndRead(final int address) throws IndexOutOfBoundsException {
            if (address == 0x10000) {
                if (cpu.C() == 2) {
                    serialConsole.append((char) cpu.E());
                    System.out.print((char) cpu.E());
                } else if (cpu.C() == 9) {
                    for (int i = cpu.DE() & 0xFFFF; memory[i] != '$'; ++i) {
                        serialConsole.append((char) memory[i]);
                        System.out.print((char) memory[i]);
                    }
                }
                System.out.flush();
                return 0;
            } else {
                return memory[address] & 0xFF;
            }
        }

        @Override
        public void scheduleScan() {
        }

        @Override
        public boolean getDevices(final Collection<BusDevice> devices) {
            return true;
        }

        @Override
        public void setBusController(@Nullable final BusController controller) {
        }

        public void setCPU(final Z80 CPU) {
            this.cpu = CPU;
        }
    }
}