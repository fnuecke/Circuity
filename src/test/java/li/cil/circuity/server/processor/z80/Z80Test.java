package li.cil.circuity.server.processor.z80;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import org.junit.Test;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class Z80Test {
    @Test
    public void runDiag() throws Exception {
        final BusControllerWithMemory controller = new BusControllerWithMemory();

        final Z80 cpu = new Z80();
        cpu.setBusController(controller);

        cpu.reset();

        final int diagnosticsOffset = 0x100;
        final byte[] diagnosticsRom = Files.readAllBytes(Paths.get("src/test/resources/cpudiag.bin"));

        cpu.setPC(diagnosticsOffset);
        for (int address = diagnosticsOffset, end = Math.min(0xFFFF, diagnosticsOffset + diagnosticsRom.length); address < end; ++address) {
            controller.mapAndWrite(address, diagnosticsRom[address - diagnosticsOffset] & 0xFF);
        }

        final int biosOffset = 0x00;
        final byte[] biosRom = {
                0x76,             // HLT       @ 0x00
                0x00,             // NOP       @ 0x01
                0x00,             // NOP       @ 0x02
                0x00,             // NOP       @ 0x03
                0x00,             // NOP       @ 0x04
                0x3E, 0x02,       // LD A,2    @ 0x05
         (byte) 0xB9,             // CMP C     @ 0x07
         (byte) 0xCA, 0x12, 0x00, // JZ chout  @ 0x08
                0x3E, 0x09,       // LD A,9    @ 0x0B
         (byte) 0xB9,             // CMP C     @ 0x0D
         (byte) 0xCA, 0x16, 0x00, // JZ sout   @ 0x0E
         (byte) 0xC9,             // RET       @ 0x11
                0x7B,             // LD A,E    @ 0x12 ; chout
         (byte) 0xD3, 0x03,       // OUT A,(3) @ 0x13
         (byte) 0xC9,             // RET       @ 0x15
                0x06, 0x24,       // LD B,'$'  @ 0x16 ; sout
                0x1A,             // LD A,(DE) @ 0x18 ; loop
         (byte) 0xB8,             // CMP B     @ 0x19
         (byte) 0xC8,             // RZ        @ 0x1A
         (byte) 0xD3, 0x03,       // OUT A,(3) @ 0x1B
                0x13,             // INC DE    @ 0x1D
         (byte) 0xC3, 0x18, 0x00, // JP loop   @ 0x1E
         (byte) 0xC9              // RET       @ 0x21
        };
        for (int address = biosOffset, end = Math.min(0xFFFF, biosOffset + biosRom.length); address < end; ++address) {
            controller.mapAndWrite(address, biosRom[address - biosOffset] & 0xFF);
        }

        final int runFor = 10;
        final int ips = 640; // 2mhz
        final int tps = 20;
        for (int tick = 0, end = runFor * tps; tick < end; ++tick) {
            cpu.run(ips / tps);
        }

        assertTrue(controller.serialConsole.toString().trim().equals("CPU IS OPERATIONAL"));
    }

    private class BusControllerWithMemory implements BusController {
        final byte[] memory = new byte[0x10000];
        final StringBuilder serialConsole = new StringBuilder();

        @Override
        public void mapAndWrite(final int address, final int value) throws IndexOutOfBoundsException {
            if (address == 0x10003) {
                serialConsole.append((char) value);
            } else {
                memory[address] = (byte) value;
            }
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