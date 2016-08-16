package li.cil.circuity.server.processor.z80;

import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.lang.reflect.Array;

// http://www.zilog.com/manage_directlink.php?filepath=docs/z80/um0080
// Opcode decoding based on http://www.z80.info/decoding.htm
// Flag computation based on https://github.com/anotherlin/z80emu/

@Serializable
public final class Z80 extends AbstractBusDevice implements InterruptSink {
    private static final int FLAG_SHIFT_C = 0;
    private static final int FLAG_SHIFT_N = 1;
    private static final int FLAG_SHIFT_PV = 2;
    private static final int FLAG_SHIFT_H = 4;
    private static final int FLAG_SHIFT_Z = 6;
    private static final int FLAG_SHIFT_S = 7;

    @SuppressWarnings("PointlessBitwiseExpression")
    private static final int FLAG_MASK_C = 1 << FLAG_SHIFT_C;
    private static final int FLAG_MASK_N = 1 << FLAG_SHIFT_N;
    private static final int FLAG_MASK_PV = 1 << FLAG_SHIFT_PV;
    private static final int FLAG_MASK_H = 1 << FLAG_SHIFT_H;
    private static final int FLAG_MASK_Z = 1 << FLAG_SHIFT_Z;
    private static final int FLAG_MASK_S = 1 << FLAG_SHIFT_S;
    private static final int FLAG_MASK_SZPV = FLAG_MASK_S | FLAG_MASK_Z | FLAG_MASK_PV;
    private static final int FLAG_MASK_SZC = FLAG_MASK_S | FLAG_MASK_Z | FLAG_MASK_C;

    // --------------------------------------------------------------------- //

    @Serialize
    private Status status;
    @Serialize
    private int cycleBudget = 0;

    @Serialize
    private byte B, C, D, E, H, L, A, F;
    @Serialize
    private byte B2, C2, D2, E2, H2, L2, A2, F2;
    @Serialize
    private byte IXH, IXL, IYH, IYL;
    @Serialize
    private short SP;
    @Serialize
    private byte I, R;
    @Serialize
    private short PC;
    @Serialize
    private boolean IFF1, IFF2;
    @Serialize
    private InterruptMode IM;

    // --------------------------------------------------------------------- //

    protected final Object lock = new Object();

