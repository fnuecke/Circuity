package li.cil.circuity.server.processor.i8080;

import com.google.common.base.Splitter;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// 2MHz CPU: 10msec == 20'000 cycles
//  400 ~ 1.25MHz
//  640 ~ 2.00MHz
// 1280 ~ 4.00MHz
// 2560 ~ 8.00MHz

@Serializable
public class I8080 extends AbstractBusDevice implements InterruptSink {
    public static final int CYCLES_1MHZ = 320;

    private static final int FLAG_MASK_BASE = 1 << 1;
    private static final int FLAG_MASK_Z = 1 << 6;
    private static final int FLAG_MASK_S = 1 << 7;
    private static final int FLAG_MASK_P = 1 << 2;
    private static final int FLAG_MASK_CY = 1 << 0;
    private static final int FLAG_MASK_AC = 1 << 4;

    private static final Pattern INTEL_HEX_PATTERN = Pattern.compile(":(.{2})(.{4})(.{2})(.*)(.{2})");

    // --------------------------------------------------------------------- //

    final Object interruptLock = new Object();

    /**
     * Address of the device handling I/O instructions.
     * <p>
     * This is expected to be a memory-mapped device handling the serial data
     * sent to it via IN and OUT instructions of the CPU.
     */
    @Serialize
    int memoryMappedIO = -1;

    // Registers

    @Serialize
    int PC = 0;
    @Serialize
    int SP = 0x10000;
    @Serialize
    int A, B, C, D, E, H, L = 0;
    @Serialize
    boolean Z = false;
    @Serialize
    boolean S = false;
    @Serialize
    boolean P = false;
    @Serialize
    boolean CY = false;
    @Serialize
    boolean AC = false;

    // Interrupts

    @Serialize
    boolean irqEnabled = false;

    @Serialize
    boolean irqRequested = false;

    @Serialize
    int irqOpcode = 0;

    // Timing / Execution

    @Serialize
    int cycles = 0;

    @Serialize
    long totalCycles = 0L;

    @Serialize
    boolean halted = false;

    // --------------------------------------------------------------------- //
    // InterruptSink

    @Override
    public int[] getAcceptedInterrupts(final int[] interrupts) {
        return new int[]{interrupts[0]};
    }

    @Override
    public void setAcceptedInterrupts(@Nullable final int[] interrupts) {
    }

    @Override
    public void interrupt(final int interrupt) {
        synchronized (interruptLock) {
            if (irqEnabled) {
                irqRequested = true;
                // TODO THIS IS BULLSHIT
                // No seriously, it's just for testing. Should replace with
                // providing multiple interrupts, then getting the index of
                // the one that's triggered and providing that.
                irqOpcode = interrupt & 0xFF;
            }
        }
    }

    // --------------------------------------------------------------------- //
    // Object

    @Override
    public String toString() {
        return String.format(" A  B  C  D  E  H  L  Flags\n" +
                "%02X %02X %02X %02X %02X %02X %02X ", A, B, C, D, E, H, L) +
                (Z ? " Z" : "") +
                (S ? " S" : "") +
                (P ? " P" : "") +
                (CY ? " CY" : "") +
                (AC ? " AC" : "");
    }

    // --------------------------------------------------------------------- //

    public void run(final int cycles) {
        if (!halted) {
            this.cycles += cycles;
            this.totalCycles += cycles;
            while (this.cycles > 0) step();
        }
    }

    public void reset() {
        PC = 0;
        SP = 0x10000;
        A = 0;
        B = 0;
        C = 0;
        D = 0;
        E = 0;
        H = 0;
        L = 0;
        Z = false;
        S = false;
        P = false;
        CY = false;
        AC = false;
        cycles = 0;
        totalCycles = 0L;
        irqEnabled = false;
        irqRequested = false;
        irqOpcode = 0;
        halted = false;
    }

    public void load(final byte[] rom, final int offset) {
        PC = offset & 0xFFFF;
        for (int address = offset, end = Math.min(0xFFFF, offset + rom.length); address < end; ++address) {
            controller.mapAndWrite(address, rom[address - offset] & 0xFF);
        }
    }

