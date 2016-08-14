package li.cil.circuity.server.processor.z80;

import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

// 2MHz CPU: 10msec == 20'000 cycles
//  400 ~ 1.25MHz
//  640 ~ 2.00MHz
// 1280 ~ 4.00MHz
// 2560 ~ 8.00MHz

// Operation decoding based on http://www.z80.info/decoding.htm
// Flag computation based on https://github.com/anotherlin/z80emu/

@Serializable
public class Z80 extends AbstractBusDevice implements InterruptSink {
    public static final int CYCLES_1MHZ = 320;

    private static final int FLAG_SHIFT_C = 0;
    private static final int FLAG_SHIFT_N = 1;
    private static final int FLAG_SHIFT_PV = 2;
    private static final int FLAG_SHIFT_H = 4;
    private static final int FLAG_SHIFT_Z = 6;
    private static final int FLAG_SHIFT_S = 7;

    private static final int FLAG_MASK_C = 1 << FLAG_SHIFT_C;
    private static final int FLAG_MASK_N = 1 << FLAG_SHIFT_N;
    private static final int FLAG_MASK_PV = 1 << FLAG_SHIFT_PV;
    private static final int FLAG_MASK_H = 1 << FLAG_SHIFT_H;
    private static final int FLAG_MASK_Z = 1 << FLAG_SHIFT_Z;
    private static final int FLAG_MASK_S = 1 << FLAG_SHIFT_S;

    private static final int FLAG_MASK_HNC = FLAG_MASK_H | FLAG_MASK_N | FLAG_MASK_C;

    // --------------------------------------------------------------------- //

    // Registers

    @Serialize
    private byte B, C, D, E, H, L, A, F;
    @Serialize
    private byte B2, C2, D2, E2, H2, L2, A2, F2;
    @Serialize
    private short IX, IY;
    @Serialize
    private short SP;
    @Serialize
    private byte I, R;
    @Serialize
    private short PC;

    // Interrupts

    @Serialize
    private boolean irqEnabled = false;

    @Serialize
    private boolean irqRequested = false;

    @Serialize
    private byte irqOpcode = 0;

    // Timing / Execution

    @Serialize
    private int cycles = 0;

    @Serialize
    private long totalCycles = 0L;

    @Serialize
    private boolean halted = false;

    // --------------------------------------------------------------------- //