    public Z80() {
        for (int i = 0; i < res.length; i++) {
            final int y = i;
            res[i] = (byte v) -> res(y, v);
        }
        for (int i = 0; i < set.length; i++) {
            final int y = i;
            set[i] = (byte v) -> set(y, v);
        }
    }

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
        // TODO THIS IS BULLSHIT
        // No seriously, it's just for testing. Should replace with
        // providing multiple interrupts, then getting the index of
        // the one that's triggered and providing that.
        if (interrupt < 0) {
            nmi();
        } else {
            irq((byte) interrupt);
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * Reset the CPU and set the initial program counter value.
     * <p>
     * This will restore all registers to their initial values.
     *
     * @param pc allows setting the initial PC to set the starting offset.
     */
    public void reset(final int pc) {
        synchronized (lock) {
            status = Status.RUNNING;
            B = C = D = E = H = L = 0;
            AF((short) 0xFFFF);
            IXH = IXL = IYH = IYL = 0;
            SP = (short) 0xFFFF;
            I = R = 0;
            PC = (short) (pc & 0xFFFF);
            IFF1 = IFF2 = false;
            IM = InterruptMode.MODE_0;
            cycleBudget = 0;
        }
    }

    /**
     * Run the CPU for the specified number of cycles.
     * <p>
     * This will continue execution of the CPU's state simulation until the
     * specified number of cycles have passed.
     *
     * @param cycles the number of cycles to emulate.
     */
    public boolean run(final int cycles) {
        synchronized (lock) {
            // Don't allow saving up cycles.
            if (status == Status.HALTED) return false;
            cycleBudget += cycles;
            for (; ; ) {
                R = (byte) ((R & 0b10000000) | ((R + 1) & 0b01111111));
                final byte opcode = read8();
                cycleBudget -= 1;
                execute(opcode);
                if (status == Status.HALTED) return false;
                if (cycleBudget <= 0) return true;
            }
        }
    }

    /**
     * Request interrupt given the specified data.
     * <p>
     * If interrupts are currently enabled and the CPU is not currently in the
     * process of parsing a prefixed opcode, this will immediately process the
     * interrupt (i.e. push the current PC and adjust the PC based on the
     * current interrupt mode and specified data).
     * <p>
     * This method is threadsafe. It is legal to call this from any thread at
     * any time. Internal locking ensures this will be processed between two
     * operations.
     *
     * @param data the data for the interrupt.
     * @return <code>true</code> if the interrupt was successful; <code>false</code> otherwise.
     */
    public boolean irq(final byte data) {
        synchronized (lock) {
            if (!IFF1 || status == Status.PARSING_DD || status == Status.PARSING_FD) return false;
            status = Status.RUNNING;

            IFF1 = IFF2 = false;
            R = (byte) ((R & 0b1000000) | ((R + 1) & 0b01111111));
            switch (IM) {
                case MODE_0:
                    execute(data);
                    cycleBudget -= 2;
                    break;
                case MODE_1:
                    push(PC);
                    PC = 0x0038;
                    cycleBudget -= 7;
                    break;
                case MODE_2: {
                    push(PC);
                    final int h = (I & 0xFF) << 8;
                    final int l = data & 0xFE;
                    PC = (short) (h | l);
                    cycleBudget -= 13;
                    break;
                }
                default:
                    throw new IllegalStateException();
            }
        }
        return true;
    }

    /**
     * Perform an non-maskable interrupt.
     * <p>
     * This will immediately process the interrupt (i.e. push the current PC
     * and adjust the PC to the NMI address <code>0x0066</code>).
     * <p>
     * This method is threadsafe. It is legal to call this from any thread at
     * any time. Internal locking ensures this will be processed between two
     * operations.
     */
    public void nmi() {
        synchronized (lock) {
            if (status == Status.PARSING_DD || status == Status.PARSING_FD) return;
            status = Status.RUNNING;

//            IFF2 = IFF1;
            IFF1 = false;
            R = (byte) ((R & 0b1000000) | ((R + 1) & 0b01111111));

            push(PC);
            PC = 0x0066;

            cycleBudget -= 11;
        }
    }

    // --------------------------------------------------------------------- //

    // 8-bit register accessors.

    public byte B() {
        return B;
    }

    private void B(final ReadAccess8 value) {
        B = value.apply();
    }

    private void B(final Transform transform) {
        B = transform.apply(B);
    }

    public byte C() {
        return C;
    }

    private void C(final ReadAccess8 value) {
        C = value.apply();
    }

    private void C(final Transform transform) {
        C = transform.apply(C);
    }

    public byte D() {
        return D;
    }

    private void D(final ReadAccess8 value) {
        D = value.apply();
    }

    private void D(final Transform transform) {
        D = transform.apply(D);
    }

    public byte E() {
        return E;
    }

    private void E(final ReadAccess8 value) {
        E = value.apply();
    }

    private void E(final Transform transform) {
        E = transform.apply(E);
    }

    public byte H() {
        return H;
    }

    private void H(final ReadAccess8 value) {
        H = value.apply();
    }

    private void H(final Transform transform) {
        H = transform.apply(H);
    }

    public byte L() {
        return L;
    }

    private void L(final ReadAccess8 value) {
        L = value.apply();
    }

    private void L(final Transform transform) {
        L = transform.apply(L);
    }

    public byte A() {
        return A;
    }

    private void A(final ReadAccess8 value) {
        A = value.apply();
    }

    private void A(final Transform transform) {
        A = transform.apply(A);
    }

    public byte IXH() {
        return IXH;
    }

    private void IXH(final ReadAccess8 value) {
        IXH = value.apply();
    }

    private void IXH(final Transform transform) {
        IXH = transform.apply(IXH);
    }

    public byte IXL() {
        return IXL;
    }

    private void IXL(final ReadAccess8 value) {
        IXL = value.apply();
    }

    private void IXL(final Transform transform) {
        IXL = transform.apply(IXL);
    }

    public byte IYH() {
        return IYH;
    }

    private void IYH(final ReadAccess8 value) {
        IYH = value.apply();
    }

    private void IYH(final Transform transform) {
        IYH = transform.apply(IYH);
    }

    public byte IYL() {
        return IYL;
    }

    private void IYL(final ReadAccess8 value) {
        IYL = value.apply();
    }

    private void IYL(final Transform transform) {
        IYL = transform.apply(IYL);
    }

    // 16-bit register accessors.

    public short BC() {
        return (short) ((B << 8) | (C & 0xFF));
    }

    private void BC(final short value) {
        B = (byte) (value >>> 8);
        C = (byte) value;
    }

    public short DE() {
        return (short) ((D << 8) | (E & 0xFF));
    }

    private void DE(final short value) {
        D = (byte) (value >>> 8);
        E = (byte) value;
    }

    public short HL() {
        return (short) ((H << 8) | (L & 0xFF));
    }

    private void HL(final short value) {
        H = (byte) (value >>> 8);
        L = (byte) value;
    }

    public short AF() {
        return (short) ((A << 8) | (F & 0xFF));
    }

    private void AF(final short value) {
        A = (byte) (value >>> 8);
        F = (byte) value;
    }

    public short IX() {
        return (short) ((IXH << 8) | (IXL & 0xFF));
    }

    private void IX(final short value) {
        IXH = (byte) (value >>> 8);
        IXL = (byte) value;
    }

    public short IY() {
        return (short) ((IYH << 8) | (IYL & 0xFF));
    }

    private void IY(final short value) {
        IYH = (byte) (value >>> 8);
        IYL = (byte) value;
    }

    public short SP() {
        return SP;
    }

    private void SP(final short value) {
        SP = value;
    }

    // Flags accessors.

    private boolean FLAG_C() {
        return (F & FLAG_MASK_C) != 0;
    }

    private boolean FLAG_NC() {
        return (F & FLAG_MASK_C) == 0;
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
        cycleBudget -= 3;
        return (byte) controller.mapAndRead(address & 0xFFFF);
    }

    private void poke8(final short address, final byte value) {
        cycleBudget -= 3;
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

    private byte peekHL() {
        return peek8(HL());
    }

    private void pokeHL(final ReadAccess8 value) {
        poke8(HL(), value.apply());
    }

    private void indirectHL(final Transform transform) {
        final short hl = HL();
        cycleBudget -= 1;
        poke8(hl, transform.apply(peek8(hl)));
    }

    private byte peekIXd() {
        final int uix = IX() & 0xFFFF, ud = read8() & 0xFFFF;
        cycleBudget -= 5;
        return peek8((short) (uix + ud));
    }

    private void pokeIXd(final ReadAccess8 value) {
        final int uix = IX() & 0xFFFF, ud = read8() & 0xFFFF;
        cycleBudget -= 5;
        poke8((short) (uix + ud), value.apply());
    }

    private void indirectIXd(final Transform transform) {
        final int uix = IX() & 0xFFFF, ud = read8() & 0xFFFF;
        final short a = (short) (uix + ud);
        cycleBudget -= 5;
        poke8(a, transform.apply(peek8(a)));
    }

    private byte peekIYd() {
        final int uiy = IY() & 0xFFFF, ud = read8() & 0xFFFF;
        cycleBudget -= 5;
        return peek8((short) (uiy + ud));
    }

    private void pokeIYd(final ReadAccess8 value) {
        final int uiy = IY() & 0xFFFF, ud = read8() & 0xFFFF;
        cycleBudget -= 5;
        poke8((short) (uiy + ud), value.apply());
    }

    private void indirectIYd(final Transform transform) {
        final int uiy = IY() & 0xFFFF, ud = read8() & 0xFFFF;
        final short a = (short) (uiy + ud);
        cycleBudget -= 5;
        poke8(a, transform.apply(peek8(a)));
    }

    private byte peekIXdCB() {
        final int uix = IX() & 0xFFFF, ud = peek8((short) (PC - 2)) & 0xFFFF;
        cycleBudget -= 2;
        return peek8((short) (uix + ud));
    }

    private void pokeIXdCB(final ReadAccess8 value) {
        final int uix = IX() & 0xFFFF, ud = peek8((short) (PC - 2)) & 0xFFFF;
        cycleBudget -= 2;
        poke8((short) (uix + ud), value.apply());
    }

    private void indirectIXdCB(final Transform transform) {
        final int uix = IX() & 0xFFFF, ud = peek8((short) (PC - 2)) & 0xFFFF;
        final short a = (short) (uix + ud);
        cycleBudget -= 2;
        poke8(a, transform.apply(peek8(a)));
    }

    private byte peekIYdCB() {
        final int uiy = IY() & 0xFFFF, ud = peek8((short) (PC - 2)) & 0xFFFF;
        cycleBudget -= 2;
        return peek8((short) (uiy + ud));
    }

    private void pokeIYdCB(final ReadAccess8 value) {
        final int uiy = IY() & 0xFFFF, ud = peek8((short) (PC - 2)) & 0xFFFF;
        cycleBudget -= 2;
        poke8((short) (uiy + ud), value.apply());
    }

    private void indirectIYdCB(final Transform transform) {
        final int uiy = IY() & 0xFFFF, ud = peek8((short) (PC - 2)) & 0xFFFF;
        final short a = (short) (uiy + ud);
        cycleBudget -= 2;
        poke8(a, transform.apply(peek8(a)));
    }

    // IO

    private byte ioRead(final short port) {
        cycleBudget -= 4;
        return (byte) controller.mapAndRead(0x10000 + (port & 0xFFFF));
    }

    private void ioWrite(final short port, final byte data) {
        cycleBudget -= 4;
        controller.mapAndWrite(0x10000 + (port & 0xFFFF), data & 0xFF);
    }

    private byte ioRead() {
        return ioRead(BC());
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
        final int z = 1 - ((u >>> bit) & 1);
        F = (byte) (FLAG_MASK_H | (F & FLAG_MASK_C));
        F |= z << FLAG_SHIFT_Z;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= carry & FLAG_MASK_H;
        f |= OVERFLOW_TABLE[carry >>> 7];
        f |= result >>> (8 - FLAG_SHIFT_C);

        A = (byte) result;
        F = f;
    }

    private void adc(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul + ur + (F & FLAG_MASK_C);
        final int carry = ul ^ ur ^ result;

        byte f = 0;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if (A == 0) f |= FLAG_MASK_Z;
        else f |= A & FLAG_MASK_S;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        F = f;
    }

    private void or(final byte rhs) {
        A |= rhs;

        byte f = 0;
        if (A == 0) f |= FLAG_MASK_Z;
        else f |= A & FLAG_MASK_S;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        F = f;
    }

    private void xor(final byte rhs) {
        A ^= rhs;

        byte f = 0;
        if (A == 0) f |= FLAG_MASK_Z;
        else f |= A & FLAG_MASK_S;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        F = f;
    }

    private void cp(final byte rhs) {
        final int ul = A & 0xFF, ur = rhs & 0xFF;
        final int result = ul - ur;
        int carry = ul ^ ur ^ result;

        byte f = FLAG_MASK_N;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
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
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte rrc(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a & 1);
        final int result = (a >>> 1) | (carry << 7);

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte rl(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a >>> 7);
        final int result = (a << 1) | (F & FLAG_MASK_C);

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte rlc(final byte value) {
        final int a = value & 0xFF;
        final byte carry = (byte) (a >>> 7);
        final int result = (a << 1) | carry;

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte sla(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u >>> 7);
        final int result = u << 1;

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte sll(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u >>> 7);
        final int result = (u << 1) | 1;

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte sra(final byte value) {
        final byte carry = (byte) (value & 1);
        final int result = value >> 1;

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private byte srl(final byte value) {
        final int u = value & 0xFF;
        final byte carry = (byte) (u & 1);
        final int result = u >>> 1;

        byte f = carry;
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= computeParity((byte) result) << FLAG_SHIFT_PV;

        F = f;
        return (byte) result;
    }

    private void daa() {
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
        if (A == 0) f |= FLAG_MASK_Z;
        else f |= A & FLAG_MASK_S;
        f |= computeParity(A) << FLAG_SHIFT_PV;
        f |= ((A ^ a) & FLAG_MASK_H);
        f |= (F & FLAG_MASK_N);
        f |= c;
        F = f;
    }

    private short add16(final short value, final int rhs) {
        final int ul = value & 0xFFFF, ur = rhs & 0xFFFF;
        final int result = ul + ur;
        final int carry = ul ^ ur ^ result;

        byte f = (byte) (F & FLAG_MASK_SZPV);
        f |= (carry >>> 8) & FLAG_MASK_H;
        f |= carry >>> (16 - FLAG_SHIFT_C);

        F = f;
        return (short) result;
    }

    private short adc16(final short value, final short rhs) {
        final int ul = value & 0xFFFF, ur = rhs & 0xFFFF;
        final int result = ul + ur + (F & FLAG_MASK_C);
        final int carry = ul ^ ur ^ result;

        byte f = 0;
        if (result == 0) f |= FLAG_MASK_Z;
        else f |= (result >>> 8) & FLAG_MASK_S;
        f |= (carry >>> 8) & FLAG_MASK_H;
        f |= OVERFLOW_TABLE[carry >>> 15];
        f |= result >>> (16 - FLAG_SHIFT_C);

        F = f;
        return (short) result;
    }

    private short sbc16(final short value, final short rhs) {
        final int ul = value & 0xFFFF, ur = rhs & 0xFFFF;
        final int result = ul - ur - (F & FLAG_MASK_C);
        int carry = ul ^ ur ^ result;

        byte f = FLAG_MASK_N;
        if (result == 0) f |= FLAG_MASK_Z;
        else f |= (result >>> 8) & FLAG_MASK_S;
        f |= (carry >>> 8) & FLAG_MASK_H;
        carry &= 0b1_10000000_00000000;
        f |= OVERFLOW_TABLE[carry >>> 15];
        f |= carry >>> (16 - FLAG_SHIFT_C);

        F = f;
        return (short) result;
    }

    private static int computeParity(final byte value) {
        final int v = value & 0xFF;
        final int i = v ^ (v >>> 4);
        final int p = 0x6996 >>> (i & 0x0F);
        return 1 - (p & 1);
    }

    // Block instructions

    private void ldi() {
        ldx(1);
    }

    private void ldd() {
        ldx(-1);
    }

    private void ldx(final int d) {
        poke8(DE(), peekHL());
        DE((short) (DE() + d));
        HL((short) (HL() + d));
        BC((short) (BC() - 1));

        cycleBudget -= 2;

        byte f = (byte) (F & FLAG_MASK_SZC);
        if (BC() != 0) f |= FLAG_MASK_PV;
        F = f;
    }

    private void cpi() {
        cpx(1);
    }

    private void cpd() {
        cpx(-1);
    }

    private void cpx(final int d) {
        final int ul = A & 0xFF, ur = peekHL() & 0xFF;
        final int result = ul - ur;
        final int carry = ul ^ ur ^ result;

        HL((short) (HL() + d));
        BC((short) (BC() - 1));

        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        if ((result & 0xFF) == 0) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= carry & FLAG_MASK_H;
        if (BC() != 0) f |= FLAG_MASK_PV;
        F = f;

        cycleBudget -= 5;
    }

    private void ini() {
        inx(1);
    }

    private void ind() {
        inx(-1);
    }

    private void inx(final int d) {
        pokeHL(this::ioRead);
        HL((short) (HL() + d));
        B--;

        cycleBudget -= 1;

        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        if (B == 0) f |= FLAG_MASK_Z;
        F = f;
    }

    private void outi() {
        outx(1);
    }

    private void outd() {
        outx(-1);
    }

    private void outx(final int d) {
        ioWrite(BC(), peekHL());
        HL((short) (HL() + d));
        B--;

        cycleBudget -= 1;

        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        if (B == 0) f |= FLAG_MASK_Z;
        F = f;
    }

    private void ldir() {
        ldxr(1);
    }

    private void lddr() {
        ldxr(-1);
    }

    private void ldxr(final int d) {
        poke8(DE(), peekHL());
        DE((short) (DE() + d));
        HL((short) (HL() + d));
        BC((short) (BC() - 1));

        cycleBudget -= 2;

        F &= FLAG_MASK_SZC;

        if (BC() != 0) {
            PC -= 2;
            cycleBudget -= 5;
        }
    }

    private void cpir() {
        cpxr(1);
    }

    private void cpdr() {
        cpxr(-1);
    }

    private void cpxr(final int d) {
        final int ul = A & 0xFF, ur = peekHL() & 0xFF;
        final int result = ul - ur;
        final int carry = ul ^ ur ^ result;

        HL((short) (HL() + d));
        BC((short) (BC() - 1));

        final boolean bcNonZero = BC() != 0;
        final boolean areEqual = (result & 0xFF) == 0;
        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        if (areEqual) f |= FLAG_MASK_Z;
        else f |= result & FLAG_MASK_S;
        f |= carry & FLAG_MASK_H;
        if (bcNonZero) f |= FLAG_MASK_PV;
        F = f;

        cycleBudget -= 5;

        if (bcNonZero && !areEqual) {
            PC -= 2;
            cycleBudget -= 5;
        }
    }

    private void inir() {
        inxr(1);
    }

    private void indr() {
        inxr(-1);
    }

    private void inxr(final int d) {
        pokeHL(this::ioRead);
        HL((short) (HL() + d));
        B--;

        cycleBudget -= 1;

        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        if (B == 0) f |= FLAG_MASK_Z;
        F = f;

        if (B != 0) {
            PC -= 2;
            cycleBudget -= 5;
        }
    }

    private void otir() {
        otxr(1);
    }

    private void otdr() {
        otxr(-1);
    }

    private void otxr(final int d) {
        ioWrite(BC(), peekHL());
        HL((short) (HL() + d));
        B--;

        cycleBudget -= 1;

        byte f = (byte) (FLAG_MASK_N | (F & FLAG_MASK_C));
        if (B == 0) f |= FLAG_MASK_Z;
        F = f;

        if (B != 0) {
            PC -= 2;
            cycleBudget -= 5;
        }
    }

    // --------------------------------------------------------------------- //

    private void execute(byte opcode) {
        RegisterAccess r;
        switch (status) {
            case PARSING_DD:
                r = registersDD;
                break;
            case PARSING_FD:
                r = registersFD;
                break;
            default:
                r = registers;
                break;
        }

        for (; ; ) {
            status = Status.RUNNING;

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
                                    return;
                                case 1: { // EX AF,AF'
                                    {
                                        final byte t = A;
                                        A = A2;
                                        A2 = t;
                                    }
                                    {
                                        final byte t = F;
                                        F = F2;
                                        F2 = t;
                                    }
                                    return;
                                }
                                case 2: { // DJNZ e
                                    final byte e = read8();
                                    cycleBudget -= 1;
                                    if (--B != 0) {
                                        PC += e;
                                        cycleBudget -= 5;
                                    }
                                    return;
                                }
                                case 3: // JR e
                                    PC += read8();
                                    cycleBudget -= 5;
                                    return;
                                case 4:
                                case 5:
                                case 6:
                                case 7: { // JR cc[y-4],e
                                    final byte e = read8();
                                    if (cc[y - 4].apply()) {
                                        PC += e;
                                        cycleBudget -= 5;
                                    }
                                    return;
                                }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 1: // 16-bit load immediate/add
                            switch (q) {
                                case 0: // LD rp[p],nn
                                    r.w16[p].apply(read16());
                                    return;
                                case 1: // ADD HL,rp[p]
                                    r.w16[IDX_HL_IX_IY].apply(add16(r.r16[IDX_HL_IX_IY].apply(), r.r16[p].apply()));
                                    cycleBudget -= 7;
                                    return;
                                default:
                                    throw new IllegalStateException();
                            }
                        case 2: // Indirect loading
                            switch (q) {
                                case 0:
                                    switch (p) {
                                        case 0: // LD (BC),A
                                            poke8(BC(), A);
                                            return;
                                        case 1: // LD (DE),A
                                            poke8(DE(), A);
                                            return;
                                        case 2: // LD (nn),HL
                                            poke16(read16(), r.r16[IDX_HL_IX_IY].apply());
                                            return;
                                        case 3: // LD (nn),A
                                            poke8(read16(), A);
                                            return;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                case 1:
                                    switch (p) {
                                        case 0: // LD A,(BC)
                                            A = peek8(BC());
                                            return;
                                        case 1: // LD A,(DE)
                                            A = peek8(DE());
                                            return;
                                        case 2: // LD HL,(nn)
                                            r.w16[IDX_HL_IX_IY].apply(peek16(read16()));
                                            return;
                                        case 3: // LD A,(nn)
                                            A = peek8(read16());
                                            return;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 3: // 16-bit INC/DEC
                            switch (q) {
                                case 0: // INC rp[p]
                                    r.w16[p].apply((short) (r.r16[p].apply() + 1));
                                    cycleBudget -= 2;
                                    return;
                                case 1: // DEC rp[p]
                                    r.w16[p].apply((short) (r.r16[p].apply() - 1));
                                    cycleBudget -= 2;
                                    return;
                                default:
                                    throw new IllegalStateException();
                            }
                        case 4: // 8-bit INC: INC r[y]
                            r.rw8[y].apply(this::inc);
                            return;
                        case 5: // 8-bit DEC: DEC r[y]
                            r.rw8[y].apply(this::dec);
                            return;
                        case 6: // 8-bit load immediate: LD r[y],n
                            r.w8[y].apply(this::read8);
                            return;
                        case 7: // Assorted operations or accumulator/flags
                            switch (y) {
                                case 0: { // RLCA
                                    final int ua = A & 0xFF;
                                    final byte carry = (byte) (ua >>> 7);
                                    A = (byte) ((ua << 1) | carry);
                                    F = (byte) ((F & FLAG_MASK_SZPV) | carry);
                                    return;
                                }
                                case 1: { // RRCA
                                    final int ua = A & 0xFF;
                                    final byte carry = (byte) (ua & 1);
                                    A = (byte) ((ua >>> 1) | (carry << 7));
                                    F = (byte) ((F & FLAG_MASK_SZPV) | carry);
                                    return;
                                }
                                case 2: { // RLA
                                    final int ua = A & 0xFF;
                                    final byte carry = (byte) (ua >>> 7);
                                    A = (byte) ((ua << 1) | (F & FLAG_MASK_C));
                                    F = (byte) ((F & FLAG_MASK_SZPV) | carry);
                                    return;
                                }
                                case 3: { // RRA
                                    final int ua = A & 0xFF;
                                    final byte carry = (byte) (ua & 1);
                                    A = (byte) ((ua >>> 1) | ((F & FLAG_MASK_C) << 7));
                                    F = (byte) ((F & FLAG_MASK_SZPV) | carry);
                                    return;
                                }
                                case 4: // DAA
                                    daa();
                                    return;
                                case 5: // CPL
                                    A = (byte) ~A;
                                    F |= FLAG_MASK_H | FLAG_MASK_N;
                                    return;
                                case 6: // SCF
                                    F &= ~FLAG_MASK_H & ~FLAG_MASK_N;
                                    F |= FLAG_MASK_C;
                                    return;
                                case 7: { // CCF
                                    final int carry = F & FLAG_MASK_C;
                                    F &= FLAG_MASK_SZPV;
                                    F |= carry << FLAG_SHIFT_H;
                                    F |= carry ^ FLAG_MASK_C;
                                    return;
                                }
                                default:
                                    throw new IllegalStateException();
                            }
                        default:
                            throw new IllegalStateException();
                    }
                case 1:
                    if (z == 6 && y == 6) { // Exception (replaces LD (HL),(HL)): HALT
                        status = Status.HALTED;
                        PC -= 1;
                    } else if (y == 6) { // 8-bit loading: LD (HL),r[z]
                        r.w8[y].apply(registers.r8[z]);
                    } else if (z == 6) { // 8-bit loading: LD r[y],(HL)
                        registers.w8[y].apply(r.r8[z]);
                    } else { // 8-bit loading: LD r[y],r[z]
                        r.w8[y].apply(r.r8[z]);
                    }
                    return;
                case 2: // Operator on accumulator and register/memory location: alu[y] r[z]
                    alu[y].apply(r.r8[z].apply());
                    return;
                case 3:
                    switch (z) {
                        case 0: // Conditional return: RET cc[y]
                            if (cc[y].apply()) {
                                PC = pop();
                            }
                            cycleBudget -= 1;
                            return;
                        case 1: // POP & various ops
                            switch (q) {
                                case 0: // POP rp2[p]
                                    r.w216[p].apply(pop());
                                    return;
                                case 1:
                                    switch (p) {
                                        case 0: // RET
                                            PC = pop();
                                            return;
                                        case 1: { // EXX
                                            {
                                                final byte t = B;
                                                B = B2;
                                                B2 = t;
                                            }
                                            {
                                                final byte t = C;
                                                C = C2;
                                                C2 = t;
                                            }
                                            {
                                                final byte t = D;
                                                D = D2;
                                                D2 = t;
                                            }
                                            {
                                                final byte t = E;
                                                E = E2;
                                                E2 = t;
                                            }
                                            {
                                                final byte t = H;
                                                H = H2;
                                                H2 = t;
                                            }
                                            {
                                                final byte t = L;
                                                L = L2;
                                                L2 = t;
                                            }
                                            return;
                                        }
                                        case 2: // JP (HL)
                                            PC = r.r16[IDX_HL_IX_IY].apply();
                                            return;
                                        case 3: // LD SP,HL
                                            SP = r.r16[IDX_HL_IX_IY].apply();
                                            cycleBudget -= 2;
                                            return;
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
                            return;
                        }
                        case 3: // Assorted operations
                            switch (y) {
                                case 0: // JP nn
                                    PC = read16();
                                    return;
                                case 1: { // (CB prefix)
                                    if (r == registersDD) {
                                        PC += 1;
                                        r = registersDDCB;
                                    } else if (r == registersFD) {
                                        PC += 1;
                                        r = registersFDCB;
                                    }

                                    opcode = read8();
                                    cycleBudget -= 1;
                                    x = (opcode & 0b11000000) >>> 6;
                                    y = (opcode & 0b00111000) >>> 3;
                                    z = (opcode & 0b00000111);

                                    switch (x) {
                                        case 0: // Roll/shift register or memory location: rot[y] r[z]
                                            r.rw8[z].apply(rot[y]);
                                            return;
                                        case 1: // Test bit: BIT y,r[z]
                                            bit(y, r.r8[z].apply());
                                            return;
                                        case 2: // Reset bit: RES y,r[z]
                                            r.rw8[z].apply(res[y]);
                                            return;
                                        case 3: // Set bit: SET y,r[z]
                                            r.rw8[z].apply(set[y]);
                                            return;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                }
                                case 2: // OUT (n),A
                                    ioWrite((short) (read8() & 0xFF), A);
                                    return;
                                case 3: // IN A,(n)
                                    A = ioRead((short) (read8() & 0xFF));
                                    return;
                                case 4: { // EX (SP), HL
                                    final short t = r.r16[IDX_HL_IX_IY].apply();
                                    r.w16[IDX_HL_IX_IY].apply(peek16(SP));
                                    poke16(SP, t);
                                    cycleBudget -= 3;
                                    return;
                                }
                                case 5: { // EX DE,HL
                                    final short t = DE();
                                    DE(HL());
                                    HL(t);
                                    return;
                                }
                                case 6: // DI
                                    IFF1 = IFF2 = false;
                                    return;
                                case 7: // EI
                                    IFF1 = IFF2 = true;
                                    return;
                                default:
                                    throw new IllegalStateException();
                            }
                        case 4: { // Conditional call: CALL cc[y],nn
                            final short nn = read16();
                            if (cc[y].apply()) {
                                push(PC);
                                PC = nn;
                                cycleBudget -= 1;
                            }
                            return;
                        }
                        case 5: // PUSH & various ops
                            switch (q) {
                                case 0: // PUSH rp2[p]
                                    push(r.r216[p].apply());
                                    cycleBudget -= 1;
                                    return;
                                case 1:
                                    switch (p) {
                                        case 0: { // CALL nn
                                            final short nn = read16();
                                            push(PC);
                                            PC = nn;
                                            cycleBudget -= 1;
                                            return;
                                        }
                                        case 1: { // (DD prefix)
                                            status = Status.PARSING_DD;
                                            if (cycleBudget <= 0) return;
                                            opcode = read8();
                                            cycleBudget -= 1;
                                            r = registersDD;
                                            continue;
                                        }
                                        case 2: { // (ED prefix)
                                            r = registers;
                                            opcode = read8();
                                            cycleBudget -= 1;
                                            x = (opcode & 0b11000000) >>> 6;
                                            y = (opcode & 0b00111000) >>> 3;
                                            z = (opcode & 0b00000111);
                                            p = y >>> 1;
                                            q = y & 1;

                                            switch (x) {
                                                case 1:
                                                    switch (z) {
                                                        case 0: { // Input from port with 16-bit address
                                                            final byte value;
                                                            if (y != 6) { // IN r[y],(C)
                                                                r.w8[y].apply(this::ioRead);
                                                                value = r.r8[y].apply();
                                                            } else { // IN (C)
                                                                ioRead();
                                                                value = 0;
                                                            }

                                                            byte f = (byte) (F & FLAG_MASK_C);
                                                            if ((value & 0xFF) == 0) f |= FLAG_MASK_Z;
                                                            else f |= value & FLAG_MASK_S;
                                                            f |= computeParity(value) << FLAG_SHIFT_PV;
                                                            F = f;
                                                            return;
                                                        }
                                                        case 1: // Output to port with 16-bit address
                                                            if (y != 6) { // OUT(C),r[y]
                                                                ioWrite(BC(), r.r8[y].apply());
                                                            } else { // OUT(C),0
                                                                ioWrite(BC(), (byte) 0);
                                                            }
                                                            return;
                                                        case 2: // 16-bit add/subtract with carry
                                                            switch (q) {
                                                                case 0: // SBC HL,rp[p]
                                                                    HL(sbc16(HL(), r.r16[p].apply()));
                                                                    cycleBudget -= 7;
                                                                    return;
                                                                case 1: // ADC HL,rp[p]
                                                                    HL(adc16(HL(), r.r16[p].apply()));
                                                                    cycleBudget -= 7;
                                                                    return;
                                                                default:
                                                                    throw new IllegalStateException();
                                                            }
                                                        case 3: // Retrieve/store register pair from/to immediate address
                                                            switch (q) {
                                                                case 0: // LD (nn),rp[p]
                                                                    poke16(read16(), r.r16[p].apply());
                                                                    return;
                                                                case 1: // LD rp[p],(nn)
                                                                    r.w16[p].apply(peek16(read16()));
                                                                    return;
                                                                default:
                                                                    throw new IllegalStateException();
                                                            }
                                                        case 4: // Negate accumulator
                                                            neg();
                                                            return;
                                                        case 5: // Return from interrupt
                                                            switch (y) {
                                                                case 1: // RETI
                                                                    PC = pop();
                                                                    return;
                                                                default: // RETN
                                                                    PC = pop();
                                                                    IFF1 = IFF2;
                                                                    return;
                                                            }
                                                        case 6: // Set interrupt mode: IM im[y]
                                                            IM = im[y];
                                                            return;
                                                        case 7: // Assorted ops
                                                            switch (y) {
                                                                case 0: // LD I,A
                                                                    I = A;
                                                                    cycleBudget -= 1;
                                                                    return;
                                                                case 1: // LD R,A
                                                                    R = A;
                                                                    cycleBudget -= 1;
                                                                    return;
                                                                case 2: // LD A,I
                                                                case 3: { // LD A,R
                                                                    if (y == 2) A = I;
                                                                    else A = R;
                                                                    cycleBudget -= 1;

                                                                    byte f = (byte) (F & FLAG_MASK_C);
                                                                    if ((A & 0xFF) == 0) f |= FLAG_MASK_Z;
                                                                    else f |= A & FLAG_MASK_S;
                                                                    if (IFF2) f |= FLAG_MASK_PV;
                                                                    F = f;
                                                                    return;
                                                                }
                                                                case 4: // RRD
                                                                case 5: { // RLD
                                                                    final int uhl = peekHL() & 0xFF, ua = A & 0xFF; // 0bHHHHLLLL, 0bAAAAaaaa
                                                                    final int uahl;
                                                                    if (y == 4) {
                                                                        uahl = ((ua & 0xF0) << 8) | ((uhl & 0x0F) << 8) | ((A & 0x0F) << 4) | (uhl >> 4); // 0bAAAALLLLaaaaHHHH
                                                                    } else {
                                                                        uahl = ((ua & 0xF0) << 8) | (uhl << 4) | (ua & 0x0F); // 0bAAAAHHHHLLLLaaaa
                                                                    }
                                                                    cycleBudget -= 4;

                                                                    poke8(HL(), (byte) uahl);
                                                                    A = (byte) (uahl >>> 8);

                                                                    byte f = (byte) (F & FLAG_MASK_C);
                                                                    if ((A & 0xFF) == 0) f |= FLAG_MASK_Z;
                                                                    else f |= A & FLAG_MASK_S;
                                                                    f |= computeParity(A) << FLAG_SHIFT_PV;
                                                                    F = f;
                                                                    return;
                                                                }
                                                                case 6:
                                                                case 7: // NOP
                                                                    return;
                                                                default:
                                                                    throw new IllegalStateException();
                                                            }
                                                        default:
                                                            throw new IllegalStateException();
                                                    }
                                                case 2:
                                                    if (z <= 3) { // Block instruction: bli[y,z]
                                                        if (y > 3) {
                                                            bli[y - 4][z].apply();
                                                        }
                                                    } // else: NOP
                                                    return;
                                                case 0:
                                                case 3: // NOP
                                                    return;
                                                default:
                                                    throw new IllegalStateException();
                                            }
                                        }
                                        case 3: // (FD prefix)
                                            status = Status.PARSING_FD;
                                            if (cycleBudget <= 0) return;
                                            opcode = read8();
                                            cycleBudget -= 1;
                                            r = registersFD;
                                            continue;
                                        default:
                                            throw new IllegalStateException();
                                    }
                                default:
                                    throw new IllegalStateException();
                            }
                        case 6: // Operate on accumulator and immediate operand: alu[y] n
                            alu[y].apply(read8());
                            return;
                        case 7: // Restart: RST y*8
                            push(PC);
                            PC = (short) (y * 8);
                            cycleBudget -= 1;
                            return;
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

    // Index of HL/IX/IY in 16-bit access tables.
    private static final int IDX_HL_IX_IY = 2;

    // Standard register access.
    private final RegisterAccess registers = new RegisterAccess(
            new ReadAccess8[]{this::B, this::C, this::D, this::E, this::H, this::L, this::peekHL, this::A},
            new IndirectWriteAccess8[]{this::B, this::C, this::D, this::E, this::H, this::L, this::pokeHL, this::A},
            new ReadWriteAccess8[]{this::B, this::C, this::D, this::E, this::H, this::L, this::indirectHL, this::A},
            new ReadAccess16[]{this::BC, this::DE, this::HL, this::SP},
            new WriteAccess16[]{this::BC, this::DE, this::HL, this::SP},
            new ReadAccess16[]{this::BC, this::DE, this::HL, this::AF},
            new WriteAccess16[]{this::BC, this::DE, this::HL, this::AF});

    // DD-prefixed register access.
    private final RegisterAccess registersDD = new RegisterAccess(
            withReplacements8(ReadAccess8.class, registers.r8, this::IXH, this::IXL, this::peekIXd),
            withReplacements8(IndirectWriteAccess8.class, registers.w8, this::IXH, this::IXL, this::pokeIXd),
            withReplacements8(ReadWriteAccess8.class, registers.rw8, this::IXH, this::IXL, this::indirectIXd),
            withReplacements16(ReadAccess16.class, registers.r16, this::IX),
            withReplacements16(WriteAccess16.class, registers.w16, this::IX),
            withReplacements16(ReadAccess16.class, registers.r216, this::IX),
            withReplacements16(WriteAccess16.class, registers.w216, this::IX));

    // FD-prefixed register access.
    private final RegisterAccess registersFD = new RegisterAccess(
            withReplacements8(ReadAccess8.class, registers.r8, this::IYH, this::IYL, this::peekIYd),
            withReplacements8(IndirectWriteAccess8.class, registers.w8, this::IYH, this::IYL, this::pokeIYd),
            withReplacements8(ReadWriteAccess8.class, registers.rw8, this::IYH, this::IYL, this::indirectIYd),
            withReplacements16(ReadAccess16.class, registers.r16, this::IY),
            withReplacements16(WriteAccess16.class, registers.w16, this::IY),
            withReplacements16(ReadAccess16.class, registers.r216, this::IY),
            withReplacements16(WriteAccess16.class, registers.w216, this::IY));

    // DDCB-prefixed register access.
    private final RegisterAccess registersDDCB = new RegisterAccess(
            withReplacements8(ReadAccess8.class, registersDD.r8, this::peekIXdCB),
            withReplacements8(IndirectWriteAccess8.class, registersDD.w8, this::pokeIXdCB),
            withReplacements8(ReadWriteAccess8.class, registersDD.rw8, this::indirectIXdCB),
            registersDD.r16,
            registersDD.w16,
            registersDD.r216,
            registersDD.w216);

    // FDCB-prefixed register access.
    private final RegisterAccess registersFDCB = new RegisterAccess(
            withReplacements8(ReadAccess8.class, registersFD.r8, this::peekIYdCB),
            withReplacements8(IndirectWriteAccess8.class, registersFD.w8, this::pokeIYdCB),
            withReplacements8(ReadWriteAccess8.class, registersFD.rw8, this::indirectIYdCB),
            registersFD.r16,
            registersFD.w16,
            registersFD.r216,
            registersFD.w216);

    // Conditions
    private final Condition[] cc = {this::FLAG_NZ, this::FLAG_Z, this::FLAG_NC, this::FLAG_C, this::FLAG_PO, this::FLAG_PE, this::FLAG_P, this::FLAG_M};
    // Arithmetic/logic operations
    private final WriteAccess8[] alu = {this::add, this::adc, this::sub, this::sbc, this::and, this::xor, this::or, this::cp};
    // Rotation/shift operations
    private final Transform[] rot = {this::rlc, this::rrc, this::rl, this::rr, this::sla, this::sra, this::sll, this::srl};
    // Bit reset
    private final Transform[] res = new Transform[8];
    // Bit set
    private final Transform[] set = new Transform[8];
    // Interrupt modes
    private final InterruptMode[] im = {InterruptMode.MODE_0, InterruptMode.MODE_0, InterruptMode.MODE_1, InterruptMode.MODE_2, InterruptMode.MODE_0, InterruptMode.MODE_0, InterruptMode.MODE_1, InterruptMode.MODE_2};
    // Block instructions
    private final Action[][] bli = {
            {this::ldi, this::cpi, this::ini, this::outi},
            {this::ldd, this::cpd, this::ind, this::outd},
            {this::ldir, this::cpir, this::inir, this::otir},
            {this::lddr, this::cpdr, this::indr, this::otdr}
    };

    // There is an overflow if the xor of the carry out and the carry of the
    // most significant bit is not zero.
    private static final int[] OVERFLOW_TABLE = {0, FLAG_MASK_PV, FLAG_MASK_PV, 0};

    // --------------------------------------------------------------------- //
    // Helpers for constructing register access tables reusing lambdas.

    @SuppressWarnings("unchecked")
    private static <T> T[] withReplacements8(final Class<T> clazz, final T[] source, final T h, final T l, final T indirectHL) {
        final T[] result = (T[]) Array.newInstance(clazz, source.length);
        System.arraycopy(source, 0, result, 0, source.length);
        result[4] = h;
        result[5] = l;
        result[6] = indirectHL;
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] withReplacements8(final Class<T> clazz, final T[] source, final T indirectHL) {
        final T[] result = (T[]) Array.newInstance(clazz, source.length);
        System.arraycopy(source, 0, result, 0, source.length);
        result[6] = indirectHL;
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] withReplacements16(final Class<T> clazz, final T[] source, final T hl) {
        final T[] result = (T[]) Array.newInstance(clazz, source.length);
        System.arraycopy(source, 0, result, 0, source.length);
        result[2] = hl;
        return result;
    }

    // --------------------------------------------------------------------- //
    // Functional interfaces for lookup tables.

    @FunctionalInterface
    private interface ReadAccess8 {
        byte apply();
    }

    @FunctionalInterface
    private interface ReadAccess16 {
        short apply();
    }

    @FunctionalInterface
    private interface WriteAccess8 {
        void apply(final byte value);
    }

    @FunctionalInterface
    private interface IndirectWriteAccess8 {
        void apply(final ReadAccess8 value);
    }

    @FunctionalInterface
    private interface WriteAccess16 {
        void apply(final short value);
    }

    @FunctionalInterface
    private interface ReadWriteAccess8 {
        void apply(final Transform transform);
    }

    @FunctionalInterface
    private interface Condition {
        boolean apply();
    }

    @FunctionalInterface
    private interface Transform {
        byte apply(final byte value);
    }

    @FunctionalInterface // Yes, there's Runnable, but I don't like using it for non-threaded use-cases.
    private interface Action {
        void apply();
    }

    // --------------------------------------------------------------------- //

    /**
     * Indirection layer for accessing registers. This is used to allow a more
     * generic implementation that handles both regular opcodes as well as DD
     * and FD prefixed opcodes.
     */
    private static final class RegisterAccess {
        final ReadAccess8[] r8; // 8-bit registers (read)
        final IndirectWriteAccess8[] w8; // 8-bit registers (write)
        final ReadWriteAccess8[] rw8; // 8-bit registers (read-write)
        final ReadAccess16[] r16; // Register pairs featuring SP (read)
        final WriteAccess16[] w16; // Register pairs featuring SP (write)
        final ReadAccess16[] r216; // Register pairs featuring AF (read)
        final WriteAccess16[] w216; // Register pairs featuring AF (write)

        private RegisterAccess(final ReadAccess8[] r8, final IndirectWriteAccess8[] w8, final ReadWriteAccess8[] rw8, final ReadAccess16[] r16, final WriteAccess16[] w16, final ReadAccess16[] r216, final WriteAccess16[] w216) {
            this.r8 = r8;
            this.w8 = w8;
            this.rw8 = rw8;
            this.r16 = r16;
            this.w16 = w16;
            this.r216 = r216;
            this.w216 = w216;
        }
    }

    private enum Status {
        RUNNING,
        HALTED,
        PARSING_DD,
        PARSING_FD
    }

    private enum InterruptMode {
        MODE_0,
        MODE_1,
        MODE_2
    }
}