    // Supports I8HEX (https://en.wikipedia.org/wiki/Intel_HEX)
    public void loadIntelHex(final Iterable<String> lines) {
        for (final String line : lines) {
            final Matcher matcher = INTEL_HEX_PATTERN.matcher(line);
            if (matcher.matches()) {
                final int bytes = Integer.parseInt(matcher.group(1), 16);
                final int offset = Integer.parseInt(matcher.group(2), 16);
                final int recordType = Integer.parseInt(matcher.group(3), 16);
                final List<Integer> data = StreamSupport.stream(Splitter.fixedLength(2).split(matcher.group(4)).spliterator(), false).map(b -> Integer.parseInt(b, 16)).collect(Collectors.toList());
                final int checksum = Integer.parseInt(matcher.group(5), 16);

                final int computedChecksum = ~(bytes + offset + recordType + data.stream().reduce(0, (a, b) -> a + b));
                if (computedChecksum != checksum) {
                    throw new IllegalArgumentException(String.format("Checksum failed for line '%s'.", line));
                }
                if (bytes != data.size()) {
                    throw new IllegalArgumentException(String.format("Byte count does not match data size on line '%s'.", line));
                }

                if (recordType == 0) {
                    for (int address = offset, end = offset + bytes; address < end; ++address) {
                        controller.mapAndWrite(address, data.get(address - offset));
                    }
                } else if (recordType == 1) {
                    break; // EOF
                } else {
                    throw new IllegalArgumentException(String.format("Unsupported record type '%d'.", recordType));
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

    // 16-bit register accessors.

    int BC() {
        return (B << 8) | C;
    }

    void setBC(final int value) {
        B = value >>> 8;
        C = value & 0xFF;
    }

    int DE() {
        return (D << 8) | E;
    }

    void setDE(final int value) {
        D = value >>> 8;
        E = value & 0xFF;
    }

    int HL() {
        return (H << 8) | L;
    }

    void setHL(final int value) {
        H = value >>> 8;
        L = value & 0xFF;
    }

    int PSW() {
        int F = FLAG_MASK_BASE;
        if (Z) F |= FLAG_MASK_Z;
        if (S) F |= FLAG_MASK_S;
        if (P) F |= FLAG_MASK_P;
        if (CY) F |= FLAG_MASK_CY;
        if (AC) F |= FLAG_MASK_AC;
        return (A << 8) | F;
    }

    void setPSW(final int value) {
        A = value >>> 8;
        final int F = value & 0xFF;
        Z = (F & FLAG_MASK_Z) != 0;
        S = (F & FLAG_MASK_S) != 0;
        P = (F & FLAG_MASK_P) != 0;
        CY = (F & FLAG_MASK_CY) != 0;
        AC = (F & FLAG_MASK_AC) != 0;
    }

    // Memory

    int peek8(final int address) {
        return controller.mapAndRead(address) & 0xFF;
    }

    void poke8(final int address, final int value) {
        controller.mapAndWrite(address, value);
    }

    int read8() {
        final int result = peek8(PC);
        PC = (PC + 1) & 0xFFFF;
        return result;
    }

    int peek16(final int address) {
        return (peek8(address + 1) << 8) | peek8(address);
    }

    int peekHL() {
        return peek8(HL());
    }

    void poke16(final int address, final int value) {
        poke8(address, value);
        poke8(address + 1, value >>> 8);
    }

    int read16() {
        final int result = peek16(PC);
        PC = (PC + 2) & 0xFFFF;
        return result;
    }

    void pokeHL(final int value) {
        poke8(HL(), value);
    }

    // IO

    int ioRead(final int port) {
        if (memoryMappedIO >= 0) {
            return controller.mapAndRead(memoryMappedIO + port) & 0xFF;
        } else {
            return 0;
        }
    }

    void ioWrite(final int port, final int data) {
        if (memoryMappedIO >= 0) {
            controller.mapAndWrite(memoryMappedIO + port, data);
        }
    }

    // Control

    void push(final int value) {
        SP = (SP - 2) & 0xFFFF;
        poke16(SP, value);
    }

    int pop() {
        final int result = peek16(SP);
        SP = (SP + 2) & 0xFFFF;
        return result;
    }

    void rst(final int address) {
        push(PC);
        PC = address;
    }

    void call(final boolean doCall) {
        if (doCall) {
            cycles -= 7;
            push(PC + 2);
            PC = read16();
        } else {
            PC = (PC + 2) & 0xFFFF;
        }
    }

    void ret(final boolean doRet) {
        if (doRet) {
            cycles -= 6;
            PC = pop();
        }
    }

    void jp(final boolean doJump, final int address) {
        if (doJump) {
            PC = address;
        }
    }

    void jp(final boolean doJump) {
        jp(doJump, read16());
    }

    // ALU

    int inc(final int value) {
        final boolean carry = CY; // Keep carry flag.
        final int result = updateFlags(value + 1, value, 1);
        CY = carry;
        return result;
    }

    int dec(final int value) {
        final boolean carry = CY; // Keep carry flag.
        final int result = updateFlags(value - 1, value, 1);
        CY = carry;
        return result;
    }

    int add2(final int rhs) {
        final int result = HL() + rhs;
        CY = result >= 0x10000 || result < 0;
        return result & 0xFFFF;
    }

    int add(final int rhs) {
        return updateFlags(A + rhs, A, rhs);
    }

    int adc(final int rhs) {
        return add(rhs + (CY ? 1 : 0));
    }

    int sub(final int rhs) {
        return updateFlags(A - rhs, A, rhs);
    }

    int sbc(final int rhs) {
        return sub(rhs + (CY ? 1 : 0));
    }

    int and(final int rhs) {
        final int result = updateFlags(A & rhs, A, rhs);
        CY = false;
        return result;
    }

    int or(final int rhs) {
        final int result = updateFlags(A | rhs, A, rhs);
        CY = false;
        AC = false;
        return result;
    }

    int xor(final int rhs) {
        final int result = updateFlags(A ^ rhs, A, rhs);
        CY = false;
        AC = false;
        return result;
    }

    void cp(final int rhs) {
        final int a = A; // Keep accumulator value.
        updateFlags(A - rhs, A, rhs);
        A = a;
    }

    void rr() {
        final boolean carry = CY;
        CY = (A & 0x01) != 0;
        A = ((A >>> 1) | (carry ? 0x80 : 0x00)) & 0xFF;
    }

    void rrc() {
        CY = (A & 0x01) != 0;
        A = ((A >>> 1) | ((A & 0x01) << 7)) & 0xFF;
    }

    void rl() {
        final boolean carry = CY;
        CY = (A & 0x80) != 0;
        A = ((A << 1) | (carry ? 0x01 : 0x00)) & 0xFF;
    }

    void rlc() {
        CY = (A & 0x80) != 0;
        A = ((A << 1) | ((A & 0x80) >>> 7)) & 0xFF;
    }

    void daa() {
        int a = 0;
        if (AC || (A & 0x0F) > 0x09) {
            a |= 0x06;
            CY = true;
        }
        if (CY || (A >>> 4) > 0x09) {
            a |= 0x60;
        }
        A = add(a);
    }

    // --------------------------------------------------------------------- //

    private void step() {
        processCode();
        processInterrupts();
    }

    private void processCode() {
        final int opcode = read8();
        execute(opcode);
    }

    private void processInterrupts() {
        synchronized (this) {
            if (irqEnabled && irqRequested) {
                irqEnabled = false;
                execute(irqOpcode);
                irqRequested = false;
            }
        }
    }

    private void execute(final int opcode) {
        final I8080Instruction instruction = I8080Instruction.INSTRUCTIONS[opcode];
        if (instruction != null) {
            instruction.operation.accept(this);
            cycles -= instruction.cycles;
        } else {
            System.out.printf("Unknown OpCode %X!\n", opcode);
            cycles -= 4; // NOP
        }
    }

    private int updateFlags(final int value, final int lhs, final int rhs) {
        final int result = value & 0xFF;
        S = (value & 0x80) != 0;
        Z = result == 0;
        P = computeParity(result);
        CY = value >= 0x100 || value < 0;
        AC = (lhs & 0xF) + (rhs & 0xF) > 0xF;
        return result;
    }

    private static boolean computeParity(final int value) {
        int parity = value ^ (value >>> 1);
        parity ^= parity >>> 2;
        parity ^= parity >>> 4;
        return (parity & 1) == 0;
    }
}
