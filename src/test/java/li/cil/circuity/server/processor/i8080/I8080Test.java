package li.cil.circuity.server.processor.i8080;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import org.junit.Test;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class I8080Test {
    @Test
    public void runDiag() throws Exception {
        final BusController controller = new BusControllerWithMemory();

        final I8080ForDiagnostics cpu = new I8080ForDiagnostics();
        cpu.setBusController(controller);

        cpu.reset();
        final byte[] data = Files.readAllBytes(Paths.get("src/test/resources/cpudiag.bin"));
        cpu.load(data, 0x100);

        final int runFor = 10;
        final int ips = 640; // 2mhz
        final int tps = 20;
        for (int tick = 0, end = runFor * tps; tick < end; ++tick) {
            cpu.run(ips / tps);
        }

        assertTrue(cpu.success);
    }

    private static class I8080ForDiagnostics extends I8080 {
        public boolean success;

        @Override
        void call(final boolean doCall) {
            super.call(doCall);

            // Hook in to handle cpudiag print calls.
            if (PC == 5) {
                if (C == 9) {
                    int offset = DE() + 3; // Skip prefix bytes
                    final StringBuilder sb = new StringBuilder();
                    while ((char) peek8(offset) != '$') {
                        sb.append((char) peek8(offset));
                        offset += 1;
                    }
                    success = sb.toString().trim().equals("CPU IS OPERATIONAL");
                }
                ret(true);
            }
        }
    }

    private class BusControllerWithMemory implements BusController {
        final byte[] memory = new byte[0x10000];

        @Override
        public void mapAndWrite(final int address, final int value) throws IndexOutOfBoundsException {
            memory[address] = (byte) value;
        }

        @Override
        public int mapAndRead(final int address) throws IndexOutOfBoundsException {
            return memory[address] & 0xFF;
        }

        @Override
        public void scheduleScan() {
        }

        @Override
        public Iterable<BusDevice> getDevices() {
            return Collections.emptyList();
        }

        @Override
        public void setBusController(@Nullable final BusController controller) {
        }
    }
}