package li.cil.circuity.server.processor.i8080;

import java.util.function.Consumer;

@SuppressWarnings("SillyAssignment")
final class I8080Instruction {
    static final I8080Instruction[] INSTRUCTIONS = new I8080Instruction[0x100];

    final int opCode;
    final String name;
    final int cycles;
    final Consumer<I8080> operation;

    public I8080Instruction(final int opCode, final String name, final int cycles, final Consumer<I8080> operation) {
        this.opCode = opCode;
        this.name = name;
        this.cycles = cycles;
        this.operation = operation;

        I8080Instruction.INSTRUCTIONS[opCode] = this;
    }

    @Override
    public String toString() {
        return String.format("%02X  %s", opCode, name);
    }

    static {
        // Misc / control

        new I8080Instruction(0x00, "NOP", 4,
                cpu -> {
                });

        new I8080Instruction(0x76, "HALT", 7,
                cpu -> {
                    cpu.halted = true;
                    cpu.PC = (cpu.PC - 1) & 0xFFFF;
                });

        new I8080Instruction(0xD3, "OUT (n),A", 10,
                cpu -> cpu.ioWrite(cpu.read8(), cpu.A));
        new I8080Instruction(0xDB, "IN A,(n)", 10,
                cpu -> cpu.A = cpu.ioRead(cpu.read8()));

        new I8080Instruction(0xF3, "DI", 4,
                cpu -> {
                    synchronized (cpu.interruptLock) {
                        cpu.irqEnabled = false;
                    }
                });
        new I8080Instruction(0xFB, "EI", 4,
                cpu -> {
                    synchronized (cpu.interruptLock) {
                        cpu.irqEnabled = true;
                    }
                });

        // Jumps / calls

        new I8080Instruction(0xC3, "JP (nn)", 10,
                cpu -> cpu.jp(true));
        new I8080Instruction(0xE9, "JP (HL)", 4,
                cpu -> cpu.jp(true, cpu.HL()));

        new I8080Instruction(0xCA, "JP Z,(nn)", 10,
                cpu -> cpu.jp(cpu.Z));
        new I8080Instruction(0xC2, "JP NZ,(nn)", 10,
                cpu -> cpu.jp(!cpu.Z));
        new I8080Instruction(0xDA, "JP C,(nn)", 10,
                cpu -> cpu.jp(cpu.CY));
        new I8080Instruction(0xD2, "JP NC,(nn)", 10,
                cpu -> cpu.jp(!cpu.CY));
        new I8080Instruction(0xEA, "JP PE,(nn)", 10,
                cpu -> cpu.jp(cpu.P));
        new I8080Instruction(0xE2, "JP PO,(nn)", 10,
                cpu -> cpu.jp(!cpu.P));
        new I8080Instruction(0xFA, "JP M,(nn)", 10,
                cpu -> cpu.jp(cpu.S));
        new I8080Instruction(0xF2, "JP P,(nn)", 10,
                cpu -> cpu.jp(!cpu.S));

        new I8080Instruction(0xCD, "CALL (nn)", 10,
                cpu -> cpu.call(true));

        new I8080Instruction(0xCC, "CALL Z,(nn)", 10,
                cpu -> cpu.call(cpu.Z));
        new I8080Instruction(0xC4, "CALL NZ,(nn)", 10,
                cpu -> cpu.call(!cpu.Z));
        new I8080Instruction(0xDC, "CALL C,(nn)", 10,
                cpu -> cpu.call(cpu.CY));
        new I8080Instruction(0xD4, "CALL NC,(nn)", 10,
                cpu -> cpu.call(!cpu.CY));
        new I8080Instruction(0xEC, "CALL PE,(nn)", 10,
                cpu -> cpu.call(cpu.P));
        new I8080Instruction(0xE4, "CALL PO,(nn)", 10,
                cpu -> cpu.call(!cpu.P));
        new I8080Instruction(0xFC, "CALL M,(nn)", 10,
                cpu -> cpu.call(cpu.S));
        new I8080Instruction(0xF4, "CALL P,(nn)", 10,
                cpu -> cpu.call(!cpu.S));

        new I8080Instruction(0xC9, "RET", 4,
                cpu -> cpu.ret(true));

        new I8080Instruction(0xC8, "RET Z", 5,
                cpu -> cpu.ret(cpu.Z));
        new I8080Instruction(0xC0, "RET NZ", 5,
                cpu -> cpu.ret(!cpu.Z));
        new I8080Instruction(0xD8, "RET C", 5,
                cpu -> cpu.ret(cpu.CY));
        new I8080Instruction(0xD0, "RET NC", 5,
                cpu -> cpu.ret(!cpu.CY));
        new I8080Instruction(0xE8, "RET PE", 5,
                cpu -> cpu.ret(cpu.P));
        new I8080Instruction(0xE0, "RET PO", 5,
                cpu -> cpu.ret(!cpu.P));
        new I8080Instruction(0xF8, "RET M", 5,
                cpu -> cpu.ret(cpu.S));
        new I8080Instruction(0xF0, "RET P", 5,
                cpu -> cpu.ret(!cpu.S));

        new I8080Instruction(0xC7, "RST 0H", 11,
                cpu -> cpu.rst(0x00));
        new I8080Instruction(0xCF, "RST 8H", 11,
                cpu -> cpu.rst(0x08));
        new I8080Instruction(0xD7, "RST 10H", 11,
                cpu -> cpu.rst(0x10));
        new I8080Instruction(0xDF, "RST 18H", 11,
                cpu -> cpu.rst(0x18));
        new I8080Instruction(0xE7, "RST 20H", 11,
                cpu -> cpu.rst(0x20));
        new I8080Instruction(0xEF, "RST 28H", 11,
                cpu -> cpu.rst(0x28));
        new I8080Instruction(0xF7, "RST 30H", 11,
                cpu -> cpu.rst(0x30));
        new I8080Instruction(0xFF, "RST 38H", 11,
                cpu -> cpu.rst(0x38));

        // Data Transfer Group

        // MOV r1,r2 (Move Register)

        // TODO Test single function for all loads using b01dddsss pattern. Needs benchmarking - better to avoid logic for address extraction or better to have fewer functions to be more cache friendly.

        new I8080Instruction(0x40, "LD B,B", 5,
                cpu -> cpu.B = cpu.B);
        new I8080Instruction(0x41, "LD B,C", 5,
                cpu -> cpu.B = cpu.C);
        new I8080Instruction(0x42, "LD B,D", 5,
                cpu -> cpu.B = cpu.D);
        new I8080Instruction(0x43, "LD B,E", 5,
                cpu -> cpu.B = cpu.E);
        new I8080Instruction(0x44, "LD B,H", 5,
                cpu -> cpu.B = cpu.H);
        new I8080Instruction(0x45, "LD B,L", 5,
                cpu -> cpu.B = cpu.L);
        new I8080Instruction(0x47, "LD B,A", 5,
                cpu -> cpu.B = cpu.A);

        new I8080Instruction(0x48, "LD C,B", 5,
                cpu -> cpu.C = cpu.B);
        new I8080Instruction(0x49, "LD C,C", 5,
                cpu -> cpu.C = cpu.C);
        new I8080Instruction(0x4A, "LD C,D", 5,
                cpu -> cpu.C = cpu.D);
        new I8080Instruction(0x4B, "LD C,E", 5,
                cpu -> cpu.C = cpu.E);
        new I8080Instruction(0x4C, "LD C,H", 5,
                cpu -> cpu.C = cpu.H);
        new I8080Instruction(0x4D, "LD C,L", 5,
                cpu -> cpu.C = cpu.L);
        new I8080Instruction(0x4F, "LD C,A", 5,
                cpu -> cpu.C = cpu.A);

        new I8080Instruction(0x50, "LD D,B", 5,
                cpu -> cpu.D = cpu.B);
        new I8080Instruction(0x51, "LD D,C", 5,
                cpu -> cpu.D = cpu.C);
        new I8080Instruction(0x52, "LD D,D", 5,
                cpu -> cpu.D = cpu.D);
        new I8080Instruction(0x53, "LD D,E", 5,
                cpu -> cpu.D = cpu.E);
        new I8080Instruction(0x54, "LD D,H", 5,
                cpu -> cpu.D = cpu.H);
        new I8080Instruction(0x55, "LD D,L", 5,
                cpu -> cpu.D = cpu.L);
        new I8080Instruction(0x57, "LD D,A", 5,
                cpu -> cpu.D = cpu.A);

        new I8080Instruction(0x58, "LD E,B", 5,
                cpu -> cpu.E = cpu.B);
        new I8080Instruction(0x59, "LD E,C", 5,
                cpu -> cpu.E = cpu.C);
        new I8080Instruction(0x5A, "LD E,D", 5,
                cpu -> cpu.E = cpu.D);
        new I8080Instruction(0x5B, "LD E,E", 5,
                cpu -> cpu.E = cpu.E);
        new I8080Instruction(0x5C, "LD E,H", 5,
                cpu -> cpu.E = cpu.H);
        new I8080Instruction(0x5D, "LD E,L", 5,
                cpu -> cpu.E = cpu.L);
        new I8080Instruction(0x5F, "LD E,A", 5,
                cpu -> cpu.E = cpu.A);

        new I8080Instruction(0x60, "LD H,B", 5,
                cpu -> cpu.H = cpu.B);
        new I8080Instruction(0x61, "LD H,C", 5,
                cpu -> cpu.H = cpu.C);
        new I8080Instruction(0x62, "LD H,D", 5,
                cpu -> cpu.H = cpu.D);
        new I8080Instruction(0x63, "LD H,E", 5,
                cpu -> cpu.H = cpu.E);
        new I8080Instruction(0x64, "LD H,H", 5,
                cpu -> cpu.H = cpu.H);
        new I8080Instruction(0x65, "LD H,L", 5,
                cpu -> cpu.H = cpu.L);
        new I8080Instruction(0x67, "LD H,A", 5,
                cpu -> cpu.H = cpu.A);

        new I8080Instruction(0x68, "LD L,B", 5,
                cpu -> cpu.L = cpu.B);
        new I8080Instruction(0x69, "LD L,C", 5,
                cpu -> cpu.L = cpu.C);
        new I8080Instruction(0x6A, "LD L,D", 5,
                cpu -> cpu.L = cpu.D);
        new I8080Instruction(0x6B, "LD L,E", 5,
                cpu -> cpu.L = cpu.E);
        new I8080Instruction(0x6C, "LD L,H", 5,
                cpu -> cpu.L = cpu.H);
        new I8080Instruction(0x6D, "LD L,L", 5,
                cpu -> cpu.L = cpu.L);
        new I8080Instruction(0x6F, "LD L,A", 5,
                cpu -> cpu.L = cpu.A);

        new I8080Instruction(0x78, "LD A,B", 5,
                cpu -> cpu.A = cpu.B);
        new I8080Instruction(0x79, "LD A,C", 5,
                cpu -> cpu.A = cpu.C);
        new I8080Instruction(0x7A, "LD A,D", 5,
                cpu -> cpu.A = cpu.D);
        new I8080Instruction(0x7B, "LD A,E", 5,
                cpu -> cpu.A = cpu.E);
        new I8080Instruction(0x7C, "LD A,H", 5,
                cpu -> cpu.A = cpu.H);
        new I8080Instruction(0x7D, "LD A,L", 5,
                cpu -> cpu.A = cpu.L);
        new I8080Instruction(0x7F, "LD A,A", 5,
                cpu -> cpu.A = cpu.A);

        // MOV r,M (Move from memory)

        new I8080Instruction(0x46, "LD B,(HL)", 7,
                cpu -> cpu.B = cpu.peekHL());
        new I8080Instruction(0x4E, "LD C,(HL)", 7,
                cpu -> cpu.C = cpu.peekHL());
        new I8080Instruction(0x56, "LD D,(HL)", 7,
                cpu -> cpu.D = cpu.peekHL());
        new I8080Instruction(0x5E, "LD E,(HL)", 7,
                cpu -> cpu.E = cpu.peekHL());
        new I8080Instruction(0x66, "LD H,(HL)", 7,
                cpu -> cpu.H = cpu.peekHL());
        new I8080Instruction(0x6E, "LD L,(HL)", 7,
                cpu -> cpu.L = cpu.peekHL());
        new I8080Instruction(0x7E, "LD A,(HL)", 7,
                cpu -> cpu.A = cpu.peekHL());

        // MOV M,r (Move to memory)

        new I8080Instruction(0x70, "LD (HL),B", 7,
                cpu -> cpu.pokeHL(cpu.B));
        new I8080Instruction(0x71, "LD (HL),C", 7,
                cpu -> cpu.pokeHL(cpu.C));
        new I8080Instruction(0x72, "LD (HL),D", 7,
                cpu -> cpu.pokeHL(cpu.D));
        new I8080Instruction(0x73, "LD (HL),E", 7,
                cpu -> cpu.pokeHL(cpu.E));
        new I8080Instruction(0x74, "LD (HL),H", 7,
                cpu -> cpu.pokeHL(cpu.H));
        new I8080Instruction(0x75, "LD (HL),L", 7,
                cpu -> cpu.pokeHL(cpu.L));
        new I8080Instruction(0x77, "LD (HL),A", 7,
                cpu -> cpu.pokeHL(cpu.A));

        // MVI r,data (Move Immediate)

        new I8080Instruction(0x06, "LD B,n", 7,
                cpu -> cpu.B = cpu.read8());
        new I8080Instruction(0x0E, "LD C,n", 7,
                cpu -> cpu.C = cpu.read8());
        new I8080Instruction(0x16, "LD D,n", 7,
                cpu -> cpu.D = cpu.read8());
        new I8080Instruction(0x1E, "LD E,n", 7,
                cpu -> cpu.E = cpu.read8());
        new I8080Instruction(0x26, "LD H,n", 7,
                cpu -> cpu.H = cpu.read8());
        new I8080Instruction(0x2E, "LD L,n", 7,
                cpu -> cpu.L = cpu.read8());
        new I8080Instruction(0x3E, "LD A,n", 7,
                cpu -> cpu.A = cpu.read8());

        // MVI M,data (Move to memory immediate)

        new I8080Instruction(0x36, "LD (HL),n", 10,
                cpu -> cpu.pokeHL(cpu.read8()));

        // LXI rp,data 16 (Load register pair immediate)

        new I8080Instruction(0x01, "LD BC,nn", 10,
                cpu -> cpu.setBC(cpu.read16()));
        new I8080Instruction(0x11, "LD DE,nn", 10,
                cpu -> cpu.setDE(cpu.read16()));
        new I8080Instruction(0x21, "LD HL,nn", 10,
                cpu -> cpu.setHL(cpu.read16()));
        new I8080Instruction(0x31, "LD SP,nn", 10,
                cpu -> cpu.SP = cpu.read16());

        // LDA addr (Load Accumulator direct)

        new I8080Instruction(0x3A, "LD A,(nn)", 13,
                cpu -> cpu.A = cpu.peek8(cpu.read16()));

        // STA addr (Store Accumulator direct)

        new I8080Instruction(0x32, "LD (nn),A", 13,
                cpu -> cpu.poke8(cpu.read16(), cpu.A));

        // LHLD addr (Load H and L direct)

        new I8080Instruction(0x2A, "LD HL,(nn)", 16,
                cpu -> cpu.setHL(cpu.peek16(cpu.read16())));

        // SHLD addr (Store H and L direct)

        new I8080Instruction(0x22, "LD (nn),HL", 16,
                cpu -> cpu.poke16(cpu.read16(), cpu.HL()));

        // LDAX rp (Load accumulator indirect)

        new I8080Instruction(0x0A, "LD A,(BC)", 7,
                cpu -> cpu.A = cpu.peek8(cpu.BC()));
        new I8080Instruction(0x1A, "LD A,(DE)", 7,
                cpu -> cpu.A = cpu.peek8(cpu.DE()));

        // STAX rp (Store accumulator indirect)

        new I8080Instruction(0x02, "LD (BC),A", 7,
                cpu -> cpu.poke8(cpu.BC(), cpu.A));
        new I8080Instruction(0x12, "LD (DE),A", 7,
                cpu -> cpu.poke8(cpu.DE(), cpu.A));

        // XCHG (Exchange H and L with D and E)

        new I8080Instruction(0xEB, "EX DE,HL", 5,
                cpu -> {
                    final int t = cpu.DE();
                    cpu.setDE(cpu.HL());
                    cpu.setHL(t);
                });

        // 16 bit load / store / move

        new I8080Instruction(0xF9, "LD SP,HL", 5,
                cpu -> cpu.SP = cpu.HL());

        new I8080Instruction(0xE3, "EX (SP),HL", 18,
                cpu -> {
                    final int t = cpu.peek16(cpu.SP);
                    cpu.poke16(cpu.SP, cpu.HL());
                    cpu.setHL(t);
                });

        new I8080Instruction(0xC1, "POP BC", 10,
                cpu -> cpu.setBC(cpu.pop()));
        new I8080Instruction(0xD1, "POP DE", 10,
                cpu -> cpu.setDE(cpu.pop()));
        new I8080Instruction(0xE1, "POP HL", 10,
                cpu -> cpu.setHL(cpu.pop()));
        new I8080Instruction(0xF1, "POP PSW", 10,
                cpu -> cpu.setPSW(cpu.pop()));

        new I8080Instruction(0xC5, "PUSH BC", 11,
                cpu -> cpu.push(cpu.BC()));
        new I8080Instruction(0xD5, "PUSH DE", 11,
                cpu -> cpu.push(cpu.DE()));
        new I8080Instruction(0xE5, "PUSH HL", 11,
                cpu -> cpu.push(cpu.HL()));
        new I8080Instruction(0xF5, "PUSH PSW", 11,
                cpu -> cpu.push(cpu.PSW()));

        // Arithmetic Group

        // ADD r (Add Register)

        new I8080Instruction(0x80, "ADD B", 4,
                cpu -> cpu.A = cpu.add(cpu.B));
        new I8080Instruction(0x81, "ADD C", 4,
                cpu -> cpu.A = cpu.add(cpu.C));
        new I8080Instruction(0x82, "ADD D", 4,
                cpu -> cpu.A = cpu.add(cpu.D));
        new I8080Instruction(0x83, "ADD E", 4,
                cpu -> cpu.A = cpu.add(cpu.E));
        new I8080Instruction(0x84, "ADD H", 4,
                cpu -> cpu.A = cpu.add(cpu.H));
        new I8080Instruction(0x85, "ADD L", 4,
                cpu -> cpu.A = cpu.add(cpu.L));
        new I8080Instruction(0x87, "ADD A", 4,
                cpu -> cpu.A = cpu.add(cpu.A));

        // ADD M (Add memory)

        new I8080Instruction(0x86, "ADD (HL)", 7,
                cpu -> cpu.A = cpu.add(cpu.peekHL()));

        // ADI data (Add immediate)

        new I8080Instruction(0xC6, "ADD n", 7,
                cpu -> cpu.A = cpu.add(cpu.read8()));

        // ADC r (Add Register with carry)

        new I8080Instruction(0x88, "ADC B", 4,
                cpu -> cpu.A = cpu.adc(cpu.B));
        new I8080Instruction(0x89, "ADC C", 4,
                cpu -> cpu.A = cpu.adc(cpu.C));
        new I8080Instruction(0x8A, "ADC D", 4,
                cpu -> cpu.A = cpu.adc(cpu.D));
        new I8080Instruction(0x8B, "ADC E", 4,
                cpu -> cpu.A = cpu.adc(cpu.E));
        new I8080Instruction(0x8C, "ADC H", 4,
                cpu -> cpu.A = cpu.adc(cpu.H));
        new I8080Instruction(0x8D, "ADC L", 4,
                cpu -> cpu.A = cpu.adc(cpu.L));
        new I8080Instruction(0x8F, "ADC A", 4,
                cpu -> cpu.A = cpu.adc(cpu.A));

        // ADC M (Add memory with carry)

        new I8080Instruction(0x8E, "ADC (HL)", 7,
                cpu -> cpu.A = cpu.adc(cpu.peekHL()));

        // ACI data (Add immediate with carry)

        new I8080Instruction(0xCE, "ADC n", 7,
                cpu -> cpu.A = cpu.adc(cpu.read8()));

        // SUB r (Subtract Register)

        new I8080Instruction(0x90, "SUB B", 4,
                cpu -> cpu.A = cpu.sub(cpu.B));
        new I8080Instruction(0x91, "SUB C", 4,
                cpu -> cpu.A = cpu.sub(cpu.C));
        new I8080Instruction(0x92, "SUB D", 4,
                cpu -> cpu.A = cpu.sub(cpu.D));
        new I8080Instruction(0x93, "SUB E", 4,
                cpu -> cpu.A = cpu.sub(cpu.E));
        new I8080Instruction(0x94, "SUB H", 4,
                cpu -> cpu.A = cpu.sub(cpu.H));
        new I8080Instruction(0x95, "SUB L", 4,
                cpu -> cpu.A = cpu.sub(cpu.L));
        new I8080Instruction(0x97, "SUB A", 4,
                cpu -> cpu.A = cpu.sub(cpu.A));

        // SUB M (Subtract memory)

        new I8080Instruction(0x96, "SUB (HL)", 7,
                cpu -> cpu.A = cpu.sub(cpu.peekHL()));

        // SUB data (Subtract immediate)

        new I8080Instruction(0xD6, "SUB n", 7,
                cpu -> cpu.A = cpu.sub(cpu.read8()));

        // SBB r (Subtract Register with borrow)

        new I8080Instruction(0x98, "SBC B", 4,
                cpu -> cpu.A = cpu.sbc(cpu.B));
        new I8080Instruction(0x99, "SBC C", 4,
                cpu -> cpu.A = cpu.sbc(cpu.C));
        new I8080Instruction(0x9A, "SBC D", 4,
                cpu -> cpu.A = cpu.sbc(cpu.D));
        new I8080Instruction(0x9B, "SBC E", 4,
                cpu -> cpu.A = cpu.sbc(cpu.E));
        new I8080Instruction(0x9C, "SBC H", 4,
                cpu -> cpu.A = cpu.sbc(cpu.H));
        new I8080Instruction(0x9D, "SBC L", 4,
                cpu -> cpu.A = cpu.sbc(cpu.L));
        new I8080Instruction(0x9F, "SBC A", 4,
                cpu -> cpu.A = cpu.sbc(cpu.A));

        // SBB M (Subtract memory with borrow)

        new I8080Instruction(0x9E, "SBC (HL)", 7,
                cpu -> cpu.A = cpu.sbc(cpu.peekHL()));

        // SBI data (Subtract immediate with borrow)

        new I8080Instruction(0xDE, "SBC n", 7,
                cpu -> cpu.A = cpu.sbc(cpu.read8()));

        // INR r (Increment Register)

        new I8080Instruction(0x04, "INC B", 5,
                cpu -> cpu.B = cpu.inc(cpu.B));
        new I8080Instruction(0x0C, "INC C", 5,
                cpu -> cpu.C = cpu.inc(cpu.C));
        new I8080Instruction(0x14, "INC D", 5,
                cpu -> cpu.D = cpu.inc(cpu.D));
        new I8080Instruction(0x1C, "INC E", 5,
                cpu -> cpu.E = cpu.inc(cpu.E));
        new I8080Instruction(0x24, "INC H", 5,
                cpu -> cpu.H = cpu.inc(cpu.H));
        new I8080Instruction(0x2C, "INC L", 5,
                cpu -> cpu.L = cpu.inc(cpu.L));
        new I8080Instruction(0x3C, "INC A", 5,
                cpu -> cpu.A = cpu.inc(cpu.A));

        // INR M (Increment memory)

        new I8080Instruction(0x34, "INC (HL)", 10,
                cpu -> cpu.pokeHL(cpu.inc(cpu.peekHL())));

        // DCR r (Decrement Register)

        new I8080Instruction(0x05, "DEC B", 5,
                cpu -> cpu.B = cpu.dec(cpu.B));
        new I8080Instruction(0x0D, "DEC C", 5,
                cpu -> cpu.C = cpu.dec(cpu.C));
        new I8080Instruction(0x15, "DEC D", 5,
                cpu -> cpu.D = cpu.dec(cpu.D));
        new I8080Instruction(0x1D, "DEC E", 5,
                cpu -> cpu.E = cpu.dec(cpu.E));
        new I8080Instruction(0x25, "DEC H", 5,
                cpu -> cpu.H = cpu.dec(cpu.H));
        new I8080Instruction(0x2D, "DEC L", 5,
                cpu -> cpu.L = cpu.dec(cpu.L));
        new I8080Instruction(0x3D, "DEC A", 5,
                cpu -> cpu.A = cpu.dec(cpu.A));

        // DCR M (Decrement Memory)

        new I8080Instruction(0x35, "DEC (HL)", 10,
                cpu -> cpu.pokeHL(cpu.dec(cpu.peekHL())));

        // INX rp (Increment register pair)

        new I8080Instruction(0x03, "INC BC", 6,
                cpu -> cpu.setBC(cpu.BC() + 1));
        new I8080Instruction(0x13, "INC DE", 6,
                cpu -> cpu.setDE(cpu.DE() + 1));
        new I8080Instruction(0x23, "INC HL", 6,
                cpu -> cpu.setHL(cpu.HL() + 1));
        new I8080Instruction(0x33, "INC SP", 6,
                cpu -> cpu.SP = (cpu.SP + 1) & 0xFFFF);

        // DCX rp (Decrement register pair)

        new I8080Instruction(0x0B, "DEC BC", 6,
                cpu -> cpu.setBC(cpu.BC() - 1));
        new I8080Instruction(0x1B, "DEC DE", 6,
                cpu -> cpu.setDE(cpu.DE() - 1));
        new I8080Instruction(0x2B, "DEC HL", 6,
                cpu -> cpu.setHL(cpu.HL() - 1));
        new I8080Instruction(0x3B, "DEC SP", 6,
                cpu -> cpu.SP = (cpu.SP - 1) & 0xFFFF);

        // DAD rp (Add register pair to H and L)

        new I8080Instruction(0x09, "ADD BC", 10,
                cpu -> cpu.setHL(cpu.add2(cpu.BC())));
        new I8080Instruction(0x19, "ADD DE", 10,
                cpu -> cpu.setHL(cpu.add2(cpu.DE())));
        new I8080Instruction(0x29, "ADD HL", 10,
                cpu -> cpu.setHL(cpu.add2(cpu.HL())));
        new I8080Instruction(0x39, "ADD SP", 10,
                cpu -> cpu.setHL(cpu.add2(cpu.SP)));

        // DAA (Decimal Adjust Accumulator)

        new I8080Instruction(0x27, "DAA", 4,
                I8080::daa);

        // Logical Group

        // ANA r (AND Register)

        new I8080Instruction(0xA0, "AND B", 4,
                cpu -> cpu.A = cpu.and(cpu.B));
        new I8080Instruction(0xA1, "AND C", 4,
                cpu -> cpu.A = cpu.and(cpu.C));
        new I8080Instruction(0xA2, "AND D", 4,
                cpu -> cpu.A = cpu.and(cpu.D));
        new I8080Instruction(0xA3, "AND E", 4,
                cpu -> cpu.A = cpu.and(cpu.E));
        new I8080Instruction(0xA4, "AND H", 4,
                cpu -> cpu.A = cpu.and(cpu.H));
        new I8080Instruction(0xA5, "AND L", 4,
                cpu -> cpu.A = cpu.and(cpu.L));
        new I8080Instruction(0xA7, "AND A", 4,
                cpu -> cpu.A = cpu.and(cpu.A));

        // ANA M (AND memory)

        new I8080Instruction(0xA6, "AND (HL)", 7,
                cpu -> cpu.A = cpu.and(cpu.peekHL()));

        // ANI data (AND immediate)

        new I8080Instruction(0xE6, "AND n", 7,
                cpu -> {
                    cpu.A = cpu.and(cpu.read8());
                    cpu.AC = false;
                });

        // XRA r (Exclusive OR Register)

        new I8080Instruction(0xA8, "XOR B", 4,
                cpu -> cpu.A = cpu.xor(cpu.B));
        new I8080Instruction(0xA9, "XOR C", 4,
                cpu -> cpu.A = cpu.xor(cpu.C));
        new I8080Instruction(0xAA, "XOR D", 4,
                cpu -> cpu.A = cpu.xor(cpu.D));
        new I8080Instruction(0xAB, "XOR E", 4,
                cpu -> cpu.A = cpu.xor(cpu.E));
        new I8080Instruction(0xAC, "XOR H", 4,
                cpu -> cpu.A = cpu.xor(cpu.H));
        new I8080Instruction(0xAD, "XOR L", 4,
                cpu -> cpu.A = cpu.xor(cpu.L));
        new I8080Instruction(0xAF, "XOR A", 4,
                cpu -> cpu.A = cpu.xor(cpu.A));

        // XRA M (Exclusive OR memory

        new I8080Instruction(0xAE, "XOR (HL)", 7,
                cpu -> cpu.A = cpu.xor(cpu.peekHL()));

        // XRI data (Exclusive OR immediate)

        new I8080Instruction(0xEE, "XOR n", 7,
                cpu -> cpu.A = cpu.xor(cpu.read8()));

        // ORA r (OR Register)

        new I8080Instruction(0xB0, "OR B", 4,
                cpu -> cpu.A = cpu.or(cpu.B));
        new I8080Instruction(0xB1, "OR C", 4,
                cpu -> cpu.A = cpu.or(cpu.C));
        new I8080Instruction(0xB2, "OR D", 4,
                cpu -> cpu.A = cpu.or(cpu.D));
        new I8080Instruction(0xB3, "OR E", 4,
                cpu -> cpu.A = cpu.or(cpu.E));
        new I8080Instruction(0xB4, "OR H", 4,
                cpu -> cpu.A = cpu.or(cpu.H));
        new I8080Instruction(0xB5, "OR L", 4,
                cpu -> cpu.A = cpu.or(cpu.L));
        new I8080Instruction(0xB7, "OR A", 4,
                cpu -> cpu.A = cpu.or(cpu.A));

        // ORA M (OR memory)

        new I8080Instruction(0xB6, "OR (HL)", 7,
                cpu -> cpu.A = cpu.or(cpu.peekHL()));

        // ORI data (OR immediate)

        new I8080Instruction(0xF6, "OR n", 7,
                cpu -> cpu.A = cpu.or(cpu.read8()));

        // CMP r (Compare Register)

        new I8080Instruction(0xB8, "CP B", 4,
                cpu -> cpu.cp(cpu.B));
        new I8080Instruction(0xB9, "CP C", 4,
                cpu -> cpu.cp(cpu.C));
        new I8080Instruction(0xBA, "CP D", 4,
                cpu -> cpu.cp(cpu.D));
        new I8080Instruction(0xBB, "CP E", 4,
                cpu -> cpu.cp(cpu.E));
        new I8080Instruction(0xBC, "CP H", 4,
                cpu -> cpu.cp(cpu.H));
        new I8080Instruction(0xBD, "CP L", 4,
                cpu -> cpu.cp(cpu.L));
        new I8080Instruction(0xBF, "CP A", 4,
                cpu -> cpu.cp(cpu.A));

        // CMP M (Compare memory)

        new I8080Instruction(0xBE, "CP (HL)", 7,
                cpu -> cpu.cp(cpu.peekHL()));

        // CPI data (Compare immediate)

        new I8080Instruction(0xFE, "CP n", 7,
                cpu -> cpu.cp(cpu.read8()));

        new I8080Instruction(0x07, "RLCA", 4,
                I8080::rlc);
        new I8080Instruction(0x0F, "RRCA", 4,
                I8080::rrc);
        new I8080Instruction(0x17, "RLA", 4,
                I8080::rl);
        new I8080Instruction(0x1F, "RRA", 4,
                I8080::rr);
        new I8080Instruction(0x2F, "CPL", 4,
                cpu -> cpu.A = (~cpu.A) & 0xFF);

        new I8080Instruction(0x37, "SCF", 4,
                cpu -> cpu.CY = true);
        new I8080Instruction(0x3F, "CCF", 4,
                cpu -> cpu.CY = !cpu.CY);

        // Aliases

        INSTRUCTIONS[0x10] = INSTRUCTIONS[0x00];
        INSTRUCTIONS[0x20] = INSTRUCTIONS[0x00];
        INSTRUCTIONS[0x30] = INSTRUCTIONS[0x00];

        INSTRUCTIONS[0x08] = INSTRUCTIONS[0x00];
        INSTRUCTIONS[0x18] = INSTRUCTIONS[0x00];
        INSTRUCTIONS[0x28] = INSTRUCTIONS[0x00];
        INSTRUCTIONS[0x38] = INSTRUCTIONS[0x00];

        INSTRUCTIONS[0xCB] = INSTRUCTIONS[0xC3];
        INSTRUCTIONS[0xD9] = INSTRUCTIONS[0xC9];
        INSTRUCTIONS[0xDD] = INSTRUCTIONS[0xCD];
        INSTRUCTIONS[0xED] = INSTRUCTIONS[0xCD];
        INSTRUCTIONS[0xFD] = INSTRUCTIONS[0xCD];
    }
}