    protected final Object interruptLock = new Object();

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
                irqOpcode = (byte) interrupt;
            }
        }
    }

    // --------------------------------------------------------------------- //
    // Object

    @Override
    public String toString() {
        return String.format(
                "%02X %02X %02X %02X %02X %02X %02X %02X | %04X %04X %04X %04X | ",
                B, C, D, E, H, L, A, F, BC(), DE(), HL(), AF()) +
                (FLAG_M() ? " S" : "") +
                (FLAG_Z() ? " Z" : "") +
                (FLAG_H() ? " H" : "") +
                (FLAG_PE() ? " P" : "") +
                (FLAG_N() ? " N" : "") +
                (FLAG_C() ? " C" : "");
    }

    // --------------------------------------------------------------------- //

    public void setPC(final int PC) {
        this.PC = (short) PC;
    }

    public void run(final int cycles) {
        if (!halted) {
            this.cycles += cycles;
            this.totalCycles += cycles;
            while (this.cycles > 0) step();
        }
    }

    public void reset() {
        B = C = D = E = H = L = A = F = 0;
        IX = IY = SP = 0;
        PC = 0;
        cycles = 0;
        totalCycles = 0L;
        irqEnabled = false;
        irqRequested = false;
        irqOpcode = 0;
        halted = false;

        //Unsafe.getUnsafe()
    }

    // --------------------------------------------------------------------- //

    // 16-bit register accessors.

    private short BC() {
        return (short) ((B << 8) | (C & 0xFF));
    }

    private void BC(final short value) {
        B = (byte) (value >>> 8);
        C = (byte) value;
    }

    private short DE() {
        return (short) ((D << 8) | (E & 0xFF));
    }

    private void DE(final short value) {
        D = (byte) (value >>> 8);
        E = (byte) value;
    }

    private short HL() {
        return (short) ((H << 8) | (L & 0xFF));
    }

    private void HL(final short value) {
        H = (byte) (value >>> 8);
        L = (byte) value;
    }

    private short AF() {
        return (short) ((A << 8) | (F & 0xFF));
    }

    private void AF(final short value) {
        A = (byte) (value >>> 8);
        F = (byte) value;
    }

    private short BC2() {
        return (short) ((B2 << 8) | (C2 & 0xFF));
    }

    private void BC2(final short value) {
        B2 = (byte) (value >>> 8);
        C2 = (byte) value;
    }

    private short DE2() {
        return (short) ((D2 << 8) | (E2 & 0xFF));
    }

    private void DE2(final short value) {
        D2 = (byte) (value >>> 8);
        E2 = (byte) value;
    }

    private short HL2() {
        return (short) ((H2 << 8) | (L2 & 0xFF));
    }

    private void HL2(final short value) {
        H2 = (byte) (value >>> 8);
        L2 = (byte) value;
    }

    private short AF2() {
        return (short) ((A2 << 8) | (F2 & 0xFF));
    }

    private void AF2(final short value) {
        A2 = (byte) (value >>> 8);
        F2 = (byte) value;
    }

    // Flags accessors.

    private boolean FLAG_C() {
        return (F & FLAG_MASK_C) != 0;
    }

    private boolean FLAG_NC() {
        return (F & FLAG_MASK_C) == 0;
    }

    private void FLAG_C(final boolean value) {
        if (value) F = (byte) (F | FLAG_MASK_C);
        else F = (byte) (F & ~FLAG_MASK_C);
    }

    private boolean FLAG_N() {
        return (F & FLAG_MASK_N) != 0;
    }

    private boolean FLAG_PE() {
        return (F & FLAG_MASK_PV) != 0;
    }

    private boolean FLAG_PO() {
        return (F & FLAG_MASK_PV) == 0;
    }

    private boolean FLAG_H() {
        return (F & FLAG_MASK_H) != 0;
    }

    private boolean FLAG_Z() {
        return (F & FLAG_MASK_Z) != 0;
    }

    private boolean FLAG_NZ() {
        return (F & FLAG_MASK_Z) == 0;
    }

    private boolean FLAG_M() {
        return (F & FLAG_MASK_S) != 0;
    }

    private boolean FLAG_P() {
        return (F & FLAG_MASK_S) == 0;
    }

    // Memory

    private byte peek8(final short address) {
        return (byte) controller.mapAndRead(address & 0xFFFF);
    }

    private void poke8(final short address, final byte value) {
        controller.mapAndWrite(address & 0xFFFF, value & 0xFF);
    }

    private byte read8() {
        final byte result = peek8(PC);
        PC += 1;
        return result;
    }

    private short peek16(final short address) {
        final int low = peek8(address) & 0xFF;
        final int high = peek8((short) (address + 1)) << 8;
        return (short) (high | low);
    }

    private void poke16(final short address, final short value) {
        poke8(address, (byte) value);
        poke8((short) (address + 1), (byte) (value >>> 8));
    }

    private short read16() {
        final short result = peek16(PC);
        PC += 2;
        return result;
    }

    // IO

    private byte ioRead(final short port) {
        return (byte) controller.mapAndRead(0x10000 + (port & 0xFFFF));
    }

    private void ioWrite(final short port, final byte data) {
        controller.mapAndWrite(0x10000 + (port & 0xFFFF), data & 0xFF);
    }

    // Control

    private void push(final short value) {
        SP -= 2;
        poke16(SP, value);
    }

    private short pop() {
        final short result = peek16(SP);
        SP += 2;
        return result;
    }

    // Bitwise

    private void bit(final int bit, final byte value) {
        final int u = value & 0xFF;
        final int s = (u >>> bit) & 1;
        F &= ~FLAG_MASK_Z;
        F |= s << FLAG_SHIFT_Z;
    }

    private static byte res(final int bit, final byte value) {
        return (byte) (value & ~(1 << bit));
    }

    private static byte set(final int bit, final byte value) {
        return (byte) (value | (1 << bit));
    }

    // ALU

    private byte inc(final byte value) {
        final int u = value & 0xFF;
        final int result = u + 1;
        final int carry = u ^ result;

        byte f = (byte) (F & FLAG_MASK_C);
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        f |= OVERFLOW_TABLE[(carry >>> 7) & 0b11];

        F = f;
        return (byte) result;
    }

    private byte dec(final byte value) {
        final int u = value & 0xFF;
        final int result = u - 1;
        final int carry = u ^ result;

        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        f |= OVERFLOW_TABLE[(carry >>> 7) & 0b11];

        F = f;
        return (byte) result;
    }

    private void add(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul + ur;
        final int carry = ul ^ ur ^ result;

        byte f = 0;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= result >>> (8 - FLAG_SHIFT_C);

        // I8080 compat {
        f &= ~FLAG_MASK_PV;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        // }

        A = (byte) result;
        F = f;
    }

    private void adc(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul + ur + (F & FLAG_MASK_C);
        final int carry = ul ^ ur ^ result;

        byte f = 0;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= result >>> (8 - FLAG_SHIFT_C);

        A = (byte) result;
        F = f;
    }

    private void sub(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul - ur;
        int carry = ul ^ ur ^ result;

        byte f = FLAG_MASK_N;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        carry &= 0b1_10000000;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= carry >>> (8 - FLAG_SHIFT_C);

        A = (byte) result;
        F = f;
    }

    private void sbc(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul - ur - (F & FLAG_MASK_C);
        int carry = ul ^ ur ^ result;

        byte f = FLAG_MASK_N;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        carry &= 0b1_10000000;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= carry >>> (8 - FLAG_SHIFT_C);

        A = (byte) result;
        F = f;
    }

    private void and(final byte rhs) {
        A &= rhs;

        byte f = FLAG_MASK_H;
        f |= A & FLAG_MASK_S;
        if (A == 0) f |= FLAG_MASK_Z;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        F = f;
    }

    private void or(final byte rhs) {
        A |= rhs;

        byte f = 0;
        f |= A & FLAG_MASK_S;
        if (A == 0) f |= FLAG_MASK_Z;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        F = f;
    }

    private void xor(final byte rhs) {
        A ^= rhs;

        byte f = 0;
        f |= A & FLAG_MASK_S;
        if (A == 0) f |= FLAG_MASK_Z;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        F = f;
    }

    private void cp(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul - ur;
        int carry = ul ^ ur ^ result;

        byte f = FLAG_MASK_N;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        carry &= 0b1_10000000;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= carry >>> (8 - FLAG_SHIFT_C);

        F = f;
    }

    private void neg() {
        final int u = A & 0xFF;
        final int result = -u;
        int carry = u ^ result;

        byte f = FLAG_MASK_N;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= carry & FLAG_MASK_H;
        carry &= 0b1_10000000;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= carry >> (8 - FLAG_SHIFT_C);

        A = (byte) result;
        F = f;
    }

    private byte rr(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a & 1);
        final int result = (a >>> 1) | ((F & FLAG_MASK_C) << 7);

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte rrc(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a & 1);
        final int result = (a >>> 1) | (carry << 7);

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte rl(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a >>> 7);
        final int result = (a << 1) | (F & FLAG_MASK_C);

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte rlc(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a >>> 7);
        final int result = (a << 1) | carry;

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte sla(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u >>> 7);
        final int result = u << 1;

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte sll(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u >>> 7);
        final int result = (u << 1) | 1;

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte sra(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u & 1);
        final int result = u >> 1;

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private byte srl(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u & 1);
        final int result = u >>> 1;

        byte f = carry;
        f |= result & FLAG_MASK_S;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;
        F = f;

        return (byte) result;
    }

    private void rld() {
        // TODO
    }

    private void rrd() {
        // TODO
    }

    private void daa() {
        // The following algorithm is from comp.sys.sinclair's FAQ.
        final int a = A & 0xFF;
        final int c;
        int d;
        if (a > 0x99 || FLAG_C()) {
            c = FLAG_MASK_C;
            d = 0x60;
        } else
            c = d = 0;

        if ((a & 0x0f) > 0x09 || FLAG_H())
            d += 0x06;
        A += FLAG_N() ? -d : +d;

        byte f = 0;
        f |= A & FLAG_MASK_S;
        if (A == 0) f |= FLAG_MASK_Z;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        f |= ((A ^ a) & FLAG_MASK_H);
        f |= (F & FLAG_MASK_N);
        f |= c;
        F = f;
    }

    private void add16(final int rhs) {
        final short hl = HL();
        final short result = (short) (hl + (rhs & 0xFFFF));
        final int carry = hl ^ rhs ^ result;

        byte f = F;
        f &= ~FLAG_MASK_HNC;
        f |= carry & 0b00001000_00000000;
        if ((carry & 0b10000000_00000000) != 0) f |= FLAG_MASK_C;
        F = f;

        HL(result);
    }

    private void adc16(final short rhs) {
        // TODO Flags
        final int result = (HL() & 0xFFFF) + (rhs & 0xFFFF) + (FLAG_C() ? 1 : 0);
        FLAG_C((result & 0xFFFF) != result);
        HL((short) result);
    }

    private void sbc16(final short rhs) {
        // TODO Flags
        final int result = (HL() & 0xFFFF) - (rhs & 0xFFFF) - (FLAG_C() ? 1 : 0);
        FLAG_C((result & 0xFFFF) != result);
        HL((short) result);
    }

    private static int computeParity(final byte value) {
        final int v = value & 0xFF;
        final int i = v ^ (v >>> 4);
        final int p = 0x6996 >>> (i & 0x0F);
        return 1 - (p & 1);
    }

    // --------------------------------------------------------------------- //

    private void step() {
        processCode();
        processInterrupts();
    }

    private void processCode() {
        cycles -= execute(read8());
    }

    private void processInterrupts() {
        synchronized (this) {
            if (irqEnabled && irqRequested) {
                irqEnabled = false;
                cycles -= execute(irqOpcode);
                irqRequested = false;
            }
        }
    }

    int execute(final byte opcode) {
        switch (opcode) {
            case (byte) 0xDD:
                return evaluateDD(read8());
            case (byte) 0xFD:
                return evaluateFD(read8());
            default:
                return evaluate(opcode);
        }
    }

    private int evaluateDD(final byte opcode) {
        final int x = (opcode & 0b11000000) >>> 6;
        final int y = (opcode & 0b00111000) >>> 3;
        final int z = (opcode & 0b00000111);
        final int p = y >>> 1;
        final int q = y & 1;

        return 0;
    }

    private int evaluateFD(final byte opcode) {
        final int x = (opcode & 0b11000000) >>> 6;
        final int y = (opcode & 0b00111000) >>> 3;
        final int z = (opcode & 0b00000111);
        final int p = y >>> 1;
        final int q = y & 1;

        return 0;
    }

    private int evaluate(byte opcode) {
//        if (DASM[opcode & 0xFF] != null)
//            System.out.println(DASM[opcode & 0xFF]);
//        else
//            System.out.println(String.format("%02X", opcode));
        for (; ; ) {
            int x = (opcode & 0b11000000) >>> 6;
            int y = (opcode & 0b00111000) >>> 3;
            int z = (opcode & 0b00000111);
            int p = y >>> 1;
            int q = y & 1;

            switch (x) {
                case 0:
                    switch (z) {
                        case 0: // Relative jumps and assorted ops
                            switch (y) {
                                case 0: // NOP
                                    return 4;
                                case 1: { // EX AF,AF'
                                    final short t = AF2();
                                    AF2(AF());
                                    AF(t);
                                    return 4;
                                }
                                case 2: { // DJNZ e
                                    final byte e = read8();
                                    if (--B != 0) {
                                        PC += e;
                                        return 13;
                                    } else {
                                        return 8;
                                    }
                                }
                                case 3: // JR e
                                    PC += read8();
                                    return 12;
                                case 4:
                                case 5:
                                case 6:
                                case 7: { // JR cc[y-4],e
                                    final byte e = read8();
                                    if (cc[y - 4].apply()) {
                                        PC += e;
                                        return 12;
                                    } else {
                                        return 7;
                                    }
                                }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 1: // 16-bit load immediate/add
                            switch (q) {
                                case 0: // LD rp[p],nn
                                    rpw[p].apply(read16());
                                    return 10;
                                case 1: // ADD HL,rp[p]
                                    add16(rpr[p].apply());
                                    return 11;
                                default:
                                    throw new IllegalStateException();
                            }
                        case 2: // Indirect loading
                            switch (q) {
                                case 0:
                                    switch (p) {
                                        case 0: // LD (BC),A
                                            poke8(BC(), A);
                                            return 7;
                                        case 1: // LD (DE),A
                                            poke8(DE(), A);
                                            return 7;
                                        case 2: // LD (nn),HL
                                            poke16(read16(), HL());
                                            return 16;
                                        case 3: // LD (nn),A
                                            poke8(read16(), A);
                                            return 13;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                case 1:
                                    switch (p) {
                                        case 0: // LD A,(BC)
                                            A = peek8(BC());
                                            return 7;
                                        case 1: // LD A,(DE)
                                            A = peek8(DE());
                                            return 7;
                                        case 2: // LD HL,(nn)
                                            HL(peek16(read16()));
                                            return 16;
                                        case 3: // LD A,(nn)
                                            A = peek8(read16());
                                            return 13;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 3: // 16-bit INC/DEC
                            switch (q) {
                                case 0: // INC rp[p]
                                    rpw[p].apply((short) (rpr[p].apply() + 1));
                                    return 6;
                                case 1: // DEC rp[p]
                                    rpw[p].apply((short) (rpr[p].apply() - 1));
                                    return 6;
                                default:
                                    throw new IllegalStateException();
                            }
                        case 4: // 8-bit INC: INC r[y]
                            rw[y].apply(inc(rr[y].apply()));
                            return INC_DEC_T[y];
                        case 5: // 8-bit DEC: DEC r[y]
                            rw[y].apply(dec(rr[y].apply()));
                            return INC_DEC_T[y];
                        case 6: // 8-bit load immediate: LD r[y],n
                            rw[y].apply(read8());
                            return LD_R_N_T[y];
                        case 7: // Assorted operations or accumulator/flags
                            switch (y) {
                                case 0: // RLCA
                                    A = rlc(A);
                                    return 4;
                                case 1: // RRCA
                                    A = rrc(A);
                                    return 4;
                                case 2: // RLA
                                    A = rl(A);
                                    return 4;
                                case 3: // RRA
                                    A = rr(A);
                                    return 4;
                                case 4: // DAA
                                    daa();
                                    return 4;
                                case 5: // CPL
                                    A = (byte) ~A;
                                    F |= FLAG_MASK_H | FLAG_MASK_N;
                                    return 4;
                                case 6: // SCF
                                    F &= ~FLAG_MASK_H & ~FLAG_MASK_N;
                                    F |= FLAG_MASK_C;
                                    return 4;
                                case 7: { // CCF
                                    final int carry = F & FLAG_MASK_C;
                                    F &= ~FLAG_MASK_H & ~FLAG_MASK_C;
                                    F |= carry << FLAG_SHIFT_H;
                                    F |= carry ^ FLAG_MASK_C;
                                    return 4;
                                }
                                default:
                                    throw new IllegalStateException();
                            }
                        default:
                            throw new IllegalStateException();
                    }
                case 1:
                    if (z == 6 && y == 6) { // Exception (replaces LD (HL),(HL)): HALT
                        halted = true;
                        cycles = 4;
                        return 4;
                    } else { // 8-bit loading: LD r[y],r[z]
                        rw[y].apply(rr[z].apply());
                        final int t1 = LD_R_R_T[y];
                        final int t2 = LD_R_R_T[z];
                        return t1 > t2 ? t1 : t2;
                    }
                case 2: // Operator on accumulator and register/memory location: alu[y] r[z]
                    alu[y].apply(rr[z].apply());
                    return ALU_T[y];
                case 3:
                    switch (z) {
                        case 0: // Conditional return: RET cc[y]
                            if (cc[y].apply()) {
                                PC = pop();
                                return 11;
                            } else {
                                return 5;
                            }
                        case 1: // POP & various ops
                            switch (q) {
                                case 0: // POP rp2[p]
                                    rp2w[p].apply(pop());
                                    return 10;
                                case 1:
                                    switch (p) {
                                        case 0: // RET
                                            PC = pop();
                                            return 10;
                                        case 1: { // EXX
                                            {
                                                final short t = BC2();
                                                BC2(BC());
                                                BC(t);
                                            }
                                            {
                                                final short t = DE2();
                                                DE2(DE());
                                                DE(t);
                                            }
                                            {
                                                final short t = HL2();
                                                HL2(HL());
                                                HL(t);
                                            }
                                            return 4;
                                        }
                                        case 2: // JP (HL)
                                            PC = HL();
                                            return 4;
                                        case 3: // LD SP,HL
                                            SP = HL();
                                            return 6;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 2: { // Conditional jump: JP cc[y],nn
                            final short nn = read16();
                            if (cc[y].apply()) {
                                PC = nn;
                            }
                            return 10;
                        }
                        case 3: // Assorted operations
                            switch (y) {
                                case 0: // JP nn
                                    PC = read16();
                                    return 10;
                                case 1: { // (CB prefix)
                                    opcode = read8();
                                    x = (opcode & 0b11000000) >>> 6;
                                    y = (opcode & 0b00111000) >>> 3;
                                    z = (opcode & 0b00000111);

                                    switch (x) {
                                        case 0: // Roll/shift register or memory location: rot[y] r[z]
                                            rw[z].apply(rot[y].apply(rr[z].apply()));
                                            return ROT_T[z];
                                        case 1: // Test bit: BIT y,r[z]
                                            bit(y, rr[z].apply());
                                            return BIT_T[z];
                                        case 2: // Reset bit: RES y,r[z]
                                            rw[z].apply(res(y, rr[z].apply()));
                                            return RES_T[z];
                                        case 3: // Set bit: SET y,r[z]
                                            rw[z].apply(set(y, rr[z].apply()));
                                            return SET_T[z];
                                        default:
                                            // "NOP"
                                            return 4;
                                    }
                                }
                                case 2: // OUT (n),A
                                    ioWrite((short) (read8() & 0xFF), A);
                                    return 11;
                                case 3: // IN A,(n)
                                    A = ioRead((short) (read8() & 0xFF));
                                    return 11;
                                case 4: { // EX (SP), HL
                                    final short t = HL();
                                    HL(peek16(SP));
                                    poke16(SP, t);
                                    return 19;
                                }
                                case 5: { // EX DE,HL
                                    final short t = DE();
                                    DE(HL());
                                    HL(t);
                                    return 4;
                                }
                                case 6: // DI
                                    synchronized (interruptLock) {
                                        irqEnabled = false;
                                    }
                                    return 4;
                                case 7: // EI
                                    synchronized (interruptLock) {
                                        irqEnabled = true;
                                    }
                                    return 4;
                                default:
                                    throw new IllegalStateException();
                            }
                        case 4: { // Conditional call: CALL cc[y],nn
                            final short nn = read16();
                            if (cc[y].apply()) {
                                push(PC);
                                PC = nn;
                                return 17;
                            } else {
                                return 10;
                            }
                        }
                        case 5: // PUSH & various ops
                            switch (q) {
                                case 0: // PUSH rp2[p]
                                    push(rp2r[p].apply());
                                    return 11;
                                case 1:
                                    switch (p) {
                                        case 0: { // CALL nn
                                            final short nn = read16();
                                            push(PC);
                                            PC = nn;
                                            return 17;
                                        }
                                        case 1: // (DD prefix); Handled in other method.
                                        case 2: { // (ED prefix)
                                            opcode = read8();
                                            x = (opcode & 0b11000000) >>> 6;
                                            y = (opcode & 0b00111000) >>> 3;
                                            z = (opcode & 0b00000111);
                                            p = y >>> 1;
                                            q = y & 1;

                                            switch (x) {
                                                case 1:
                                                    switch (z) {
                                                        case 0: // Input from port with 16-bit address
                                                            // TODO
                                                            return 0;
                                                        case 1: // Output to port with 16-bit address
                                                            // TODO
                                                            return 0;
                                                        case 2: // 16-bit add/subtract with carry
                                                            switch (q) {
                                                                case 0: // SBC HL,rp[p]
                                                                    sbc16(rpr[p].apply());
                                                                    return 15;
                                                                case 1: // ADC HL,rp[p]
                                                                    adc16(rpr[p].apply());
                                                                    return 15;
                                                                default:
                                                                    throw new IllegalStateException();
                                                            }
                                                        case 3: // Retrieve/store register pair from/to immediate address
                                                            // TODO
                                                            return 0;
                                                        case 4: // Negate accumulator
                                                            neg();
                                                            return 8;
                                                        case 5: // Return from interrupt
                                                            switch (y) {
                                                                case 1: // RETI
                                                                    // TODO
                                                                    return 14;
                                                                default: // RETN
                                                                    // TODO
                                                                    return 14;
                                                            }
                                                        case 6: // Set interrupt mode: IM im[y]
                                                            // TODO
                                                            return 8;
                                                        case 7: // Assorted ops
                                                            switch (y) {
                                                                case 0: // LD I,A
                                                                    I = A;
                                                                    return 9;
                                                                case 1: // LD R,A
                                                                    R = A;
                                                                    return 9;
                                                                case 2: // LD A,I
                                                                    A = I;
                                                                    return 9;
                                                                case 3: // LD A,R
                                                                    A = R;
                                                                    return 9;
                                                                case 4: // RRD
                                                                    rrd();
                                                                    return 18;
                                                                case 5: // RLD
                                                                    rld();
                                                                    return 18;
                                                                case 6:
                                                                case 7: // NOP
                                                                    return 4;
                                                                default:
                                                                    throw new IllegalStateException();
                                                            }
                                                    }
                                                    return 0;
                                                case 2:
                                                    switch (z) {
                                                        case 0:
                                                        case 1:
                                                        case 2:
                                                        case 3: // Block instruction: bli[y,z]
                                                            // TODO
                                                            return 0;
                                                        default: // "NOP
                                                            return 4;
                                                    }
                                                case 0:
                                                case 3: // "NOP"
                                                    return 4;
                                            }
                                            return 4;
                                        }
                                        case 3: // (FD prefix); Handled in other method.
                                        default:
                                            throw new IllegalStateException();
                                    }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 6: // Operate on accumulator and immediate operand: alu[y] n
                            alu[y].apply(read8());
                            return 7;
                        case 7: // Restart: RST y*8
                            push(PC);
                            PC = (short) (y * 8);
                            return 11;
                        default:
                            throw new IllegalStateException();
                    }
                default:
                    throw new IllegalStateException();
            }
        }
    }

    // --------------------------------------------------------------------- //
    // Glorious lookup tables.

    // 8-bit registers (read)
    private final ReadAccess8[] rr = {() -> B, () -> C, () -> D, () -> E, () -> H, () -> L, () -> peek8(HL()), () -> A};
    // 8-bit registers (write)
    private final WriteAccess8[] rw = {x -> B = x, x -> C = x, x -> D = x, x -> E = x, x -> H = x, x -> L = x, x -> poke8(HL(), x), x -> A = x};
    // Register pairs featuring SP (read)
    private final ReadAccess16[] rpr = {this::BC, this::DE, this::HL, () -> SP};
    // Register pairs featuring SP (write)
    private final WriteAccess16[] rpw = {this::BC, this::DE, this::HL, x -> SP = x};
    // Register pairs featuring AF (read)
    private final ReadAccess16[] rp2r = {this::BC, this::DE, this::HL, this::AF};
    // Register pairs featuring AF (write)
    private final WriteAccess16[] rp2w = {this::BC, this::DE, this::HL, this::AF};
    // Conditions
    private final Condition[] cc = {this::FLAG_NZ, this::FLAG_Z, this::FLAG_NC, this::FLAG_C, this::FLAG_PO, this::FLAG_PE, this::FLAG_P, this::FLAG_M};
    // Arithmetic/logic operations
    private final WriteAccess8[] alu = {this::add, this::adc, this::sub, this::sbc, this::and, this::xor, this::or, this::cp};
    // Rotation/shift operations
    private final Rotation[] rot = {this::rlc, this::rrc, this::rl, this::rr, this::sla, this::sra, this::sll, this::srl};

    // Timings for incrementing/decrementing 16-bit registers/memory.
    private static final int[] INC_DEC_T = {4, 4, 4, 4, 4, 4, 11, 4};
    // Timings for loading data into registers/memory.
    private static final int[] LD_R_N_T = {7, 7, 7, 7, 7, 7, 10, 7};
    // Timings for ALU operations based on register index.
    private static final int[] ALU_T = {4, 4, 4, 4, 4, 4, 7, 4};
    // Timings for copying data between registers/memory.
    private static final int[] LD_R_R_T = ALU_T;
    // Timings for bit set operations based on register index.
    private static final int[] ROT_T = {8, 8, 8, 8, 8, 8, 15, 8};
    // Timings for bit get operations based on register index.
    private static final int[] BIT_T = {8, 8, 8, 8, 8, 8, 12, 8};
    // Timings for bit reset operations based on register index.
    private static final int[] RES_T = {8, 8, 8, 8, 8, 8, 15, 8};
    // Timings for bit set operations based on register index.
    private static final int[] SET_T = RES_T;
    // There is an overflow if the xor of the carry out and the carry of the
    // most significant bit is not zero.
    private static final int[] OVERFLOW_TABLE = {0, FLAG_MASK_PV, FLAG_MASK_PV, 0};

    // --------------------------------------------------------------------- //
    // Functional interfaces for lookup tables.

    @FunctionalInterface
    interface ReadAccess8 {
        byte apply();
    }

    @FunctionalInterface
    interface ReadAccess16 {
        short apply();
    }

    @FunctionalInterface
    interface WriteAccess8 {
        void apply(final byte value);
    }

    @FunctionalInterface
    interface WriteAccess16 {
        void apply(final short value);
    }

    @FunctionalInterface
    interface Condition {
        boolean apply();
    }

    @FunctionalInterface
    interface Rotation {
        byte apply(final byte value);
    }

    // --------------------------------------------------------------------- //
    // Mapping of opcodes to mnemonics for debugging readability.

    private static final String[] DASM = new String[0x100];

    static {
        DASM[0x7F] = "MOV A,A";
        DASM[0x78] = "MOV A,B";
        DASM[0x79] = "MOV A,C";
        DASM[0x7A] = "MOV A,D";
        DASM[0x7B] = "MOV A,E";
        DASM[0x7C] = "MOV A,H";
        DASM[0x7D] = "MOV A,L";
        DASM[0x7E] = "MOV A,M";
        DASM[0x0A] = "LDAX B";
        DASM[0x1A] = "LDAX D";
        DASM[0x3A] = "LDA nn";
        DASM[0x47] = "MOV B,A";
        DASM[0x40] = "MOV B,B";
        DASM[0x41] = "MOV B,C";
        DASM[0x42] = "MOV B,D";
        DASM[0x43] = "MOV B,E";
        DASM[0x44] = "MOV B,H";
        DASM[0x45] = "MOV B,L";
        DASM[0x46] = "MOV B,M";
        DASM[0x4F] = "MOV C,A";
        DASM[0x48] = "MOV C,B";
        DASM[0x49] = "MOV C,C";
        DASM[0x4A] = "MOV C,D";
        DASM[0x4B] = "MOV C,E";
        DASM[0x4C] = "MOV C,H";
        DASM[0x4D] = "MOV C,L";
        DASM[0x4E] = "MOV C,M";
        DASM[0x57] = "MOV D,A";
        DASM[0x50] = "MOV D,B";
        DASM[0x51] = "MOV D,C";
        DASM[0x52] = "MOV D,D";
        DASM[0x53] = "MOV D,E";
        DASM[0x54] = "MOV D,H";
        DASM[0x55] = "MOV D,L";
        DASM[0x56] = "MOV D,M";
        DASM[0x5F] = "MOV E,A";
        DASM[0x58] = "MOV E,B";
        DASM[0x59] = "MOV E,C";
        DASM[0x5A] = "MOV E,D";
        DASM[0x5B] = "MOV E,E";
        DASM[0x5C] = "MOV E,H";
        DASM[0x5D] = "MOV E,L";
        DASM[0x5E] = "MOV E,M";
        DASM[0x67] = "MOV H,A";
        DASM[0x60] = "MOV H,B";
        DASM[0x61] = "MOV H,C";
        DASM[0x62] = "MOV H,D";
        DASM[0x63] = "MOV H,E";
        DASM[0x64] = "MOV H,H";
        DASM[0x65] = "MOV H,L";
        DASM[0x66] = "MOV H,M";
        DASM[0x6F] = "MOV L,A";
        DASM[0x68] = "MOV L,B";
        DASM[0x69] = "MOV L,C";
        DASM[0x6A] = "MOV L,D";
        DASM[0x6B] = "MOV L,E";
        DASM[0x6C] = "MOV L,H";
        DASM[0x6D] = "MOV L,L";
        DASM[0x6E] = "MOV L,M";
        DASM[0x77] = "MOV M,A";
        DASM[0x70] = "MOV M,B";
        DASM[0x71] = "MOV M,C";
        DASM[0x72] = "MOV M,D";
        DASM[0x73] = "MOV M,E";
        DASM[0x74] = "MOV M,H";
        DASM[0x75] = "MOV M,L";
        DASM[0x3E] = "MVI A,n";
        DASM[0x06] = "MVI B,n";
        DASM[0x0E] = "MVI C,n";
        DASM[0x16] = "MVI D,n";
        DASM[0x1E] = "MVI E,n";
        DASM[0x26] = "MVI H,n";
        DASM[0x2E] = "MVI L,n";
        DASM[0x36] = "MVI M,n";
        DASM[0x02] = "STAX B";
        DASM[0x12] = "STAX D";
        DASM[0x32] = "STA nn";
        DASM[0x01] = "LXI B,nn";
        DASM[0x11] = "LXI D,nn";
        DASM[0x21] = "LXI H,nn";
        DASM[0x31] = "LXI SP,nn";
        DASM[0x2A] = "LHLD nn";
        DASM[0x22] = "SHLD nn";
        DASM[0xF9] = "SPHL";
        DASM[0xEB] = "XCHG";
        DASM[0xE3] = "XTHL";
        DASM[0x87] = "ADD A";
        DASM[0x80] = "ADD B";
        DASM[0x81] = "ADD C";
        DASM[0x82] = "ADD D";
        DASM[0x83] = "ADD E";
        DASM[0x84] = "ADD H";
        DASM[0x85] = "ADD L";
        DASM[0x86] = "ADD M";
        DASM[0xC6] = "ADI n";
        DASM[0x8F] = "ADC A";
        DASM[0x88] = "ADC B";
        DASM[0x89] = "ADC C";
        DASM[0x8A] = "ADC D";
        DASM[0x8B] = "ADC E";
        DASM[0x8C] = "ADC H";
        DASM[0x8D] = "ADC L";
        DASM[0x8E] = "ADC M";
        DASM[0xCE] = "ACI n";
        DASM[0x97] = "SUB A";
        DASM[0x90] = "SUB B";
        DASM[0x91] = "SUB C";
        DASM[0x92] = "SUB D";
        DASM[0x93] = "SUB E";
        DASM[0x94] = "SUB H";
        DASM[0x95] = "SUB L";
        DASM[0x96] = "SUB M";
        DASM[0xD6] = "SUI n";
        DASM[0x9F] = "SBB A";
        DASM[0x98] = "SBB B";
        DASM[0x99] = "SBB C";
        DASM[0x9A] = "SBB D";
        DASM[0x9B] = "SBB E";
        DASM[0x9C] = "SBB H";
        DASM[0x9D] = "SBB L";
        DASM[0x9E] = "SBB M";
        DASM[0xDE] = "SBI n";
        DASM[0x09] = "DAD B";
        DASM[0x19] = "DAD D";
        DASM[0x29] = "DAD H";
        DASM[0x39] = "DAD SP";
        DASM[0xF3] = "DI";
        DASM[0xFB] = "EI";
        DASM[0x00] = "NOP";
        DASM[0x76] = "HLT";
        DASM[0x3C] = "INR A";
        DASM[0x04] = "INR B";
        DASM[0x0C] = "INR C";
        DASM[0x14] = "INR D";
        DASM[0x1C] = "INR E";
        DASM[0x24] = "INR H";
        DASM[0x2C] = "INR L";
        DASM[0x34] = "INR M";
        DASM[0x3D] = "DCR A";
        DASM[0x05] = "DCR B";
        DASM[0x0D] = "DCR C";
        DASM[0x15] = "DCR D";
        DASM[0x1D] = "DCR E";
        DASM[0x25] = "DCR H";
        DASM[0x2D] = "DCR L";
        DASM[0x35] = "DCR M";
        DASM[0x03] = "INX B";
        DASM[0x13] = "INX D";
        DASM[0x23] = "INX H";
        DASM[0x33] = "INX SP";
        DASM[0x0B] = "DCX B";
        DASM[0x1B] = "DCX D";
        DASM[0x2B] = "DCX H";
        DASM[0x3B] = "DCX SP";
        DASM[0x27] = "DAA";
        DASM[0x2F] = "CMA";
        DASM[0x37] = "STC";
        DASM[0x3F] = "CMC";
        DASM[0x07] = "RLC";
        DASM[0x0F] = "RRC";
        DASM[0x17] = "RAL";
        DASM[0x1F] = "RAR";
        DASM[0xA7] = "ANA A";
        DASM[0xA0] = "ANA B";
        DASM[0xA1] = "ANA C";
        DASM[0xA2] = "ANA D";
        DASM[0xA3] = "ANA E";
        DASM[0xA4] = "ANA H";
        DASM[0xA5] = "ANA L";
        DASM[0xA6] = "ANA M";
        DASM[0xE6] = "ANI n";
        DASM[0xAF] = "XRA A";
        DASM[0xA8] = "XRA B";
        DASM[0xA9] = "XRA C";
        DASM[0xAA] = "XRA D";
        DASM[0xAB] = "XRA E";
        DASM[0xAC] = "XRA H";
        DASM[0xAD] = "XRA L";
        DASM[0xAE] = "XRA M";
        DASM[0xEE] = "XRI n";
        DASM[0xB7] = "ORA A";
        DASM[0xB0] = "ORA B";
        DASM[0xB1] = "ORA C";
        DASM[0xB2] = "ORA D";
        DASM[0xB3] = "ORA E";
        DASM[0xB4] = "ORA H";
        DASM[0xB5] = "ORA L";
        DASM[0xB6] = "ORA M";
        DASM[0xF6] = "ORI n";
        DASM[0xBF] = "CMP A";
        DASM[0xB8] = "CMP B";
        DASM[0xB9] = "CMP C";
        DASM[0xBA] = "CMP D";
        DASM[0xBB] = "CMP E";
        DASM[0xBC] = "CMP H";
        DASM[0xBD] = "CMP L";
        DASM[0xBE] = "CMP M";
        DASM[0xFE] = "CPI n";
        DASM[0xC3] = "JMP aa";
        DASM[0xC2] = "JNZ aa";
        DASM[0xCA] = "JZ aa";
        DASM[0xD2] = "JNC aa";
        DASM[0xDA] = "JC aa";
        DASM[0xE2] = "JPO aa";
        DASM[0xEA] = "JPE aa";
        DASM[0xF2] = "JP aa";
        DASM[0xFA] = "JM aa";
        DASM[0xE9] = "PCHL";
        DASM[0xCD] = "CALL aa";
        DASM[0xC4] = "CNZ aa";
        DASM[0xCC] = "CZ aa";
        DASM[0xD4] = "CNC aa";
        DASM[0xDC] = "CC aa";
        DASM[0xE4] = "CPO aa";
        DASM[0xEC] = "CPE aa";
        DASM[0xF4] = "CP aa";
        DASM[0xFC] = "CM aa";
        DASM[0xC9] = "RET";
        DASM[0xC0] = "RNZ";
        DASM[0xC8] = "RZ";
        DASM[0xD0] = "RNC";
        DASM[0xD8] = "RC";
        DASM[0xE0] = "RPO";
        DASM[0xE8] = "RPE";
        DASM[0xF0] = "RP";
        DASM[0xF8] = "RM";
        DASM[0xC7] = "RST 0";
        DASM[0xCF] = "RST 1";
        DASM[0xD7] = "RST 2";
        DASM[0xDF] = "RST 3";
        DASM[0xE7] = "RST 4";
        DASM[0xEF] = "RST 5";
        DASM[0xF7] = "RST 6";
        DASM[0xFF] = "RST 7";
        DASM[0xC5] = "PUSH B";
        DASM[0xD5] = "PUSH D";
        DASM[0xE5] = "PUSH H";
        DASM[0xF5] = "PUSH PSW";
        DASM[0xC1] = "POP B";
        DASM[0xD1] = "POP D";
        DASM[0xE1] = "POP H";
        DASM[0xF1] = "POP PSW";
        DASM[0xDB] = "IN n";
        DASM[0xD3] = "OUT n";
    }
}
